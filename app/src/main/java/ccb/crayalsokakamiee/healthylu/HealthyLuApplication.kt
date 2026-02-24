package ccb.crayalsokakamiee.healthylu

import android.app.Application

/**
 * HealthyLu应用程序入口
 */
class HealthyLuApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化日志管理器
        LogManager.init(this)

        // 初始化崩溃处理器
        CrashHandler.init(this)

        LogManager.i("HealthyLuApplication", "Application created successfully")
    }
}