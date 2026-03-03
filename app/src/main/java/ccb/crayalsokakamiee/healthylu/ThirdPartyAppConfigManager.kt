package ccb.crayalsokakamiee.healthylu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 第三方应用配置管理器
 * 管理第三方应用包名和自定义信息的配置
 */
object ThirdPartyAppConfigManager {
    
    private const val PREFS_NAME = "third_party_app_config"
    private const val KEY_CONFIGS = "configs"
    private const val KEY_INITIALIZED = "default_initialized"
    
    /**
     * 默认配置列表
     */
    private val DEFAULT_CONFIGS = listOf(
        ThirdPartyAppConfig(
            packageName = "com.xjs.ehviewer",
            displayName = "EHViewer",
            customInfos = mutableListOf(
                "EHViewer启动！",
                "今天来的大家想看的东西啊",
                "无码同人志！爽！"
            )
        ),
        ThirdPartyAppConfig(
            packageName = "com.perol.pixez",
            displayName = "PixEz",
            customInfos = mutableListOf(
                "P站启动！",
                "不用翻墙就是爽！",
                "来点色图"
            )
        ),
        ThirdPartyAppConfig(
            packageName = "com.JMComic3.app",
            displayName = "JMComic",
            customInfos = mutableListOf(
                "禁漫！爽！",
                "记得去签到",
                "向传奇禁漫汉化组致敬！"
            )
        ),
        ThirdPartyAppConfig(
            packageName = "com.picacomic.fregata",
            displayName = "哔咔",
            customInfos = mutableListOf(
                "哔咔启动！",
                "记得签到",
                "看漫画的好地方"
            )
        ),
        ThirdPartyAppConfig(
            packageName = "sg.jxrgq.wbbzrf",
            displayName = "91",
            customInfos = mutableListOf(
                "7891",
                "第91号隐私协议启动",
                "警惕91破解版！虽然你可能听不进去但还是警告一下"
            )
        ),
        ThirdPartyAppConfig(
            packageName = "jp.pxv.android",
            displayName = "Pixiv",
            customInfos = mutableListOf(
                "Pixiv——日本最大的插画交流网站",
                "如果你只是来看二次元图片的话请把这个消息划掉",
                "抱着普通看图的心情的话请无视这条消息"
            )
        )
    )
    
    /**
     * 获取 SharedPreferences
     */
    private fun getPrefs(context: Context) = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 确保默认配置已初始化
     */
    private fun ensureDefaultConfigsInitialized(context: Context) {
        val prefs = getPrefs(context)
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            // 第一次运行，保存默认配置
            saveAllConfigs(context, DEFAULT_CONFIGS)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
            android.util.Log.d("ThirdPartyAppConfigManager", "Default configs initialized")
        }
    }
    
    /**
     * 获取所有配置
     */
    fun getAllConfigs(context: Context): MutableList<ThirdPartyAppConfig> {
        // 确保默认配置已初始化
        ensureDefaultConfigsInitialized(context)
        
        val prefs = getPrefs(context)
        val configsJson = prefs.getString(KEY_CONFIGS, null) ?: return mutableListOf()
        
        return try {
            val jsonArray = JSONArray(configsJson)
            val configs = mutableListOf<ThirdPartyAppConfig>()
            for (i in 0 until jsonArray.length()) {
                configs.add(ThirdPartyAppConfig.fromJson(jsonArray.getJSONObject(i)))
            }
            configs
        } catch (e: Exception) {
            android.util.Log.e("ThirdPartyAppConfigManager", "Failed to parse configs: ${e.message}")
            mutableListOf()
        }
    }
    
    /**
     * 保存所有配置
     */
    fun saveAllConfigs(context: Context, configs: List<ThirdPartyAppConfig>) {
        val jsonArray = JSONArray()
        configs.forEach { config ->
            jsonArray.put(config.toJson())
        }
        getPrefs(context).edit().putString(KEY_CONFIGS, jsonArray.toString()).apply()
    }
    
    /**
     * 添加配置
     */
    fun addConfig(context: Context, config: ThirdPartyAppConfig) {
        val configs = getAllConfigs(context)
        // 检查是否已存在相同包名的配置
        val existingIndex = configs.indexOfFirst { it.packageName == config.packageName }
        if (existingIndex >= 0) {
            configs[existingIndex] = config
        } else {
            configs.add(config)
        }
        saveAllConfigs(context, configs)
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(context: Context, config: ThirdPartyAppConfig) {
        val configs = getAllConfigs(context)
        val index = configs.indexOfFirst { it.packageName == config.packageName }
        if (index >= 0) {
            configs[index] = config
            saveAllConfigs(context, configs)
        }
    }
    
    /**
     * 删除配置
     */
    fun deleteConfig(context: Context, packageName: String) {
        val configs = getAllConfigs(context)
        configs.removeAll { it.packageName == packageName }
        saveAllConfigs(context, configs)
    }
    
    /**
     * 根据包名获取配置
     */
    fun getConfigByPackageName(context: Context, packageName: String): ThirdPartyAppConfig? {
        return getAllConfigs(context).find { it.packageName == packageName }
    }
    
    /**
     * 导出配置到JSON文件
     * @return 导出的文件路径，失败返回null
     */
    fun exportConfigToFile(context: Context, filePath: String): Boolean {
        return try {
            val configs = getAllConfigs(context)
            val jsonArray = JSONArray()
            configs.forEach { config ->
                jsonArray.put(config.toJson())
            }
            
            val rootJson = JSONObject().apply {
                put("version", 1)
                put("appName", "HealthyLu")
                put("configs", jsonArray)
            }
            
            val file = File(filePath)
            FileOutputStream(file).use { fos ->
                fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ThirdPartyAppConfigManager", "Export failed: ${e.message}")
            false
        }
    }
    
    /**
     * 从JSON文件导入配置
     * @param replaceExisting 是否替换现有配置，false则合并
     * @return 导入是否成功
     */
    fun importConfigFromFile(context: Context, filePath: String, replaceExisting: Boolean = false): ImportResult {
        return try {
            val file = File(filePath)
            val content = FileInputStream(file).use { fis ->
                fis.readBytes().toString(Charsets.UTF_8)
            }
            
            val rootJson = JSONObject(content)
            val configsArray = rootJson.getJSONArray("configs")
            
            val importedConfigs = mutableListOf<ThirdPartyAppConfig>()
            for (i in 0 until configsArray.length()) {
                importedConfigs.add(ThirdPartyAppConfig.fromJson(configsArray.getJSONObject(i)))
            }
            
            if (replaceExisting) {
                saveAllConfigs(context, importedConfigs)
                ImportResult(true, importedConfigs.size, 0)
            } else {
                // 合并配置
                val existingConfigs = getAllConfigs(context)
                var addedCount = 0
                var updatedCount = 0
                
                importedConfigs.forEach { imported ->
                    val existingIndex = existingConfigs.indexOfFirst { it.packageName == imported.packageName }
                    if (existingIndex >= 0) {
                        existingConfigs[existingIndex] = imported
                        updatedCount++
                    } else {
                        existingConfigs.add(imported)
                        addedCount++
                    }
                }
                
                saveAllConfigs(context, existingConfigs)
                ImportResult(true, addedCount, updatedCount)
            }
        } catch (e: Exception) {
            android.util.Log.e("ThirdPartyAppConfigManager", "Import failed: ${e.message}")
            ImportResult(false, 0, 0, e.message)
        }
    }
    
    /**
     * 导入结果
     */
    data class ImportResult(
        val success: Boolean,
        val addedCount: Int,
        val updatedCount: Int,
        val errorMessage: String? = null
    )
}
