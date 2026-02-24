package ccb.crayalsokakamiee.healthylu

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DrinkWaterReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DRINK_WATER = "ccb.crayalsokakamiee.healthylu.ACTION_DRINK_WATER"
        const val NOTIFICATION_ID_KEY = "notification_id"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DRINK_WATER) {
            // 增加鹿管次数
            val waterRecordManager = WaterRecordManager(context)
            waterRecordManager.addWaterRecord()
            
            // 关闭通知
            val notificationId = intent.getIntExtra(NOTIFICATION_ID_KEY, 0)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
        }
    }
}
