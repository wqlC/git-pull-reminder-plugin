package com.github.zhenyuan.gitpullreminder;

import com.intellij.openapi.application.ApplicationManager;
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
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Git commit 前检查远程是否有未 pull 的提交
 * 
 * @author 贞元
 */
public class GitPullReminderCheckinHandler extends CheckinHandler {

    private static final Logger LOG = Logger.getInstance(GitPullReminderCheckinHandler.class);

    private final CheckinProjectPanel panel;
    private final Project project;

    public GitPullReminderCheckinHandler(@NotNull CheckinProjectPanel panel) {
        this.panel = panel;
        this.project = panel.getProject();
    }

    @Override
    public ReturnResult beforeCheckin() {
        if (project == null) {
            return ReturnResult.COMMIT;
        }

        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        Collection<GitRepository> repositories = repositoryManager.getRepositories();

        if (repositories.isEmpty()) {
            return ReturnResult.COMMIT;
        }

        AtomicReference<ReturnResult> resultRef = new AtomicReference<>(ReturnResult.COMMIT);
        AtomicBoolean hasRemoteCommits = new AtomicBoolean(false);
        AtomicInteger behindCount = new AtomicInteger(0);
        AtomicReference<String> branchInfo = new AtomicReference<>("");

        ProgressManager.getInstance().run(new Task.Modal(project, "检查远程仓库状态...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("正在获取远程仓库最新状态...");

                for (GitRepository repository : repositories) {
                    if (indicator.isCanceled()) {
                        resultRef.set(ReturnResult.CANCEL);
                        return;
                    }

                    indicator.setText2("正在 fetch: " + repository.getRoot().getName());

                    boolean fetchSuccess = fetchRemote(repository, indicator);
                    if (!fetchSuccess) {
                        LOG.warn("Fetch failed for repository: " + repository.getRoot().getPath());
                        continue;
                    }

                    indicator.setText2("正在检查分支差异...");

                    int behind = checkBehindCount(repository);
                    if (behind > 0) {
                        hasRemoteCommits.set(true);
                        behindCount.addAndGet(behind);

                        GitLocalBranch currentBranch = repository.getCurrentBranch();
                        if (currentBranch != null) {
                            GitRemoteBranch trackedBranch = currentBranch.findTrackedBranch(repository);
                            if (trackedBranch != null) {
                                branchInfo.set(currentBranch.getName() + " <- " + trackedBranch.getName());
                            }
                        }
                    }
                }
            }
        });

        if (resultRef.get() == ReturnResult.CANCEL) {
            return ReturnResult.CANCEL;
        }

        if (hasRemoteCommits.get()) {
            return showPullConfirmDialog(behindCount.get(), branchInfo.get(), repositories);
        }

        return ReturnResult.COMMIT;
    }

    /**
     * 执行 git fetch 获取远程最新状态
     */
    private boolean fetchRemote(@NotNull GitRepository repository, @NotNull ProgressIndicator indicator) {
        try {
            GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.FETCH);
            handler.addParameters("--all");
            GitCommandResult result = Git.getInstance().runCommand(handler);
            
            if (result.success()) {
                repository.update();
                return true;
            } else {
                LOG.warn("Fetch failed: " + result.getErrorOutputAsJoinedString());
                return false;
            }
        } catch (Exception e) {
            LOG.warn("Fetch failed", e);
            return false;
        }
    }

    /**
     * 检查本地分支落后远程分支的提交数
     */
    private int checkBehindCount(@NotNull GitRepository repository) {
        GitLocalBranch currentBranch = repository.getCurrentBranch();
        if (currentBranch == null) {
            return 0;
        }

        GitRemoteBranch trackedBranch = currentBranch.findTrackedBranch(repository);
        if (trackedBranch == null) {
            return 0;
        }

        try {
            GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.REV_LIST);
            handler.addParameters("--count");
            handler.addParameters(currentBranch.getName() + ".." + trackedBranch.getName());

            GitCommandResult result = Git.getInstance().runCommand(handler);
            if (result.success()) {
                String output = result.getOutputAsJoinedString().trim();
                return Integer.parseInt(output);
            }
        } catch (Exception e) {
            LOG.warn("Failed to check behind count", e);
        }

        return 0;
    }

    /**
     * 显示确认对话框，询问用户是否需要先 pull
     */
    private ReturnResult showPullConfirmDialog(int behindCount, 
                                                String branchInfo, 
                                                Collection<GitRepository> repositories) {
        String message = String.format(
                "检测到远程仓库有 %d 个新提交尚未 pull。\n\n" +
                "分支信息: %s\n\n" +
                "建议先执行 pull 操作以避免潜在的合并冲突。\n\n" +
                "是否现在执行 pull？",
                behindCount, branchInfo
        );

        int choice = Messages.showYesNoCancelDialog(
                project,
                message,
                "Git Pull 提醒",
                "Pull 后再提交",
                "直接提交",
                "取消",
                Messages.getWarningIcon()
        );

        switch (choice) {
            case Messages.YES:
                return executePullAndCommit(repositories);
            case Messages.NO:
                return ReturnResult.COMMIT;
            default:
                return ReturnResult.CANCEL;
        }
    }

    /**
     * 执行 pull 操作
     */
    private ReturnResult executePullAndCommit(Collection<GitRepository> repositories) {
        AtomicBoolean pullSuccess = new AtomicBoolean(true);

        ProgressManager.getInstance().run(new Task.Modal(project, "正在执行 Git Pull...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                for (GitRepository repository : repositories) {
                    if (indicator.isCanceled()) {
                        pullSuccess.set(false);
                        return;
                    }

                    indicator.setText("正在 pull: " + repository.getRoot().getName());

                    try {
                        GitLineHandler handler = new GitLineHandler(project, repository.getRoot(), GitCommand.PULL);
                        GitCommandResult result = Git.getInstance().runCommand(handler);

                        if (!result.success()) {
                            pullSuccess.set(false);
                            ApplicationManager.getApplication().invokeLater(() -> {
                                Messages.showErrorDialog(
                                        project,
                                        "Pull 失败:\n" + result.getErrorOutputAsJoinedString(),
                                        "Git Pull 错误"
                                );
                            });
                            return;
                        }

                        repository.update();
                    } catch (Exception e) {
                        LOG.error("Pull failed", e);
                        pullSuccess.set(false);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            Messages.showErrorDialog(
                                    project,
                                    "Pull 失败:\n" + e.getMessage(),
                                    "Git Pull 错误"
                            );
                        });
                        return;
                    }
                }
            }
        });

        if (pullSuccess.get()) {
            Messages.showInfoMessage(project, "Pull 成功！现在可以继续提交。", "Git Pull 完成");
            return ReturnResult.COMMIT;
        } else {
            return ReturnResult.CANCEL;
        }
    }
}
