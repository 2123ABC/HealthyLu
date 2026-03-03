package ccb.crayalsokakamiee.healthylu

import org.json.JSONObject

/**
 * 第三方应用配置数据类
 * @param packageName 应用包名
 * @param displayName 显示名称/备注
 * @param customInfos 自定义信息列表
 */
data class ThirdPartyAppConfig(
    val packageName: String,
    val displayName: String,
    val customInfos: MutableList<String>
) {
    /**
     * 转换为JSON对象
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("packageName", packageName)
            put("displayName", displayName)
            put("customInfos", org.json.JSONArray(customInfos))
        }
    }

    companion object {
        /**
         * 从JSON对象解析
         */
        fun fromJson(json: JSONObject): ThirdPartyAppConfig {
            val packageName = json.getString("packageName")
            val displayName = json.getString("displayName")
            val customInfosArray = json.getJSONArray("customInfos")
            val customInfos = mutableListOf<String>()
            for (i in 0 until customInfosArray.length()) {
                customInfos.add(customInfosArray.getString(i))
            }
            return ThirdPartyAppConfig(packageName, displayName, customInfos)
        }
    }
}
