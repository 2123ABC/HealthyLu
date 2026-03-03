package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * 语言管理器
 * 管理应用的语言设置
 */
object LanguageManager {
    
    private const val PREFS_NAME = "language_settings"
    private const val KEY_LANGUAGE = "selected_language"
    
    // 支持的语言
    const val LANGUAGE_SYSTEM = "system"  // 跟随系统
    const val LANGUAGE_CHINESE = "zh"     // 中文
    const val LANGUAGE_ENGLISH = "en"     // 英文
    
    /**
     * 获取当前保存的语言设置
     */
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }
    
    /**
     * 保存语言设置
     */
    fun setLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }
    
    /**
     * 获取应用语言的Context
     * 在Activity的attachBaseContext中调用
     */
    fun wrapContext(context: Context): Context {
        val savedLanguage = getSavedLanguage(context)
        
        val locale = when (savedLanguage) {
            LANGUAGE_CHINESE -> Locale.CHINESE
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            else -> getSystemLocale()  // 跟随系统
        }
        
        return updateResources(context, locale)
    }
    
    /**
     * 获取系统当前语言
     */
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Resources.getSystem().configuration
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            Locale.getDefault()
        }
    }
    
    /**
     * 更新资源的语言设置
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 获取所有支持的语言列表
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            Pair(LANGUAGE_SYSTEM, "跟随系统"),
            Pair(LANGUAGE_CHINESE, "中文"),
            Pair(LANGUAGE_ENGLISH, "English")
        )
    }
}
