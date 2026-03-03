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
        
        // 默认文案（当用户没有配置任何启用的文案时使用）
        private val defaultMessages = listOf(
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
            val triggerTime = System.currentTimeMillis() + intervalMillis
            
            // 检查是否有精确闹钟权限
            val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canScheduleExactAlarms) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // 没有精确闹钟权限，使用不精确的闹钟
                    alarmManager.setAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            
            android.util.Log.d("WaterReminderReceiver", "Scheduled reminder in ${intervalMillis / 1000} seconds, exactAlarms=$canScheduleExactAlarms")
        }
        
        fun scheduleHourlyReminder(context: Context) {
            val intervalMinutes = AppSettingsManager.getReminderIntervalMinutes(context)
            val intervalMillis = (intervalMinutes * 60 * 1000).toLong()
            scheduleReminder(context, intervalMillis)
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
        
        /**
         * 获取提醒文案列表
         * 优先使用用户自定义的启用的文案，如果没有则使用默认文案
         */
        fun getReminderMessages(context: Context): List<String> {
            val customMessages = ReminderConfigManager.getEnabledMessages(context)
            return if (customMessages.isNotEmpty()) {
                customMessages
            } else {
                defaultMessages
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("WaterReminderReceiver", "onReceive called")
        
        val waterRecordManager = WaterRecordManager(context)
        val weekCount = waterRecordManager.getWeekCount()
        
        // 如果本周打卡次数小于1，发送通知并调度下一次提醒
        if (weekCount < 1) {
            // 检查是否有通知权限
            if (hasNotificationPermission(context)) {
                android.util.Log.d("WaterReminderReceiver", "Week count is $weekCount, showing notification")
                showNotification(context)
            } else {
                android.util.Log.d("WaterReminderReceiver", "No notification permission granted, skipping notification")
            }
            
            // 调度下一次提醒
            scheduleHourlyReminder(context)
        } else {
            android.util.Log.d("WaterReminderReceiver", "Week count is $weekCount, no more reminders needed")
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(context: Context) {
        createNotificationChannel(context)
        
        // 从用户配置或默认文案中随机选择一条
        val messages = getReminderMessages(context)
        val message = messages[Random.nextInt(messages.size)]
        
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
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
