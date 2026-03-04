# RootShellRunner 🚀

RootShellRunner 是一款专为安卓 Root 用户设计的现代化、扁平化的脚本执行工具。它允许你将复杂的 Shell 脚本路径保存为直观的桌面按钮，并通过交互式终端实时监控输出。

## ✨ 功能特性

- **现代 UI 设计**：采用 Material 3 (M3) 设计规范，支持扁平化卡片布局与动态配色。
- **脚本持久化**：一键保存脚本路径与名称，自动生成执行按钮，无需重复输入。
- **交互式终端**：经典黑底白字风格，支持实时输出滚动及命令手动输入（Standard Input）。
- **一键强杀**：集成"结束脚本"功能，随时停止失控的进程。
- **自动编译**：项目集成了 GitHub Actions，只要提交代码，云端自动构建 APK。
- **极简架构**：基于 Kotlin + ViewBinding + Coroutines 编写，响应迅速不卡顿。

## 🛠️ 项目结构

```
RootShellRunner/
├── .github/workflows/   # CI/CD 自动编译脚本
├── app/
│   ├── src/main/
│   │   ├── java/        # Kotlin 核心逻辑
│   │   └── res/         # Material 3 资源与布局
│   └── build.gradle     # 构建配置
├── gradlew              # 构建引导脚本
└── README.md            # 项目文档
```

## 🚀 快速开始

### 1. 构建 APK

你可以直接在本地使用 Gradle 构建，或者利用 GitHub 的 Actions 功能。

**本地编译：**

```bash
chmod +x gradlew
./gradlew assembleDebug
```

编译完成后的 APK 路径：`app/build/outputs/apk/debug/app-debug.apk`

**GitHub 自动编译：**

1. 推送代码到你的仓库。
2. 点击仓库顶部的 **Actions** 选项卡。
3. 找到最近的一次构建任务，在 **Artifacts** 处下载 APK。

### 2. 使用说明

1. **授予权限**：首次打开请授予 Root (SU) 权限。
2. **添加脚本**：在主界面输入脚本名称（如：清理系统）和脚本绝对路径（如：`/data/local/tmp/clean.sh`）。
3. **运行脚本**：点击下方生成的按钮即可进入终端界面。
4. **交互与结束**：在终端下方可输入额外参数或命令，点击右上角红色按钮可强制结束脚本。

## ⚠️ 免责声明

> [!CAUTION]
> **玩火注意**：本程序运行在 Root 权限下，执行任何脚本均具有破坏系统文件的潜力。作者"未闻花名"不对因使用本程序或脚本导致的死机、变砖或数据丢失负责。请在执行脚本前务必了解脚本的具体内容。

## 👨‍💻 关于作者

- **作者**：未闻花名
- **项目地址**：https://github.com/linjunyou04/RootShellRunner

## 📜 许可证

本项目基于 MIT License 开源。

---

如果你觉得这个项目对你有帮助，欢迎点个 ⭐ Star！
