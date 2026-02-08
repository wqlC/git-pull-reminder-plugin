package com.github.zhenyuan.gitpullreminder;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 工厂类，用于创建 Git Pull 提醒的 CheckinHandler
 * 
 * @author 贞元
 */
public class GitPullReminderCheckinHandlerFactory extends CheckinHandlerFactory {

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, 
                                         @NotNull CommitContext commitContext) {
        return new GitPullReminderCheckinHandler(panel);
    }
}
