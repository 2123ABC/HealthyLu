package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题管理器 - 管理应用的主题模式
 */
object ThemeManager {
    private const val PREF_NAME = "theme_settings"
    private const val KEY_THEME_MODE = "theme_mode"

    const val THEME_MODE_LIGHT = 0      // 白天模式
    const val THEME_MODE_DARK = 1       // 黑夜模式
    const val THEME_MODE_SYSTEM = 2     // 跟随系统

    /**
     * 获取当前主题模式
     */
    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM)
    }

    /**
     * 设置主题模式
     */
    fun setThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        applyTheme(mode)
    }

    /**
     * 应用主题
     */
    fun applyTheme(mode: Int) {
        val nightMode = when (mode) {
            THEME_MODE_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_MODE_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            THEME_MODE_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /**
     * 应用保存的主题
     */
    fun applySavedTheme(context: Context) {
        val mode = getThemeMode(context)
        applyTheme(mode)
    }

    /**
     * 获取主题模式名称
     */
    fun getThemeModeName(mode: Int): String {
        return when (mode) {
            THEME_MODE_LIGHT -> "白天模式"
            THEME_MODE_DARK -> "黑夜模式"
            THEME_MODE_SYSTEM -> "跟随系统"
            else -> "跟随系统"
        }
    }
}