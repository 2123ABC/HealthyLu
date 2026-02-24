# 健康🦌管

一款帮助用户养成健康生活习惯的 Android 打卡应用。

## 功能特性

### 核心功能
- **打卡记录** - 记录每日打卡次数，支持撤销操作
- **数据统计** - 查看今日和本周打卡统计
- **阶段提醒** - 根据打卡次数显示不同的提醒和状态图片

### 提醒功能
- **定时提醒** - 通过通知提醒用户打卡
- **应用提醒** - 检测前台应用，在特定场景下提醒用户
- **开机自启** - 支持开机后自动启动后台服务

### 个性化设置
- **主题模式** - 支持白天模式、黑夜模式、跟随系统三种主题
- **功能开关** - 可自由开启/关闭开机启动、后台驻留、应用提醒等功能

### 其他功能
- **数据安全** - 使用 EncryptedSharedPreferences 加密存储敏感数据
- **日志记录** - 完整的应用日志，支持查看和清除

### 主要依赖
- AndroidX Core KTX
- AndroidX Lifecycle (ViewModel, LiveData)
- AndroidX Security Crypto (数据加密)
- AndroidX AppCompat & Material Design
- QMUI Android 组件库
- ColorPickerView 颜色选择器

## 权限说明

| 权限 | 用途 |
|------|------|
| 通知权限 | 显示打卡提醒通知 |
| 使用情况访问权限 | 检测前台应用以提供智能提醒 |
| 自启动权限 | 开机后自动运行后台服务 |
| 精确闹钟权限 | 定时发送提醒通知 |

## 安装

1. Clone 仓库
```bash
git clone https://github.com/2123ABC/HealthyLu.git
```

2. 使用 Android Studio 打开项目
3. 同步 Gradle 依赖
4. 编译并安装到设备
## 项目结构

```
app/src/main/java/ccb/crayalsokakamiee/healthylu/
├── MainActivity.kt           # 主Activity
├── CheckInFragment.kt        # 打卡页面
├── CheckInViewModel.kt       # 打卡数据管理
├── TotalFragment.kt          # 统计页面
├── SettingsFragment.kt       # 设置页面
├── WaterRecordManager.kt     # 记录数据管理
├── WaterReminderService.kt   # 后台提醒服务
├── WaterReminderReceiver.kt  # 提醒广播接收器
├── DrinkWaterReceiver.kt     # 打卡广播接收器
├── BootReceiver.kt           # 开机启动接收器
├── ThemeManager.kt           # 主题管理
├── AppSettingsManager.kt     # 应用设置管理
├── BackgroundManager.kt      # 后台服务管理
├── LogManager.kt             # 日志管理
├── CrashHandler.kt           # 崩溃处理
├── FireworkRing.kt           # 烟花动画组件
└── HealthyLuApplication.kt   # Application类
```

## 作者

**HengryCray (Also Kakamiee and MekoNacho)**

- B站主页: [@474494752](https://space.bilibili.com/474494752)

此README由GLM5生成

## 许可证

MIT License
