package ccb.crayalsokakamiee.healthylu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import androidx.core.app.NotificationCompat
import kotlin.random.Random

class WaterReminderService : Service() {
    companion object {
        const val CHANNEL_ID = "water_reminder_service"
        const val NOTIFICATION_ID = 1002
        const val READING_REMINDER_CHANNEL_ID = "reading_reminder"
        const val READING_REMINDER_NOTIFICATION_ID = 1003
        
        // 需要检测的应用包名列表
        val TARGET_PACKAGES = listOf(
            "com.xjs.ehviewer",      // EHViewer
            "com.perol.pixez", //PixEz
            "com.JMComic3.app", //JMComic
            "com.picacomic.fregata", //哔咔
            "sg.jxrgq.wbbzrf", //91
            "jp.pxv.android" //Pixiv
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval: Long = 30000 // 30秒检查一次
    
    // 提醒冷却机制
    private var lastReminderTime: Long = 0
    private var lastReminderPackage: String? = null
    private val reminderCooldown: Long = 5 * 60 * 1000 // 5分钟冷却时间
    
    // 使用WeakReference避免内存泄漏
    private val serviceRef = java.lang.ref.WeakReference(this)
    
    // 各应用的专属提醒文案（每个应用3条）
    private val appReminderMessages = mapOf(
        "com.xjs.ehviewer" to listOf(
            "EHViewer启动！",
            "今天来的大家想看的东西啊",
            "无码同人志！爽！"
        ),
        "com.perol.pixez" to listOf(
            "P站启动！",
            "不用翻墙就是爽！",
            "来点色图"
        ),
        "com.JMComic3.app" to listOf(
            "禁漫！爽！",
            "记得去签到",
            "向传奇禁漫汉化组设敬！"
        ),
        "sg.jxrgq.wbbzrf" to listOf(
            "7891",
            "第91号隐私协议启动",
            "警惕91破解版！虽然你可能听不进去但还是警告一下"
        ),
        "jp.pxv.android" to listOf(
            "Pixiv——日本最大的插画交流网站",
            "如果你只是来看二次元图片的话请把这个消息划掉",
            "抱着普通看图的心情的话请无视这条消息"
        )
    )
    
    // 后台服务通知随机文案
    private val serviceStatusMessages = listOf(
        "后台服务正在运行，保持鹿管提醒",
        "守护你的性福，时刻准备提醒",
        "鹿鹿小助手待命中~",
        "服务运行中，为你的健康鹿管保驾护航",
        "后台运行中，做你的鹿管小管家",
        "时刻关注你的鹿管习惯",
        "鹿管助手在线，请勿忘记鹿管"
    )
    
    // 鹿管超过次数的额外提醒文案
    private val overLimitReminderMessages = listOf(
        "这周已经鹿了太多次了，想想看鹿太多的后果！",
        "鹿得更多不会让你精力满满！",
        "你要守住底线啊！！！",
        "前列腺炎、精囊炎、尿道炎......",
        "STOOOOOP!"
    )
    
    // 使用WeakReference避免内存泄漏
    private val checkRunnable = object : Runnable {
        override fun run() {
            val service = serviceRef.get()
            if (service != null) {
                service.checkForegroundApp()
                handler.postDelayed(this, checkInterval)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("WaterReminderService", "Service onCreate called")
        createNotificationChannel()
        createReadingReminderChannel()
        
        // 启动定时提醒
        schedulePeriodicReminder()
    }
    
    private fun schedulePeriodicReminder() {
        val waterRecordManager = WaterRecordManager(this)
        // 如果本周打卡次数小于2，启动定时提醒
        if (waterRecordManager.getWeekCount() < 2) {
            WaterReminderReceiver.scheduleHourlyReminder(this)
            android.util.Log.d("WaterReminderService", "Periodic reminder scheduled")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("WaterReminderService", "Service onStartCommand called")
        
        // 检查是否启用后台驻留
        if (!AppSettingsManager.isBackgroundServiceEnabled(this)) {
            android.util.Log.d("WaterReminderService", "Background service is disabled, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }
        
        // 确保通知通道存在（防止服务重启时通道不存在）
        createNotificationChannel()
        createReadingReminderChannel()
        
        val notification = createNotification()
        android.util.Log.d("WaterReminderService", "Creating foreground notification with ID: $NOTIFICATION_ID")
        startForeground(NOTIFICATION_ID, notification)
        android.util.Log.d("WaterReminderService", "Foreground service started successfully")
        
        // 开始检测前台应用
        handler.post(checkRunnable)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "鹿管提醒服务",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "保持鹿管提醒服务在后台运行"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d("WaterReminderService", "Notification channel created")
        }
    }
    
    private fun createReadingReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                READING_REMINDER_CHANNEL_ID,
                "鹿管提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "提醒鹿管打卡"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            android.app.PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        // 随机选择一条后台服务文案
        val message = serviceStatusMessages[Random.nextInt(serviceStatusMessages.size)]
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("鹿管提醒运行中")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun checkForegroundApp() {
        // 确保后台保活通知始终显示
        updateForegroundNotification()
        
        // 检查是否启用应用提醒
        if (!AppSettingsManager.isAppReminderEnabled(this)) {
            android.util.Log.d("WaterReminderService", "App reminder is disabled, skipping check")
            return
        }
        
        // 先检查是否有 UsageStats 权限
        if (!hasUsageStatsPermission()) {
            android.util.Log.d("WaterReminderService", "No UsageStats permission granted")
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val startTime = endTime - 10000 // 最近10秒
                
                val usageStatsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                )
                
                if (usageStatsList != null && usageStatsList.isNotEmpty()) {
                    // 按最后使用时间排序，获取最近使用的应用
                    val sortedStats = usageStatsList.sortedByDescending { it.lastTimeUsed }
                    val foregroundApp = sortedStats.firstOrNull()?.packageName
                    
                    if (foregroundApp != null && foregroundApp in TARGET_PACKAGES) {
                        // 检查是否需要显示提醒（冷却机制）
                        val currentTime = System.currentTimeMillis()
                        val shouldShowReminder = when {
                            // 如果是不同的应用，显示提醒
                            lastReminderPackage != foregroundApp -> true
                            // 如果是相同应用但冷却时间已过，显示提醒
                            currentTime - lastReminderTime >= reminderCooldown -> true
                            // 否则不显示
                            else -> false
                        }
                        
                        if (shouldShowReminder) {
                            // 检查今天是否已经鹿管过
                            val waterRecordManager = WaterRecordManager(this)
                            val todayCount = waterRecordManager.getTodayCount()
                            
                            // 根据鹿管次数显示不同的提醒
                            if (todayCount == 0) {
                                // 没鹿管时显示应用提醒
                                showReadingReminder(foregroundApp)
                                lastReminderTime = currentTime
                                lastReminderPackage = foregroundApp
                            } else if (todayCount > 2) {
                                // 鹿管超过2次时显示额外提醒
                                showOverLimitReminder()
                                lastReminderTime = currentTime
                                lastReminderPackage = foregroundApp
                            }
                        }
                    } else {
                        // 如果当前应用不是目标应用，重置状态
                        // 这样下次进入目标应用时会重新显示提醒
                        lastReminderPackage = null
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("WaterReminderService", "查找前台应用的严望说不行，理由是: ${e.message}", e)
            }
        }
    }
    
    /**
     * 检查是否有 UsageStats 权限
     */
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    
    private fun updateForegroundNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showReadingReminder(packageName: String) {
        // 根据应用包名获取对应的提醒文案
        val messages = appReminderMessages[packageName] ?: listOf("鹿管打卡吧！")
        // 随机选择一条
        val message = messages[Random.nextInt(messages.size)]
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.PendingIntent.getActivity(
                this,
                1,
                notificationIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            android.app.PendingIntent.getActivity(
                this,
                1,
                notificationIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        // 鹿管按钮的 Intent
        val drinkIntent = Intent(this, DrinkWaterReceiver::class.java).apply {
            action = DrinkWaterReceiver.ACTION_DRINK_WATER
            putExtra(DrinkWaterReceiver.NOTIFICATION_ID_KEY, READING_REMINDER_NOTIFICATION_ID)
        }
        val drinkPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.PendingIntent.getBroadcast(
                this,
                2,
                drinkIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            android.app.PendingIntent.getBroadcast(
                this,
                2,
                drinkIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        val notification = NotificationCompat.Builder(this, READING_REMINDER_CHANNEL_ID)
            .setContentTitle("鹿管提醒")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_add, "鹿管次数+1", drinkPendingIntent)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(READING_REMINDER_NOTIFICATION_ID, notification)
    }
    
    private fun showOverLimitReminder() {
        // 随机选择一条额外提醒文案
        val message = overLimitReminderMessages[Random.nextInt(overLimitReminderMessages.size)]
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.PendingIntent.getActivity(
                this,
                3,
                notificationIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            android.app.PendingIntent.getActivity(
                this,
                3,
                notificationIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        val notification = NotificationCompat.Builder(this, READING_REMINDER_CHANNEL_ID)
            .setContentTitle("不要再鹿了！")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(READING_REMINDER_NOTIFICATION_ID, notification)
    }
}