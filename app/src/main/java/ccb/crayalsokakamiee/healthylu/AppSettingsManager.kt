package ccb.crayalsokakamiee.healthylu

import android.content.Context

/**
 * 应用功能设置管理器
 * 管理开机启动、后台驻留、应用提醒等功能开关
 */
object AppSettingsManager {
    
    private const val PREFS_NAME = "app_feature_settings"
    
    // 设置键名
    private const val KEY_BOOT_STARTUP = "boot_startup_enabled"
    private const val KEY_BACKGROUND_SERVICE = "background_service_enabled"
    private const val KEY_APP_REMINDER = "app_reminder_enabled"
    private const val KEY_REMINDER_INTERVAL = "reminder_interval_minutes"
    
    // 默认值（全部默认启用）
    private const val DEFAULT_BOOT_STARTUP = true
    private const val DEFAULT_BACKGROUND_SERVICE = true
    private const val DEFAULT_APP_REMINDER = true
    private const val DEFAULT_REMINDER_INTERVAL = 60.0f // 默认一小时（分钟）
    
    /**
     * 获取 SharedPreferences
     */
    private fun getPrefs(context: Context) = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 是否启用开机启动
     */
    fun isBootStartupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BOOT_STARTUP, DEFAULT_BOOT_STARTUP)
    }
    
    /**
     * 设置开机启动开关
     */
    fun setBootStartupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BOOT_STARTUP, enabled).apply()
    }
    
    /**
     * 是否启用后台驻留服务
     */
    fun isBackgroundServiceEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BACKGROUND_SERVICE, DEFAULT_BACKGROUND_SERVICE)
    }
    
    /**
     * 设置后台驻留开关
     */
    fun setBackgroundServiceEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BACKGROUND_SERVICE, enabled).apply()
    }
    
    /**
     * 是否启用应用提醒（检测特定应用并提醒）
     */
    fun isAppReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_APP_REMINDER, DEFAULT_APP_REMINDER)
    }
    
    /**
     * 设置应用提醒开关
     */
    fun setAppReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_APP_REMINDER, enabled).apply()
    }
    
    /**
     * 获取定时提醒间隔（分钟）
     */
    fun getReminderIntervalMinutes(context: Context): Float {
        return getPrefs(context).getFloat(KEY_REMINDER_INTERVAL, DEFAULT_REMINDER_INTERVAL)
    }
    
    /**
     * 设置定时提醒间隔（分钟）
     */
    fun setReminderIntervalMinutes(context: Context, intervalMinutes: Float) {
        getPrefs(context).edit().putFloat(KEY_REMINDER_INTERVAL, intervalMinutes).apply()
    }
}
