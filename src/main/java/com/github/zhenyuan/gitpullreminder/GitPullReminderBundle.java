package com.github.zhenyuan.gitpullreminder;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * 国际化资源 Bundle，支持中英文切换。
 *
 * @author 贞元
 */
public final class GitPullReminderBundle extends DynamicBundle {

    @NonNls
    private static final String BUNDLE = "messages.GitPullReminderBundle";

    private static final GitPullReminderBundle INSTANCE = new GitPullReminderBundle();

    private GitPullReminderBundle() {
        super(BUNDLE);
    }

    @Nls
    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                  @NotNull Object... params) {
        return INSTANCE.getMessage(key, params);
    }
}
