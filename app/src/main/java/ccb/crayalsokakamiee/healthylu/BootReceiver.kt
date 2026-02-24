package ccb.crayalsokakamiee.healthylu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * 开机自启动接收器
 * 接收系统开机广播后启动后台服务
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking settings...")
            
            // 检查是否启用开机启动
            if (!AppSettingsManager.isBootStartupEnabled(context)) {
                Log.d(TAG, "Boot startup is disabled, skipping service start")
                return
            }
            
            // 检查是否启用后台驻留
            if (!AppSettingsManager.isBackgroundServiceEnabled(context)) {
                Log.d(TAG, "Background service is disabled, skipping service start")
                return
            }
            
            try {
                // 启动后台服务
                val serviceIntent = Intent(context, WaterReminderService::class.java)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ 需要使用 startForegroundService
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                Log.d(TAG, "Background service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting background service: ${e.message}", e)
            }
        }
    }
}
