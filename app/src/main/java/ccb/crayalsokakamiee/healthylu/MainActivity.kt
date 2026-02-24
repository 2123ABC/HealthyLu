package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.app.AppOpsManager
import android.os.Process
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.InputStream
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var backgroundManager: BackgroundManager
    private lateinit var mainLayout: ConstraintLayout
    
    // 使用懒加载创建Fragment，避免重复创建
    private val checkInFragment by lazy { CheckInFragment() }
    private val totalFragment by lazy { TotalFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    // 使用Handler管理延迟任务，避免Activity销毁后回调导致的崩溃
    private val handler = Handler(Looper.getMainLooper())
    private val activityRef = java.lang.ref.WeakReference(this)
    private val pendingRunnables = mutableListOf<Runnable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在创建Activity之前应用保存的主题（用try-catch包裹防止崩溃）
        try {
            ThemeManager.applySavedTheme(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error applying theme: ${e.message}", e)
        }
        
        try {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            fragmentContainer = findViewById(R.id.fragmentContainer)
            bottomNavigation = findViewById(R.id.bottomNavigation)
            mainLayout = findViewById(R.id.mainLayout)
            backgroundManager = BackgroundManager(this)

            // 应用背景设置
            try {
                applyBackgroundByArea()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error applying background: ${e.message}", e)
            }
            
            // 延迟启动后台保活服务，避免启动时崩溃
            val startServiceRunnable = object : Runnable {
                override fun run() {
                    activityRef.get()?.startBackgroundService()
                }
            }
            pendingRunnables.add(startServiceRunnable)
            handler.postDelayed(startServiceRunnable, 1000)
            
            // 检查权限
            val hasUsageStats = hasUsageStatsPermission()
            val hasNotificationPermission = hasNotificationPermission()
            if (!hasUsageStats || !hasNotificationPermission) {
                checkAllPermissions()
            }
            
            // 延迟启动每周提醒，避免启动时崩溃
            val scheduleReminderRunnable = object : Runnable {
                override fun run() {
                    activityRef.get()?.scheduleMorningReminder()
                }
            }
            pendingRunnables.add(scheduleReminderRunnable)
            handler.postDelayed(scheduleReminderRunnable, 1500)

            bottomNavigation.setOnItemSelectedListener { item ->
                try {
                    when (item.itemId) {
                        R.id.navigation_checkin -> {
                            replaceFragment(checkInFragment)
                            true
                        }
                        R.id.navigation_total -> {
                            replaceFragment(totalFragment)
                            true
                        }
                        R.id.navigation_settings -> {
                            replaceFragment(settingsFragment)
                            true
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error handling navigation: ${e.message}", e)
                    false
                }
            }

            // 默认显示打卡页面
            if (savedInstanceState == null) {
                try {
                    replaceFragment(checkInFragment)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error setting default fragment: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in onCreate: ${e.message}", e)
            // 尝试设置基本UI以避免白屏
            try {
                setContentView(R.layout.activity_main)
            } catch (ex: Exception) {
                android.util.Log.e("MainActivity", "Critical error setting content view: ${ex.message}", ex)
            }
        }
    }
    
    fun applyBackgroundByArea() {
        try {
            val tilingArea = backgroundManager.getTilingArea()
            val hasBackgroundImage = backgroundManager.getBackgroundImageUri() != null
            
            when (tilingArea) {
                "fullscreen" -> {
                    // 全屏背景
                    applyBackgroundToView(mainLayout)
                    fragmentContainer.background = null
                }
                "content", "above_nav" -> {
                    // 内容区域背景
                    if (!hasBackgroundImage) {
                        // 只有在没有背景图片时才设置默认背景色
                        try {
                            mainLayout.setBackgroundColor(Color.parseColor("#f0f0f0"))
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error setting main layout background: ${e.message}", e)
                        }
                    } else {
                        // 有背景图片时清除背景色
                        mainLayout.background = null
                    }
                    applyBackgroundToView(fragmentContainer)
                }
                else -> {
                    applyBackgroundToView(mainLayout)
                    fragmentContainer.background = null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error applying background by area: ${e.message}", e)
            // 出错时设置默认背景
            try {
                mainLayout.setBackgroundColor(Color.parseColor("#f0f0f0"))
                fragmentContainer.background = null
            } catch (ex: Exception) {
                android.util.Log.e("MainActivity", "Error setting default background: ${ex.message}", ex)
            }
        }
    }
    
    private fun applyBackgroundToView(view: android.view.View) {
        val imageUriString = backgroundManager.getBackgroundImageUri()
        
        android.util.Log.d("MainActivity", "applyBackgroundToView - imageUriString: $imageUriString")
        android.util.Log.d("MainActivity", "applyBackgroundToView - view.width: ${view.width}, view.height: ${view.height}")
        
        if (imageUriString != null && imageUriString.isNotEmpty()) {
            try {
                val imageUri = Uri.parse(imageUriString)
                android.util.Log.d("MainActivity", "applyBackgroundToView - imageUri: $imageUri")
                
                // 使用优化的Bitmap加载方法
                val bitmap = decodeSampledBitmapFromUri(imageUri, view.width, view.height)
                
                android.util.Log.d("MainActivity", "applyBackgroundToView - bitmap: $bitmap")
                
                if (bitmap != null) {
                    val bitmapDrawable = createBitmapDrawableByMode(bitmap)
                    val combinedDrawable = applyOverlayToBackground(bitmapDrawable)
                    view.background = combinedDrawable
                    android.util.Log.d("MainActivity", "applyBackgroundToView - background set successfully")
                    return
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error loading background image: ${e.message}", e)
                try {
                    view.setBackgroundColor(Color.parseColor("#f0f0f0"))
                } catch (ex: Exception) {
                    android.util.Log.e("MainActivity", "Error setting default background: ${ex.message}", ex)
                }
                return
            }
        }
        
        // 如果没有图片或加载失败，使用默认背景
        android.util.Log.d("MainActivity", "applyBackgroundToView - no image, using default background")
        try {
            view.setBackgroundColor(Color.parseColor("#f0f0f0"))
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error setting default background: ${e.message}", e)
        }
    }
    
    /**
     * 优化的Bitmap加载方法，使用采样压缩和RGB_565配置降低内存占用
     */
    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
            
            // 如果view尺寸为0，使用屏幕尺寸
            val displayMetrics = resources.displayMetrics
            val actualReqWidth = if (reqWidth > 0) reqWidth else displayMetrics.widthPixels
            val actualReqHeight = if (reqHeight > 0) reqHeight else displayMetrics.heightPixels
            
            options.inSampleSize = calculateInSampleSize(options, actualReqWidth, actualReqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565  // 降低内存占用
            
            contentResolver.openInputStream(uri)?.use { decodedStream ->
                android.graphics.BitmapFactory.decodeStream(decodedStream, null, options)
            }
        }
    }
    
    /**
     * 计算合适的采样率
     */
    private fun calculateInSampleSize(
        options: android.graphics.BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    private fun createBitmapDrawableByMode(bitmap: Bitmap): BitmapDrawable {
        // 始终使用拉伸模式
        return BitmapDrawable(resources, bitmap)
    }
    
    private fun applyOverlayToBackground(backgroundDrawable: BitmapDrawable): Drawable {
        val overlayColor = backgroundManager.getOverlayColor()
        
        android.util.Log.d("MainActivity", "applyOverlayToBackground - color: $overlayColor, alpha: ${Color.alpha(overlayColor)}")
        
        return if (overlayColor != -1) {
            // 直接使用颜色的 alpha 值，不再使用单独的 overlay_opacity
            val overlayDrawable = ColorDrawable(overlayColor)
            TintedBitmapDrawable(backgroundDrawable, overlayDrawable)
        } else {
            android.util.Log.d("MainActivity", "No overlay color, returning background only")
            backgroundDrawable
        }
    }

    private fun startBackgroundService() {
        try {
            android.util.Log.d("MainActivity", "Attempting to start background service")
            
            val serviceIntent = Intent(this, WaterReminderService::class.java)
            val result = startService(serviceIntent)
            android.util.Log.d("MainActivity", "Service start result: $result")
            if (result == null) {
                android.util.Log.e("MainActivity", "Service start failed - result is null")
            } else {
                android.util.Log.d("MainActivity", "Service started successfully")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error starting background service: ${e.message}", e)
        }
    }
    
    private fun checkAllPermissions() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val hasShownPermissionDialog = prefs.getBoolean("has_shown_permission_dialog", false)
        
        // 如果已经显示过权限提示，就不再重复显示
        if (hasShownPermissionDialog) {
            return
        }
        
        val hasUsageStats = hasUsageStatsPermission()
        val hasNotificationPermission = hasNotificationPermission()
        
        if (!hasUsageStats || !hasNotificationPermission) {
            val message = buildString {
                    append("第一次安装应用请去设置里检查权限以获得最佳体验awa")
            }
            
            AlertDialog.Builder(this)
                .setTitle("需要权限")
                .setMessage(message)
                .setPositiveButton("我知道了") { _, _ ->
                    // 记录已经显示过权限提示
                    prefs.edit().putBoolean("has_shown_permission_dialog", true).apply()
                }
                .show()
        } else {
            // 如果权限都已开启，也记录一下，避免不必要的检查
            prefs.edit().putBoolean("has_shown_permission_dialog", true).apply()
        }
    }
    
    private fun checkUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            // 引导用户去设置中开启权限
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    private fun scheduleMorningReminder() {
        try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // 设置为每周一
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                
                // 如果时间已经过了，设置为下周一
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            
            val intent = Intent(this, WaterReminderReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.setRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                android.app.AlarmManager.INTERVAL_DAY * 7, // 每周提醒
                pendingIntent
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error scheduling morning reminder: ${e.message}", e)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        // 检查当前显示的Fragment是否与要替换的Fragment相同
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment?.javaClass == fragment.javaClass) {
            // Fragment已经显示，不需要替换
            return
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    

        override fun onDestroy() {

            super.onDestroy()

            // 清理所有Handler回调，避免Activity销毁后回调导致的崩溃

            pendingRunnables.forEach { handler.removeCallbacks(it) }

            pendingRunnables.clear()

            android.util.Log.d("MainActivity", "MainActivity onDestroy - cleaned up handler callbacks")

        }

    

    }
