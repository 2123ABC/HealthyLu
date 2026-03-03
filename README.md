# HealthyLu 🦌

一款帮助用户养成健康生活习惯的 Android 打卡应用。

## ✨ 功能特性

### 📝 打卡记录
- 记录每日打卡次数，支持撤销操作
- 查看今日和本周打卡统计
- 根据打卡次数显示不同的提醒和状态图片

### 🔔 智能提醒
- **定时提醒** - 通过通知提醒用户打卡
- **应用检测提醒** - 检测前台应用，在特定场景下提醒用户
- **自定义提醒语** - 支持为每个第三方应用设置专属提醒文案
- **冷却机制** - 同一应用5分钟内不重复提醒

### 🛠️ 第三方应用配置
- 内置 6 款常用应用预设
- 支持自定义添加第三方应用
- 支持导入/导出配置文件
- 配置即时生效

### 🎨 个性化设置
- **多主题模式** - 白天模式、黑夜模式、跟随系统
- **多语言支持** - 中文、English
- **功能开关** - 开机启动、后台驻留、应用提醒等

### 🔒 安全特性
- 使用 EncryptedSharedPreferences 加密存储敏感数据
- 完整的应用日志记录
- 崩溃处理机制

## 📱 预置支持的应用

| 应用 | 包名 |
|------|------|
| EHViewer | com.xjs.ehviewer |
| PixEz | com.perol.pixez |
| JMComic | com.JMComic3.app |
| 哔咔 | com.picacomic.fregata |
| 91 | sg.jxrgq.wbbzrf |
| Pixiv | jp.pxv.android |

> 💡 用户可在设置中自定义添加更多应用

## 🔑 权限说明

| 权限 | 用途 |
|------|------|
| 通知权限 | 显示打卡提醒通知 |
| 使用情况访问权限 | 检测前台应用以提供智能提醒 |
| 自启动权限 | 开机后自动运行后台服务 |
| 精确闹钟权限 | 设置精确的定时提醒通知 |

## 📦 安装

### 方式一：直接下载
前往 [Releases](https://github.com/2123ABC/HealthyLu/releases) 页面下载最新版本 APK

### 方式二：自行编译
```bash
# 克隆仓库
git clone https://github.com/2123ABC/HealthyLu.git

# 使用 Android Studio 打开项目
# 同步 Gradle 依赖后编译安装
```

## 📁 项目结构

```
app/src/main/java/ccb/crayalsokakamiee/healthylu/
├── MainActivity.kt               # 主Activity
├── CheckInFragment.kt            # 打卡页面
├── CheckInViewModel.kt           # 打卡数据管理
├── TotalFragment.kt              # 统计页面
├── SettingsFragment.kt           # 设置页面
├── WaterRecordManager.kt         # 记录数据管理
├── WaterReminderService.kt       # 后台提醒服务
├── ThirdPartyAppConfig.kt        # 第三方应用配置模型
├── ThirdPartyAppConfigManager.kt # 第三方应用配置管理
├── ThirdPartyAppConfigActivity.kt# 第三方应用配置界面
├── ThemeManager.kt               # 主题管理
├── AppSettingsManager.kt         # 应用设置管理
└── ...
```

## 🛠️ 技术栈

- **Kotlin** - 主要开发语言
- **AndroidX** - 现代化 Android 开发组件
- **ViewModel & LiveData** - MVVM 架构
- **EncryptedSharedPreferences** - 数据加密存储
- **QMUI Android** - UI 组件库
- **Material Design** - 设计规范

## 📄 许可证

MIT License

## 👤 作者

**HengryCray (Also Kakamiee and MekoNacho)**

- B站: [@474494752](https://space.bilibili.com/474494752)

---

> ⚠️ **免责声明**：本应用仅供个人学习和娱乐使用，请勿用于任何违法用途。使用第三方应用时请注意安全，谨慎授予敏感权限（如无障碍服务）。
