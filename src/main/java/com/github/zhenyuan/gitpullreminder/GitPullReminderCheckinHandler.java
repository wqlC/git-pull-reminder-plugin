package com.github.zhenyuan.gitpullreminder;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Git commit 前检查远程是否有未 pull 的提交。
 * <p>
 * 优化点：
 * <ul>
 *   <li>fetch 只拉当前分支对应的 remote，而非 --all</li>
 *   <li>fetch 支持超时保护</li>
 *   <li>pull 策略尊重用户 git config 或插件配置</li>
 *   <li>多仓库场景按仓库分别展示落后信息</li>
 *   <li>Detached HEAD 场景给出提示</li>
 *   <li>Pull 成功使用 Notification 替代阻塞 Dialog</li>
 *   <li>Pull 失败不再重复弹出错误提示</li>
 *   <li>全部 UI 文案国际化</li>
 * </ul>
 *
 * @author 贞元
 */
public class GitPullReminderCheckinHandler extends CheckinHandler {

    private static final Logger LOG = Logger.getInstance(GitPullReminderCheckinHandler.class);

    private final Project project;

    public GitPullReminderCheckinHandler(@NotNull CheckinProjectPanel panel) {
        this.project = panel.getProject();
    }

    @Override
    public ReturnResult beforeCheckin() {
        if (project == null) {
            return ReturnResult.COMMIT;
        }

        GitPullReminderSettings settings = GitPullReminderSettings.getInstance();
        if (!settings.isEnabled()) {
            return ReturnResult.COMMIT;
        }

        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        Collection<GitRepository> repositories = repositoryManager.getRepositories();

        if (repositories.isEmpty()) {
            return ReturnResult.COMMIT;
        }

        AtomicReference<ReturnResult> resultRef = new AtomicReference<>(ReturnResult.COMMIT);
        List<BehindInfo> behindInfoList = new ArrayList<>();
        List<String> detachedHeadWarnings = new ArrayList<>();

        ProgressManager.getInstance().run(new Task.Modal(project,
                GitPullReminderBundle.message("progress.checking.remote"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText(GitPullReminderBundle.message("progress.fetching.remote"));

                int totalRepositories = repositories.size();
                int currentIndex = 0;

                for (GitRepository repository : repositories) {
                    if (indicator.isCanceled()) {
                        resultRef.set(ReturnResult.CANCEL);
                        return;
                    }

                    currentIndex++;
                    indicator.setFraction((double) currentIndex / totalRepositories);
                    String repositoryName = repository.getRoot().getName();

                    GitLocalBranch currentBranch = repository.getCurrentBranch();
                    if (currentBranch == null) {
                        detachedHeadWarnings.add(
                                GitPullReminderBundle.message("dialog.detached.head.warning", repositoryName));
                        LOG.info("Repository in detached HEAD state: " + repositoryName);
                        continue;
                    }

                    GitRemoteBranch trackedBranch = currentBranch.findTrackedBranch(repository);
                    if (trackedBranch == null) {
                        continue;
                    }

                    indicator.setText2(GitPullReminderBundle.message("progress.fetching.repository", repositoryName));

                    boolean fetchSuccess = fetchRemoteForBranch(repository, trackedBranch, indicator);
                    if (!fetchSuccess) {
                        LOG.warn("Fetch failed for repository: " + repository.getRoot().getPath());
                        continue;
                    }

                    indicator.setText2(GitPullReminderBundle.message("progress.checking.branch.diff"));

                    int behindCount = checkBehindCount(repository, currentBranch, trackedBranch);
                    if (behindCount > 0) {
                        behindInfoList.add(new BehindInfo(
                                repository, repositoryName, behindCount,
                                currentBranch.getName(), trackedBranch.getName()));
                    }
                }
            }
        });

        if (resultRef.get() == ReturnResult.CANCEL) {
            return ReturnResult.CANCEL;
        }

        if (!behindInfoList.isEmpty()) {
            return showPullConfirmDialog(behindInfoList, detachedHeadWarnings);
        }

        return ReturnResult.COMMIT;
    }

