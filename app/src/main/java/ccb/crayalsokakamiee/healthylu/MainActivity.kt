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
    
    // 记录期望的页面位置，用于检测并纠正状态恢复导致的错误跳转
    private var expectedPagePosition = 0
    // 标记是否正在纠正页面位置
    private var isCorrectingPosition = false

    // 使用Handler管理延迟任务，避免Activity销毁后回调导致的崩溃
    private val handler = Handler(Looper.getMainLooper())
    private val activityRef = java.lang.ref.WeakReference(this)
    private val pendingRunnables = mutableListOf<Runnable>()
    
    companion object {
        private const val PREFS_NAME = "main_activity_state"
        private const val PREF_KEY_PAGE = "saved_page"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在创建Activity之前应用保存的主题
        try {
            ThemeManager.applySavedTheme(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error applying theme: ${e.message}", e)
        }
        
        try {
            // 传入 null 阻止 Fragment 自动恢复，避免主题切换时页面跳转
            super.onCreate(null)
            setContentView(R.layout.activity_main)

            viewPager = findViewById(R.id.viewPager)
            bottomNavigation = findViewById(R.id.bottomNavigation)
            
            // 先读取保存的页面位置
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedPage = prefs.getInt(PREF_KEY_PAGE, 0)
            expectedPagePosition = savedPage
            
            // 禁用ViewPager2的自动状态恢复（必须在设置adapter之前）
            viewPager.isSaveEnabled = false
            
            // 设置ViewPager2适配器
            viewPager.adapter = ViewPagerAdapter(this)
            
            // 预加载相邻页面
            viewPager.offscreenPageLimit = 1
            
            // 立即同步底部导航栏（必须在setCurrentItem之前设置，避免回调冲突）
            val expectedNavId = when (savedPage) {
                0 -> R.id.navigation_checkin
                1 -> R.id.navigation_total
                2 -> R.id.navigation_settings
                else -> R.id.navigation_checkin
            }
            bottomNavigation.selectedItemId = expectedNavId
            
            // 恢复页面位置
            viewPager.setCurrentItem(savedPage, false)
            
            // 禁用RecyclerView的ItemAnimator，避免干扰PageTransformer
            (viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)?.itemAnimator = null
            
            // 设置淡入淡出页面切换动画
            viewPager.setPageTransformer(FadePageTransformer())
            
            // ViewPager2页面切换监听，同步底部导航栏
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    // 如果正在纠正位置，跳过处理
                    if (isCorrectingPosition) {
                        return
                    }
                    
                    // 如果position和期望不一致，说明是状态恢复导致的错误跳转，需要纠正
                    if (position != expectedPagePosition) {
                        isCorrectingPosition = true
                        viewPager.post {
                            viewPager.setCurrentItem(expectedPagePosition, false)
                            // 纠正后同步底栏
                            val correctNavId = when (expectedPagePosition) {
                                0 -> R.id.navigation_checkin
                                1 -> R.id.navigation_total
                                2 -> R.id.navigation_settings
                                else -> R.id.navigation_checkin
                            }
                            bottomNavigation.selectedItemId = correctNavId
                            // 延迟重置标记，确保纠正完成
                            viewPager.postDelayed({
                                isCorrectingPosition = false
                            }, 100)
                        }
                        return
                    }
                    
                    val navId = when (position) {
                        0 -> R.id.navigation_checkin
                        1 -> R.id.navigation_total
                        2 -> R.id.navigation_settings
                        else -> R.id.navigation_checkin
                    }
                    bottomNavigation.selectedItemId = navId
                    // 更新期望位置
                    expectedPagePosition = position
                    // 保存当前页面位置
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(PREF_KEY_PAGE, position)
                        .apply()
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
                expectedPagePosition = position
                viewPager.setCurrentItem(position, true)
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "onCreate error: ${e.message}", e)
            try {
                setContentView(R.layout.activity_main)
            } catch (ex: Exception) {
                android.util.Log.e("MainActivity", "setContentView error: ${ex.message}", ex)
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
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "后台服务启动失败: ${e.message}", e)
        }
    }
    
    private fun checkAllPermissions() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val hasShownPermissionDialog = prefs.getBoolean("has_shown_permission_dialog", false)
        
        if (hasShownPermissionDialog) {
            return
        }
        
        val hasUsageStats = hasUsageStatsPermission()
        val hasNotificationPermission = hasNotificationPermission()
        
        if (!hasUsageStats || !hasNotificationPermission) {
            AlertDialog.Builder(this)
                .setTitle("需要权限")
                .setMessage("第一次安装应用请去设置里检查权限以获得最佳体验awa")
                .setPositiveButton("我知道了") { _, _ ->
                    prefs.edit().putBoolean("has_shown_permission_dialog", true).apply()
                }
                .show()
        } else {
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
        pendingRunnables.forEach { handler.removeCallbacks(it) }
        pendingRunnables.clear()
    }
    
    override fun onResume() {
        super.onResume()
        syncBottomNavigationState()
        viewPager.post {
            try {
                val recyclerView = viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView
                recyclerView?.let { rv ->
                    rv.scrollBy(1, 0)
                    rv.scrollBy(-1, 0)
                }
            } catch (e: Exception) {
                // 忽略
            }
        }
    }
    
    private fun syncBottomNavigationState() {
        if (::viewPager.isInitialized && ::bottomNavigation.isInitialized) {
            val expectedItemId = when (expectedPagePosition) {
                0 -> R.id.navigation_checkin
                1 -> R.id.navigation_total
                2 -> R.id.navigation_settings
                else -> R.id.navigation_checkin
            }
            if (bottomNavigation.selectedItemId != expectedItemId) {
                bottomNavigation.selectedItemId = expectedItemId
            }
            if (viewPager.currentItem != expectedPagePosition) {
                viewPager.setCurrentItem(expectedPagePosition, false)
            }
        }
    }
}
