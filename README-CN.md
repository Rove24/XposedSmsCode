# XposedSmsCode (短信验证码)

[![Latest Release](https://img.shields.io/github/v/release/rove24/XposedSmsCode?label=Latest%20Release)](https://github.com/rove24/XposedSmsCode/releases)
[![License](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)
[![API Level](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com)
[![LibXposed](https://img.shields.io/badge/LibXposed-API%20102-orange.svg)](https://github.com/libxposed)

识别短信验证码的 Xposed 模块，支持自动复制验证码到剪切板，亦可自动在目标输入框中填充验证码。

采用现代 **Material Design 3** 规范设计（深度对齐原生 Google 信息设置体验，支持 Android 12+ Monet 动态取色），底层全面升级至 **LibXposed 现代模块 API 102** 标准。

[English README](./README.md)

---

## ✨ 核心特性

- 📋 **自动复制验证码**：收到验证码短信后毫秒级提取并复制至系统剪切板。
- ⚡ **自动输入验证码**：自动检测前台活动输入框并模拟输入验证码（支持自定义延迟）。
- 🔔 **常驻验证码通知**：生成 Material 3 风格验证码通知，支持一键点击复制、定时自动清除。
- 💬 **轻量 Toast 提示**：提取成功后弹出 Material 3 Toast 告知验证码内容。
- 🚫 **短信拦截与去重**：支持验证码短信拦截防骚扰，可过滤短时间内重复发送的验证码。
- ⚡ **跨进程实时热生效**：全设置项支持广播瞬时同步，修改任何开关 **0ms 即刻生效**，无需重启手机或电话服务。
- 🎨 **Material Design 3 原生界面**：
  - 支持 Android 12+ Monet 原生动态取色（Material You）。
  - 对齐 Google 信息设置布局规范（平滑圆角卡片、M3 开关、M3 徽章状态指示）。
  - 自定义 M3 风格单选与延迟调节对话框。
- 🛡️ **现代 LSPosed 静态作用域**：
  - 推荐作用域：`电话服务` (`com.android.phone`) 与 `系统框架` (`system`)。
  - 采用 `Settings.Global` 系统级全局标志 + 多重 Context 捕获，开机即激活，秒级绿标。
- 📝 **匹配规则高度可定制**：
  - 内置丰富高精度正则匹配规则。
  - 支持自定义短信验证码关键字及正则表达式。
  - 支持验证码规则的导入、导出与备份。
- 📜 **验证码历史记录**：本地保存历史验证码记录，方便随时回溯查验。

---

## 📱 推荐作用域 (LSPosed)

本模块推荐并默认配置了 LSPosed 静态作用域，无需勾选多余应用：

1. **电话服务** (`com.android.phone`)
2. **系统框架** (`system` / `android`)

> [!NOTE]
> 安装并勾选推荐作用域后，仅需重启一次手机即可完成激活。之后在应用内调整任何开关设置均**实时生效**，不再需要重启！

---

## 🛠️ 兼容性

- **系统版本**：Android 6.0 (API 23) ～ Android 14+ (API 34)
- **支持框架**：LSPosed (推荐)、EdXposed、太极·Magisk、传统 Xposed
- **运行环境**：原生 AOSP、Pixel OS、LineageOS 及绝大部分第三方类原生与定制系统

---

## 📥 下载安装

- **[GitHub Releases](https://github.com/rove24/XposedSmsCode/releases)**（推荐，获取最新稳定版构建）
- **LSPosed 模块仓库**

---

## 🔨 从源码构建

本项目采用标准 Gradle 构建系统：

```bash
# 克隆仓库
git clone https://github.com/rove24/XposedSmsCode.git
cd XposedSmsCode

# 构建 Release APK
./gradlew assembleRelease
```

构建产物位于 `app/build/outputs/apk/release/XposedSmsCode.apk`。

---

## 📜 更新日志

详见 [更新日志 (LOG-CN.md)](./LOG-CN.md)。

---

## 🙏 鸣谢与开源依赖

- [LibXposed](https://github.com/libxposed)
- [LSPosed](https://github.com/LSPosed/LSPosed)
- [Xposed](https://github.com/rovo89/Xposed)
- [NekoSMS](https://github.com/apsun/NekoSMS)
- 原作者 [tianma8023/XposedSmsCode](https://github.com/tianma8023/XposedSmsCode)

---

## 📄 开源协议

本项目所有源码遵循 [GPLv3](LICENSE) 开源协议。