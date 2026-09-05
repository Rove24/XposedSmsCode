# XposedSmsCode v0.0.13 (Version 13)

### 🎨 Material Design 3 界面升级
- 全面适配原生 Material Design 3（MD3）风格规范。
- 深度对齐 Google 信息设置界面的视觉与交互体验（平滑圆角卡片、M3 开关组件、M3 徽章状态指示）。
- 全面支持 Android 12+ Monet 原生动态取色（Material You），跟随壁纸系统主题色自适应流转。
- 采用全新 M3 风格单选对话框与自定义数值调节面板。

### ⚡ 实时热生效与跨进程同步
- 新增全配置项跨进程实时热同步（Intent Bundle 广播 + 电话服务私有数据持久化）。
- 界面内切换任何开关（显示 Toast、复制验证码、拦截短信、自动输入、显示通知、历史记录等）**0ms 立即热生效**，无需重启设备或电话服务。

### 🛡️ LibXposed Modern API 102 架构与作用域
- 全面升级至现代 LibXposed API 102 标准架构。
- 适配现代 LSPosed 静态作用域机制，推荐且默认作用域：
  - **电话服务** (com.android.phone)
  - **系统框架** (system / ndroid)
- 深度重构激活检测链路，采用系统框架层 Settings.Global 全局状态共享与电话服务多层生命周期 Context 捕获，彻底解决重启或卸载重装后偶现的 未激活误报。

### 🔧 系统与兼容性优化
- 适配 Android 14+ 广播导出控制规范（RECEIVER_EXPORTED）。
- 优化 NotificationChannel 创建与动态更新机制。
- 适配点击通知直接复制并显示 Toast 的全流程动作。
