package com.github.zhenyuan.gitpullreminder;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 插件持久化配置，存储用户的偏好设置。
 *
 * @author 贞元
 */
@State(
        name = "GitPullReminderSettings",
        storages = @Storage("GitPullReminderSettings.xml")
)
public class GitPullReminderSettings implements PersistentStateComponent<GitPullReminderSettings.State> {

    /**
     * Pull 策略枚举
     */
    public enum PullStrategy {
        /** 跟随用户的 git config 配置 */
        FOLLOW_GIT_CONFIG,
        /** 强制使用 merge 方式 */
        MERGE,
        /** 强制使用 rebase 方式 */
        REBASE
    }

    /**
     * 持久化状态数据
     */
    public static class State {
        /** 是否启用插件 */
        public boolean enabled = true;
        /** Pull 策略 */
        public PullStrategy pullStrategy = PullStrategy.FOLLOW_GIT_CONFIG;
        /** Pull 成功后是否显示通知 */
        public boolean showSuccessNotification = true;
        /** Fetch 超时时间（秒） */
        public int fetchTimeoutSeconds = 30;
    }

    private State state = new State();

    public static GitPullReminderSettings getInstance() {
        return ApplicationManager.getApplication().getService(GitPullReminderSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public boolean isEnabled() {
        return state.enabled;
    }

    public void setEnabled(boolean enabled) {
        state.enabled = enabled;
    }

    public PullStrategy getPullStrategy() {
        return state.pullStrategy;
    }

    public void setPullStrategy(PullStrategy pullStrategy) {
        state.pullStrategy = pullStrategy;
    }

    public boolean isShowSuccessNotification() {
        return state.showSuccessNotification;
    }

    public void setShowSuccessNotification(boolean showSuccessNotification) {
        state.showSuccessNotification = showSuccessNotification;
    }

    public int getFetchTimeoutSeconds() {
        return state.fetchTimeoutSeconds;
    }

    public void setFetchTimeoutSeconds(int fetchTimeoutSeconds) {
        state.fetchTimeoutSeconds = fetchTimeoutSeconds;
    }
}
