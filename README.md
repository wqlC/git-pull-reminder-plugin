# Git Pull Reminder Plugin

[![JetBrains IntelliJ Plugins](https://img.shields.io/badge/JetBrains-Plugin-blue?logo=intellij-idea)](https://plugins.jetbrains.com/)
[![GitHub](https://img.shields.io/github/license/wqlC/git-pull-reminder-plugin)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/wqlC/git-pull-reminder-plugin?include_prereleases)](https://github.com/wqlC/git-pull-reminder-plugin/releases)

A lightweight IntelliJ IDEA plugin that reminds you to pull before committing when your branch is behind the remote.

一个轻量级的 IntelliJ IDEA 插件，在 Git commit 之前自动检查远程仓库是否有未 pull 的提交。

## ✨ Features

- 🔍 **Auto Fetch**: Automatically executes `git fetch` before each commit to get the latest remote status
- 📊 **Smart Detection**: Compares local branch with remote tracking branch to detect unpulled commits
- 💬 **User-Friendly Dialog**: Shows a clear dialog when remote has new commits
- 🎯 **Flexible Options**:
  - **Pull then Commit**: Automatically pull and continue with commit
  - **Commit Anyway**: Ignore the warning and proceed with commit
  - **Cancel**: Cancel the commit operation

## 📦 Installation

### From JetBrains Marketplace (Recommended)

1. Open IntelliJ IDEA
2. Go to `Settings/Preferences` → `Plugins` → `Marketplace`
3. Search for "Git Pull Reminder"
4. Click `Install` and restart IDE

### From Disk

1. Download the latest release from [GitHub Releases](https://github.com/wqlC/git-pull-reminder-plugin/releases)
2. In IntelliJ IDEA: `Settings/Preferences` → `Plugins` → ⚙️ → `Install Plugin from Disk...`
3. Select the downloaded `.zip` file
4. Restart IDE

### Build from Source

```bash
git clone https://github.com/wqlC/git-pull-reminder-plugin.git
cd git-pull-reminder-plugin
./gradlew buildPlugin
```

The plugin file will be at `build/distributions/git-pull-reminder-plugin-*.zip`

## 🚀 Usage

After installation, the plugin works automatically:

1. When you commit changes in IntelliJ IDEA
2. Plugin fetches the latest remote status
3. If remote has new commits, a dialog appears
4. Choose your preferred action

## 📋 Requirements

- IntelliJ IDEA 2023.3 or later (also works with other JetBrains IDEs)
- Git plugin enabled

## 🏗️ Project Structure

```
git-pull-reminder-plugin/
├── build.gradle.kts                    # Gradle build configuration
├── settings.gradle.kts                 # Gradle settings
├── src/main/
│   ├── java/.../gitpullreminder/
│   │   ├── GitPullReminderCheckinHandlerFactory.java
│   │   └── GitPullReminderCheckinHandler.java
│   └── resources/META-INF/
│       └── plugin.xml                  # Plugin configuration
└── gradle/wrapper/
    └── gradle-wrapper.properties
```

## 🔧 Development

### Run in Development Mode

```bash
./gradlew runIde
```

### Build Plugin

```bash
./gradlew buildPlugin
```

### Verify Plugin

```bash
./gradlew verifyPlugin
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by [Git Push Reminder](https://github.com/ChrisCarini/git-push-reminder-jetbrains-plugin)
- Built with [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
