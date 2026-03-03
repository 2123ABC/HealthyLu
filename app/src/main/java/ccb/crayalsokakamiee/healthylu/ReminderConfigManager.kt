package ccb.crayalsokakamiee.healthylu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 提醒文案配置管理器
 * 管理提醒文案的配置、导入导出
 */
object ReminderConfigManager {
    
    private const val PREFS_NAME = "reminder_config"
    private const val KEY_CONFIGS = "configs"
    private const val KEY_INITIALIZED = "default_initialized"
    
    /**
     * 默认文案列表
     */
    private val DEFAULT_CONFIGS = listOf(
        ReminderConfig(id = "default_1", message = "这周还没有鹿管哦，记得鹿管保持性福！", enabled = true),
        ReminderConfig(id = "default_2", message = "？快去鹿管吧~", enabled = true),
        ReminderConfig(id = "default_3", message = "Luguanluguanlulushijiandaole", enabled = true),
        ReminderConfig(id = "default_4", message = "美好一周从鹿管开始，这周你鹿管了吗？", enabled = true),
        ReminderConfig(id = "default_5", message = "你在呼唤鹿管~", enabled = true),
        ReminderConfig(id = "default_6", message = "鹿管提醒：该去🦌一发啦！", enabled = true),
        ReminderConfig(id = "default_7", message = "保持鹿管，精力充沛！", enabled = true),
        ReminderConfig(id = "default_8", message = "一个小时过去了，两个小时过去了，三个小时过去了...\n你这周还没有鹿过管哦", enabled = true),
        ReminderConfig(id = "default_9", message = "我说三顾茅庐来四次有没有懂的？", enabled = true)
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
            saveAllConfigs(context, DEFAULT_CONFIGS)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
            android.util.Log.d("ReminderConfigManager", "Default configs initialized")
        }
    }
    
    /**
     * 获取所有配置
     */
    fun getAllConfigs(context: Context): MutableList<ReminderConfig> {
        ensureDefaultConfigsInitialized(context)
        
        val prefs = getPrefs(context)
        val configsJson = prefs.getString(KEY_CONFIGS, null) ?: return mutableListOf()
        
        return try {
            val jsonArray = JSONArray(configsJson)
            val configs = mutableListOf<ReminderConfig>()
            for (i in 0 until jsonArray.length()) {
                configs.add(ReminderConfig.fromJson(jsonArray.getJSONObject(i)))
            }
            configs.toMutableList()
        } catch (e: Exception) {
            android.util.Log.e("ReminderConfigManager", "Failed to parse configs: ${e.message}")
            mutableListOf()
        }
    }
    
    /**
     * 获取启用的配置（启用的文案列表）
     */
    fun getEnabledConfigs(context: Context): List<ReminderConfig> {
        return getAllConfigs(context).filter { it.enabled }
    }
    
    /**
     * 获取启用的文案列表
     */
    fun getEnabledMessages(context: Context): List<String> {
        return getEnabledConfigs(context).map { it.message }
    }
    
    /**
     * 保存所有配置
     */
    fun saveAllConfigs(context: Context, configs: List<ReminderConfig>) {
        val jsonArray = JSONArray()
        configs.forEach { config ->
            jsonArray.put(config.toJson())
        }
        getPrefs(context).edit().putString(KEY_CONFIGS, jsonArray.toString()).apply()
    }
    
    /**
     * 添加配置
     */
    fun addConfig(context: Context, config: ReminderConfig) {
        val configs = getAllConfigs(context)
        configs.add(config)
        saveAllConfigs(context, configs)
    }
    
    /**
     * 更新配置
     */
    fun updateConfig(context: Context, config: ReminderConfig) {
        val configs = getAllConfigs(context)
        val index = configs.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            configs[index] = config
            saveAllConfigs(context, configs)
        }
    }
    
    /**
     * 删除配置
     */
    fun deleteConfig(context: Context, id: String) {
        val configs = getAllConfigs(context)
        configs.removeAll { it.id == id }
        saveAllConfigs(context, configs)
    }
    
    /**
     * 根据ID获取配置
     */
    fun getConfigById(context: Context, id: String): ReminderConfig? {
        return getAllConfigs(context).find { it.id == id }
    }
    
    /**
     * 切换配置启用状态
     */
    fun toggleConfigEnabled(context: Context, id: String) {
        val configs = getAllConfigs(context)
        val index = configs.indexOfFirst { it.id == id }
        if (index >= 0) {
            val config = configs[index]
            configs[index] = config.copy(enabled = !config.enabled)
            saveAllConfigs(context, configs)
        }
    }
    
    /**
     * 导出配置到JSON字符串
     */
    fun exportConfigToJson(context: Context): String {
        val configs = getAllConfigs(context)
        val jsonArray = JSONArray()
        configs.forEach { config ->
            jsonArray.put(config.toJson())
        }
        
        val rootJson = JSONObject().apply {
            put("version", 2)
            put("appName", "HealthyLu")
            put("type", "reminder_config")
            put("configs", jsonArray)
        }
        
        return rootJson.toString(2)
    }
    
    /**
     * 从JSON字符串导入配置
     */
    fun importConfigFromJson(context: Context, jsonString: String, replaceExisting: Boolean = false): ImportResult {
        return try {
            val rootJson = JSONObject(jsonString)
            val configsArray = rootJson.getJSONArray("configs")
            
            val importedConfigs = mutableListOf<ReminderConfig>()
            for (i in 0 until configsArray.length()) {
                importedConfigs.add(ReminderConfig.fromJson(configsArray.getJSONObject(i)))
            }
            
            if (replaceExisting) {
                saveAllConfigs(context, importedConfigs)
                ImportResult(true, importedConfigs.size, 0)
            } else {
                val existingConfigs = getAllConfigs(context)
                var addedCount = 0
                var updatedCount = 0
                
                importedConfigs.forEach { imported ->
                    val existingIndex = existingConfigs.indexOfFirst { it.id == imported.id }
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
            android.util.Log.e("ReminderConfigManager", "Import failed: ${e.message}")
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