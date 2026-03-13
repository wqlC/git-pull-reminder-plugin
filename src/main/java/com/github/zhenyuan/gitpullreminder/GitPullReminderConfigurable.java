package com.github.zhenyuan.gitpullreminder;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Settings 配置页面，路径：Settings → Tools → Git Pull Reminder
 *
 * @author 贞元
 */
public class GitPullReminderConfigurable implements Configurable {

    private JCheckBox enabledCheckBox;
    private JComboBox<GitPullReminderSettings.PullStrategy> pullStrategyComboBox;
    private JCheckBox showSuccessNotificationCheckBox;
    private JSpinner fetchTimeoutSpinner;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Git Pull Reminder";
    }

    @Override
    public @Nullable JComponent createComponent() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(5, 5, 5, 5);

        int row = 0;

        // 启用/禁用
        enabledCheckBox = new JCheckBox(GitPullReminderBundle.message("settings.enabled"));
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        mainPanel.add(enabledCheckBox, constraints);
        row++;

        // Pull 策略
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = row;
        mainPanel.add(new JLabel(GitPullReminderBundle.message("settings.pull.strategy")), constraints);

        pullStrategyComboBox = new JComboBox<>(GitPullReminderSettings.PullStrategy.values());
        pullStrategyComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof GitPullReminderSettings.PullStrategy strategy) {
                    setText(switch (strategy) {
                        case FOLLOW_GIT_CONFIG -> GitPullReminderBundle.message("settings.pull.strategy.follow.git.config");
                        case MERGE -> GitPullReminderBundle.message("settings.pull.strategy.merge");
                        case REBASE -> GitPullReminderBundle.message("settings.pull.strategy.rebase");
                    });
                }
                return this;
            }
        });
        constraints.gridx = 1;
        mainPanel.add(pullStrategyComboBox, constraints);
        row++;

        // 显示成功通知
        showSuccessNotificationCheckBox = new JCheckBox(
                GitPullReminderBundle.message("settings.show.success.notification"));
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        mainPanel.add(showSuccessNotificationCheckBox, constraints);
        row++;

        // Fetch 超时
        constraints.gridwidth = 1;
        constraints.gridx = 0;
        constraints.gridy = row;
        mainPanel.add(new JLabel(GitPullReminderBundle.message("settings.fetch.timeout")), constraints);

        fetchTimeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 120, 5));
        constraints.gridx = 1;
        mainPanel.add(fetchTimeoutSpinner, constraints);
        row++;

        // 填充剩余空间
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.weighty = 1.0;
        mainPanel.add(new JPanel(), constraints);

        return mainPanel;
    }

    @Override
    public boolean isModified() {
        GitPullReminderSettings settings = GitPullReminderSettings.getInstance();
        return enabledCheckBox.isSelected() != settings.isEnabled()
                || !Objects.equals(pullStrategyComboBox.getSelectedItem(), settings.getPullStrategy())
                || showSuccessNotificationCheckBox.isSelected() != settings.isShowSuccessNotification()
                || (int) fetchTimeoutSpinner.getValue() != settings.getFetchTimeoutSeconds();
    }

    @Override
    public void apply() throws ConfigurationException {
        int timeout = (int) fetchTimeoutSpinner.getValue();
        if (timeout < 5 || timeout > 120) {
            throw new ConfigurationException(
                    GitPullReminderBundle.message("settings.fetch.timeout.invalid"));
        }

        GitPullReminderSettings settings = GitPullReminderSettings.getInstance();
        settings.setEnabled(enabledCheckBox.isSelected());
        settings.setPullStrategy((GitPullReminderSettings.PullStrategy) pullStrategyComboBox.getSelectedItem());
        settings.setShowSuccessNotification(showSuccessNotificationCheckBox.isSelected());
        settings.setFetchTimeoutSeconds(timeout);
    }

    @Override
    public void reset() {
        GitPullReminderSettings settings = GitPullReminderSettings.getInstance();
        enabledCheckBox.setSelected(settings.isEnabled());
        pullStrategyComboBox.setSelectedItem(settings.getPullStrategy());
        showSuccessNotificationCheckBox.setSelected(settings.isShowSuccessNotification());
        fetchTimeoutSpinner.setValue(settings.getFetchTimeoutSeconds());
    }
}
