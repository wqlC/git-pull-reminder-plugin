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
public final class GitPullReminderBundle {

    @NonNls
    private static final String BUNDLE = "messages.GitPullReminderBundle";

    private static final DynamicBundle DYNAMIC_BUNDLE =
            new DynamicBundle(GitPullReminderBundle.class, BUNDLE);

    private GitPullReminderBundle() {}

    @Nls
    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key,
                                  @NotNull Object... params) {
        return DYNAMIC_BUNDLE.getMessage(key, params);
    }
}