    /**
     * 只 fetch 当前分支对应的 remote，而非 --all，并支持超时保护。
     */
    private boolean fetchRemoteForBranch(@NotNull GitRepository repository,
                                          @NotNull GitRemoteBranch trackedBranch,
                                          @NotNull ProgressIndicator indicator) {
        GitRemote remote = trackedBranch.getRemote();
        int timeoutSeconds = GitPullReminderSettings.getInstance().getFetchTimeoutSeconds();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(() -> {
            try {
                GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.FETCH);
                handler.addParameters(remote.getName());
                GitCommandResult result = Git.getInstance().runCommand(handler);

                if (result.success()) {
                    repository.update();
                    return true;
                } else {
                    LOG.warn("Fetch failed for remote '" + remote.getName() + "': "
                            + result.getErrorOutputAsJoinedString());
                    return false;
                }
            } catch (Exception exception) {
                LOG.warn("Fetch failed for remote '" + remote.getName() + "'", exception);
                return false;
            }
        });

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutException) {
            future.cancel(true);
            LOG.warn(GitPullReminderBundle.message("notification.fetch.timeout",
                    repository.getRoot().getName()));
            return false;
        } catch (InterruptedException | ExecutionException exception) {
            LOG.warn("Fetch interrupted or failed", exception);
            return false;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 检查本地分支落后远程分支的提交数。
     */
    private int checkBehindCount(@NotNull GitRepository repository,
                                  @NotNull GitLocalBranch localBranch,
                                  @NotNull GitRemoteBranch remoteBranch) {
        try {
            GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.REV_LIST);
            handler.addParameters("--count");
            handler.addParameters(localBranch.getName() + ".." + remoteBranch.getName());

            GitCommandResult result = Git.getInstance().runCommand(handler);
            if (result.success()) {
                String output = result.getOutputAsJoinedString().trim();
                return Integer.parseInt(output);
            }
        } catch (Exception exception) {
            LOG.warn("Failed to check behind count for " + localBranch.getName(), exception);
        }

        return 0;
    }

    /**
     * 显示确认对话框，按仓库分别展示落后信息。
     */
    private ReturnResult showPullConfirmDialog(@NotNull List<BehindInfo> behindInfoList,
                                                @NotNull List<String> detachedHeadWarnings) {
        StringBuilder detailBuilder = new StringBuilder();
        List<GitRepository> repositoriesToPull = new ArrayList<>();

        for (BehindInfo info : behindInfoList) {
            detailBuilder.append(GitPullReminderBundle.message("dialog.behind.detail",
                    info.repositoryName, info.behindCount,
                    info.localBranchName, info.remoteBranchName));
            detailBuilder.append("\n");
            repositoriesToPull.add(info.repository);
        }

        for (String warning : detachedHeadWarnings) {
            detailBuilder.append(warning);
        }

        String message = GitPullReminderBundle.message("dialog.behind.message",
                detailBuilder.toString().trim());

        int choice = Messages.showYesNoCancelDialog(
                project,
                message,
                GitPullReminderBundle.message("dialog.title"),
                GitPullReminderBundle.message("dialog.button.pull"),
                GitPullReminderBundle.message("dialog.button.commit"),
                GitPullReminderBundle.message("dialog.button.cancel"),
                Messages.getWarningIcon()
        );

        return switch (choice) {
            case Messages.YES -> executePullAndCommit(repositoriesToPull);
            case Messages.NO -> ReturnResult.COMMIT;
            default -> ReturnResult.CANCEL;
        };
    }

    /**
     * 执行 pull 操作，根据插件配置或 git config 决定使用 merge 还是 rebase。
     */
    private ReturnResult executePullAndCommit(@NotNull List<GitRepository> repositories) {
        AtomicBoolean pullSuccess = new AtomicBoolean(true);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        ProgressManager.getInstance().run(new Task.Modal(project,
                GitPullReminderBundle.message("progress.pulling"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                for (GitRepository repository : repositories) {
                    if (indicator.isCanceled()) {
                        pullSuccess.set(false);
                        return;
                    }

                    indicator.setText(GitPullReminderBundle.message(
                            "progress.pulling.repository", repository.getRoot().getName()));

                    try {
                        GitLineHandler handler = new GitLineHandler(
                                project, repository.getRoot(), GitCommand.PULL);
                        appendPullStrategyParameters(handler, repository);

                        GitCommandResult result = Git.getInstance().runCommand(handler);

                        if (!result.success()) {
                            pullSuccess.set(false);
                            errorMessage.set(result.getErrorOutputAsJoinedString());
                            return;
                        }

                        repository.update();
                    } catch (Exception exception) {
                        LOG.error("Pull failed for " + repository.getRoot().getName(), exception);
                        pullSuccess.set(false);
                        errorMessage.set(exception.getMessage());
                        return;
                    }
                }
            }
        });

        if (pullSuccess.get()) {
            if (GitPullReminderSettings.getInstance().isShowSuccessNotification()) {
                showNotification(
                        GitPullReminderBundle.message("notification.pull.success"),
                        NotificationType.INFORMATION);
            }
            return ReturnResult.COMMIT;
        } else {
            Messages.showErrorDialog(
                    project,
                    GitPullReminderBundle.message("notification.pull.failed.message",
                            errorMessage.get() != null ? errorMessage.get() : "Unknown error"),
                    GitPullReminderBundle.message("notification.pull.failed.title"));
            return ReturnResult.CANCEL;
        }
    }

    /**
     * 根据插件配置决定 pull 策略参数。
     * <ul>
     *   <li>FOLLOW_GIT_CONFIG：不添加额外参数，由 git config 决定</li>
     *   <li>MERGE：强制 --no-rebase</li>
     *   <li>REBASE：强制 --rebase</li>
     * </ul>
     */
    private void appendPullStrategyParameters(@NotNull GitLineHandler handler,
                                               @NotNull GitRepository repository) {
        GitPullReminderSettings.PullStrategy strategy =
                GitPullReminderSettings.getInstance().getPullStrategy();

        switch (strategy) {
            case MERGE -> handler.addParameters("--no-rebase");
            case REBASE -> handler.addParameters("--rebase");
            case FOLLOW_GIT_CONFIG -> {
                // 不添加额外参数，完全尊重用户的 git config
            }
        }
    }

    /**
     * 使用 IDE 气泡通知替代阻塞式 Dialog。
     */
    private void showNotification(@NotNull String content, @NotNull NotificationType type) {
        Notification notification = NotificationGroupManager.getInstance()
                .getNotificationGroup(GitPullReminderBundle.message("notification.group.id"))
                .createNotification(content, type);
        notification.notify(project);
    }

    /**
     * 记录单个仓库的落后信息，用于多仓库场景下分别展示。
     */
    private record BehindInfo(
            GitRepository repository,
            String repositoryName,
            int behindCount,
            String localBranchName,
            String remoteBranchName
    ) {
    }
}
