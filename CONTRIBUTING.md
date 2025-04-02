# 贡献指南

## 重要提示：Windows 用户

虽然 Git hooks 通过 Gradle 自动安装，但 **`ktlint` Gradle 任务本身可能需要在 Git Bash 或类似的 Unix 环境下运行才能正确执行**。

如果直接在 Android Studio (未使用 Git Bash 作为终端) 中提交时遇到 pre-commit 错误，请尝试：
1.  在 **Git Bash** 中运行 `./gradlew ktlintFormat` 手动修复格式。
2.  在 **Git Bash** 中运行 `git add .` 重新暂存文件。
3.  在 **Git Bash** 中运行 `git commit` 再次提交。
或者，配置 Android Studio 使用 Git Bash 作为其内置终端。

## Git Hooks (自动安装)

本项目使用 Gradle 插件自动管理 Git hooks。**确保在开始工作前至少执行一次 Gradle Sync (通常在打开项目时 Android Studio 会自动执行)。**

每次提交 (`git commit`) 时，`pre-commit` hook 会自动执行以下操作：
1.  检查暂存区是否有 Kotlin, Java, 或 XML 文件被修改。
2.  运行 `./gradlew ktlintCheck` 检查代码格式。
3.  如果检查失败，提交将被阻止。
4.  运行 `./gradlew ktlintFormat` 自动格式化代码。
5.  将格式化后的文件重新添加到暂存区。

## 代码风格

本项目使用 ktlint 进行代码格式化和检查，遵循 `.editorconfig` 中的规则。

## 开发流程

1.  **克隆项目后：** 确保 Android Studio 完成了 Gradle Sync。
Hooks 会自动安装到 `.git/hooks/pre-commit`。
2.  **日常开发：** 正常编写代码。
3.  **提交代码：** 使用 `git commit` (通过命令行或 Android Studio)。pre-commit hook 会自动运行。
4.  **手动检查/格式化 (可选)：**
    ```bash
    # 检查格式 (建议在 Git Bash 中运行)
    ./gradlew ktlintCheck

    # 格式化代码 (建议在 Git Bash 中运行)
    ./gradlew ktlintFormat
    ```

## IDE 设置

### Android Studio

1. 安装 "Ktlint (unofficial)" 插件 (可选，但推荐实时反馈)。
2. (推荐) 配置 Android Studio 使用 Git Bash 作为终端：File -> Settings -> Tools -> Terminal -> Shell path (设置为你的 Git Bash路径，例如 `C:\Program Files\Git\bin\bash.exe`)。

### VS Code

1. 安装 "Ktlint" 扩展 (可选)。

## 常见问题

1.  **提交时失败并提示格式错误：**
    *   通常 hook 会自动格式化。如果仍然失败，尝试在 **Git Bash** 中运行 `./gradlew ktlintFormat`，然后 `git add .`，再重新 `git commit`。
    *   检查 Gradle 输出获取详细错误信息。
2.  **Hooks 没有生效：**
    *   确保 Gradle Sync 已成功完成。
    *   检查 `.git/hooks/pre-commit` 文件是否存在并且内容是由 Gradle 插件生成的。
    *   尝试手动运行 `installGitHooks` Gradle 任务：`./gradlew installGitHooks` (在 Git Bash 中)。
3.  **Windows 环境下 Hook 脚本报错 (非格式问题)：**
    *   确认您是通过 Git Bash 执行的提交，或者 Android Studio 的终端配置为 Git Bash。
    *   脚本中的 `./gradlew` 命令可能需要 Unix 环境才能正确执行 ktlint。