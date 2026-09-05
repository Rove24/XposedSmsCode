# XposedSmsCode

[![Latest Release](https://img.shields.io/github/v/release/rove24/XposedSmsCode?label=Latest%20Release)](https://github.com/rove24/XposedSmsCode/releases)
[![License](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)
[![API Level](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://developer.android.com)
[![LibXposed](https://img.shields.io/badge/LibXposed-API%20102-orange.svg)](https://github.com/libxposed)

An Xposed module that automatically recognizes and parses SMS verification codes, copies them to the clipboard, and auto-fills them into active text fields.

Crafted with **Material Design 3** (deeply aligned with Google Messages settings UX, supporting Android 12+ Monet dynamic theming), and modernized with the **LibXposed Modern Module API 102** standard.

[中文版说明 (Chinese README)](./README-CN.md)

---

## ✨ Features

- 📋 **Auto-Copy Verification Code**: Extracts and copies SMS verification codes to the system clipboard in milliseconds.
- ⚡ **Auto-Input Code**: Automatically detects active input fields and inputs the verification code (with customizable delay).
- 🔔 **Verification Code Notifications**: Displays clean Material 3 notifications with one-tap copy and configurable auto-dismiss.
- 💬 **Lightweight Toast**: Shows a clean Material 3 Toast with the extracted code upon arrival.
- 🚫 **SMS Blocking & Deduplication**: Block verification SMS notifications to prevent distractions; filter repeated SMS within short intervals.
- ⚡ **Zero-Delay Cross-Process Hot Sync**: All toggle switches sync instantly across processes via Intent Bundle broadcast, taking effect **immediately without rebooting**.
- 🎨 **Native Material Design 3 UI**:
  - Full Android 12+ Monet dynamic color extraction (Material You).
  - Matches Google Messages settings layout (smooth rounded cards, M3 switches, M3 badge status indicators).
  - Custom M3-styled dialogs for radio options and delay inputs.
- 🛡️ **Modern LSPosed Static Scope**:
  - Recommended scope: `Phone Services` (`com.android.phone`) & `System Framework` (`system` / `android`).
  - Powered by system-level `Settings.Global` state and multi-layer context capture for instant boot-time activation.
- 📝 **Custom Matching Rules**:
  - Built-in high-accuracy regular expressions.
  - Supports custom keywords and custom regex patterns.
  - Import, export, and backup custom rule sets.
- 📜 **Verification History**: Stores local records of received verification codes for easy retrieval.

---

## 📱 Recommended Scope (LSPosed)

This module recommends and comes configured with LSPosed static scope:

1. **Phone Services** (`com.android.phone`)
2. **System Framework** (`system` / `android`)

> [!NOTE]
> After enabling the recommended scope, simply reboot once to activate. Once activated, any setting or switch changed in the app takes effect **immediately in real time** without requiring any further reboots!

---

## 🛠️ Compatibility

- **Android Version**: Android 6.0 (API 23) through Android 14+ (API 34)
- **Supported Frameworks**: LSPosed (Recommended), EdXposed, TaiChi·Magisk, Traditional Xposed
- **ROM Support**: AOSP, Pixel OS, LineageOS, and most third-party customized ROMs

---

## 📥 Download

- **[GitHub Releases](https://github.com/rove24/XposedSmsCode/releases)**
- **LSPosed Module Repository**

---

## 🔨 Building From Source

Built with standard Gradle:

```bash
# Clone the repository
git clone https://github.com/rove24/XposedSmsCode.git
cd XposedSmsCode

# Build Release APK
./gradlew assembleRelease
```

The compiled release APK will be located at `app/build/outputs/apk/release/XposedSmsCode.apk`.

---

## 📜 Changelog

See [Changelog (LOG-EN.md)](./LOG-EN.md) / [中文更新日志 (LOG-CN.md)](./LOG-CN.md).

---

## 🙏 Credits & Acknowledgments

- [LibXposed](https://github.com/libxposed)
- [LSPosed](https://github.com/LSPosed/LSPosed)
- [Xposed](https://github.com/rovo89/Xposed)
- [NekoSMS](https://github.com/apsun/NekoSMS)
- Original project by [tianma8023/XposedSmsCode](https://github.com/tianma8023/XposedSmsCode)

---

## 📄 License

All source code is licensed under the [GPLv3](LICENSE) License. 