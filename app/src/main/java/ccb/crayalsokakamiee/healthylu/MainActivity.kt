package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.content.Intent
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
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView

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

            viewPager = findViewById(R.id.viewPager)
            bottomNavigation = findViewById(R.id.bottomNavigation)
            
            // 设置ViewPager2适配器
            viewPager.adapter = ViewPagerAdapter(this)
            
            // 预加载相邻页面
            viewPager.offscreenPageLimit = 1
            
            // 禁用RecyclerView的ItemAnimator，避免干扰PageTransformer
            (viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.itemAnimator = null
            
            // 设置淡入淡出页面切换动画
            viewPager.setPageTransformer(FadePageTransformer())
            
            // ViewPager2页面切换监听，同步底部导航栏
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    bottomNavigation.selectedItemId = when (position) {
                        0 -> R.id.navigation_checkin
                        1 -> R.id.navigation_total
                        2 -> R.id.navigation_settings
                        else -> R.id.navigation_checkin
                    }
                }
                
                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                    // 确保滚动时PageTransformer被正确触发
                    // 这个回调在首次切换时会确保动画生效
                }
            })
            
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

            // 底部导航栏点击监听，同步ViewPager2
            bottomNavigation.setOnItemSelectedListener { item ->
                val position = when (item.itemId) {
                    R.id.navigation_checkin -> 0
                    R.id.navigation_total -> 1
                    R.id.navigation_settings -> 2
                    else -> 0
                }
                viewPager.setCurrentItem(position, true)
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "onCreate里的陈争风说启动有问题，理由是: ${e.message}", e)
            // 尝试设置基本UI以避免白屏
            try {
                setContentView(R.layout.activity_main)
            } catch (ex: Exception) {
                android.util.Log.e("MainActivity", "设置content view的潘相舜让程序的错误代码滚出来了: ${ex.message}", ex)
            }
        }
    }
    
    // ViewPager2适配器
    private class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> CheckInFragment()
                1 -> TotalFragment()
                else -> SettingsFragment()
            }
        }
    }

    private fun startBackgroundService() {
        try {
            android.util.Log.d("MainActivity", "尝试启动后台服务")
            
            val serviceIntent = Intent(this, WaterReminderService::class.java)
            val result = startService(serviceIntent)
            android.util.Log.d("MainActivity", "服务启动返回结果: $result")
            if (result == null) {
                android.util.Log.e("MainActivity", "服务启动失败，返回了null")
            } else {
                android.util.Log.d("MainActivity", "服务启动成功！")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "后台服务启动再起不能: ${e.message}", e)
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

    override fun onDestroy() {
        super.onDestroy()

        // 清理所有Handler回调，避免Activity销毁后回调导致的崩溃
        pendingRunnables.forEach { handler.removeCallbacks(it) }
        pendingRunnables.clear()
        android.util.Log.d("MainActivity", "MainActivity onDestroy - 清除了handler callbacks")
    }
    
    override fun onResume() {
        super.onResume()
        // 通过RecyclerView的scrollTo触发PageTransformer初始化
        viewPager.post {
            try {
                val recyclerView = viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
                recyclerView?.let { rv ->
                    // 保存当前位置
                    val currentPos = viewPager.currentItem
                    // 极短暂的滚动来触发变换，然后立即恢复
                    rv.scrollBy(1, 0)
                    rv.scrollBy(-1, 0)
                }
            } catch (e: Exception) {
                // 忽略
            }
        }
    }
}