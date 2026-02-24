package ccb.crayalsokakamiee.healthylu

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

class WaterReminderReceiver : BroadcastReceiver() {
    
    companion object {
        private const val CHANNEL_ID = "water_reminder_channel"
        private const val NOTIFICATION_ID = 1001
        
        // 定时提醒随机文案
        private val reminderMessages = listOf(
            "这周还没有鹿管哦，记得鹿管保持性福！",
            "？快去鹿管吧~",
            "Luguanluguanlulushijiandaole",
            "美好一周从鹿管开始，这周你鹿管了吗？",
            "你在呼唤鹿管~",
            "鹿管提醒：该去🦌一发啦！",
            "保持鹿管，精力充沛！",
            "一个小时过去了，两个小时过去了，三个小时过去了...\n你这周还没有鹿过管哦",
            "我说三顾茅庐来四次有没有懂的？"
        )
        
        fun scheduleReminder(context: Context, intervalMillis: Long) {
            val intent = Intent(context, WaterReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis,
                pendingIntent
            )
        }
        
        fun scheduleHourlyReminder(context: Context) {
            val intent = Intent(context, WaterReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 60000, // 1分钟后开始
                android.app.AlarmManager.INTERVAL_HOUR, // 每小时提醒
                pendingIntent
            )
        }
        
        fun cancelReminder(context: Context) {
            val intent = Intent(context, WaterReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 检查是否有通知权限
        if (!hasNotificationPermission(context)) {
            android.util.Log.d("WaterReminderReceiver", "No notification permission granted, skipping notification")
            return
        }
        
        val waterRecordManager = WaterRecordManager(context)
        
        // 如果这周还没有喝过水，发送通知并安排下一次提醒
        if (waterRecordManager.getWeekCount() == 0) {
            showNotification(context)
            
            // 安排下一次提醒（1小时后）
            scheduleHourlyReminder(context)
        }
    }

    /**
     * 检查是否有通知权限
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    private fun showNotification(context: Context) {
        createNotificationChannel(context)
        
        // 随机选择一条文案
        val message = reminderMessages[Random.nextInt(reminderMessages.size)]
        
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_info_details)
            .setContentTitle("🦌管提醒")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "🦌管提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "每周提醒你鹿管"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}