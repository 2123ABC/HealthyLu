package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WaterRecordManager(context: Context) {
    private val prefs: SharedPreferences
    
    init {
        // 创建MasterKey用于加密
        prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            // 创建加密的SharedPreferences
            EncryptedSharedPreferences.create(
                context,
                "water_records",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // 如果加密失败，回退到普通SharedPreferences
            android.util.Log.e("WaterRecordManager", "负责加密的杨强校长说不行，理由是: ${e.message}", e)
            context.getSharedPreferences("water_records", Context.MODE_PRIVATE)
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val KEY_DAILY_COUNT = "daily_count_"
        private const val KEY_DAILY_TIMESTAMP = "daily_timestamp_"
    }

    fun getTodayDate(): String {
        return dateFormat.format(Date())
    }

    fun getTodayCount(): Int {
        val today = getTodayDate()
        val timestamp = prefs.getLong(KEY_DAILY_TIMESTAMP + today, 0)
        
        // 如果是新的一天，重置计数
        if (timestamp == 0L) {
            return 0
        }
        
        return prefs.getInt(KEY_DAILY_COUNT + today, 0)
    }

    fun getWeekCount(): Int {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToMonday = if (currentDay == Calendar.SUNDAY) 6 else currentDay - Calendar.MONDAY
        
        // 获取本周一
        calendar.add(Calendar.DAY_OF_MONTH, -daysToMonday)
        val mondayDate = dateFormat.format(calendar.time)
        
        // 计算本周的鹿管次数
        var weekCount = 0
        for (i in 0..daysToMonday) {
            // 使用 clone() 创建 Calendar 的独立副本，避免修改原始对象
            val dayCalendar = calendar.clone() as Calendar
            dayCalendar.add(Calendar.DAY_OF_MONTH, i)
            val dateStr = dateFormat.format(dayCalendar.time)
            weekCount += prefs.getInt(KEY_DAILY_COUNT + dateStr, 0)
        }
        
        return weekCount
    }

    fun addWaterRecord() {
        val today = getTodayDate()
        val currentCount = getTodayCount()
        prefs.edit()
            .putInt(KEY_DAILY_COUNT + today, currentCount + 1)
            .putLong(KEY_DAILY_TIMESTAMP + today, System.currentTimeMillis())
            .apply()
    }

    fun undoWaterRecord() {
        val today = getTodayDate()
        val currentCount = getTodayCount()
        if (currentCount > 0) {
            prefs.edit()
                .putInt(KEY_DAILY_COUNT + today, currentCount - 1)
                .apply()
        }
    }

    fun getTodayVolume(): Int {
        val count = getTodayCount()
        return count * 4 // 每次4mL
    }

    fun formatVolume(mL: Int): String {
        return if (mL >= 1000) {
            "${mL / 1000}.${(mL % 1000) / 100}L"
        } else {
            "${mL}mL"
        }
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}