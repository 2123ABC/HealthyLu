package ccb.crayalsokakamiee.healthylu

import org.json.JSONObject

/**
 * 提醒文案配置数据类
 * @param id 唯一标识
 * @param message 提醒消息文案
 * @param enabled 是否启用
 */
data class ReminderConfig(
    val id: String,
    val message: String,
    val enabled: Boolean = true
) {
    /**
     * 转换为JSON对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("message", message)
            put("enabled", enabled)
        }
    }

    companion object {
        /**
         * 从JSON对象解析
         */
        fun fromJson(json: JSONObject): ReminderConfig {
            val id = json.getString("id")
            val message = json.getString("message")
            val enabled = json.optBoolean("enabled", true)
            return ReminderConfig(id, message, enabled)
        }
        
        /**
         * 生成唯一ID
         */
        fun generateId(): String {
            return System.currentTimeMillis().toString()
        }
    }
}