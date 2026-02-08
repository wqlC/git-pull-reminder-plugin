# Git Pull Reminder Plugin

一个 IntelliJ IDEA 插件，在 Git commit 之前自动检查远程仓库是否有未 pull 的提交。

## 功能特性

- ✅ 在每次 commit 之前自动执行 `git fetch` 获取远程最新状态
- ✅ 比较本地分支和远程跟踪分支的差异
- ✅ 如果远程有新提交，弹出对话框提示用户
- ✅ 用户可以选择：
  - **Pull 后再提交**：自动执行 pull 操作，成功后继续提交
  - **直接提交**：忽略警告，直接提交
  - **取消**：取消本次提交操作

## 安装方式

### 方式一：从源码构建

1. 克隆或下载本项目
2. 使用 IntelliJ IDEA 打开项目
3. 执行 Gradle 任务构建插件：
   ```bash
   ./gradlew buildPlugin
   ```
4. 构建完成后，插件文件位于 `build/distributions/git-pull-reminder-plugin-1.0.0.zip`
5. 在 IntelliJ IDEA 中：
   - 打开 `Settings/Preferences` → `Plugins`
   - 点击齿轮图标 → `Install Plugin from Disk...`
   - 选择构建好的 zip 文件
   - 重启 IDE

### 方式二：直接运行调试

```bash
./gradlew runIde
```

这将启动一个带有插件的 IntelliJ IDEA 实例用于测试。

## 使用说明

安装插件后，每次在 IntelliJ IDEA 中执行 Git commit 操作时：

1. 插件会自动执行 `git fetch` 获取远程最新状态
2. 检查当前分支是否落后于远程跟踪分支
3. 如果远程有新提交，会弹出提示对话框
4. 根据你的选择执行相应操作

## 系统要求

- IntelliJ IDEA 2023.3 或更高版本
- Git 插件已启用

## 开发说明

### 项目结构

```
git-pull-reminder-plugin/
├── build.gradle.kts              # Gradle 构建配置
├── settings.gradle.kts           # Gradle 设置
├── src/main/
│   ├── java/com/github/zhenyuan/gitpullreminder/
│   │   ├── GitPullReminderCheckinHandlerFactory.java  # 工厂类
│   │   └── GitPullReminderCheckinHandler.java         # 核心处理逻辑
│   └── resources/META-INF/
│       └── plugin.xml            # 插件配置
└── gradle/wrapper/
    └── gradle-wrapper.properties # Gradle Wrapper 配置
```

### 核心技术

- 使用 `CheckinHandlerFactory` 扩展点注册提交前检查
- 使用 `Git4Idea` API 与 Git 交互
- 使用 `git rev-list --count` 命令计算落后的提交数

## License

MIT License
