package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 全局异常处理器 - 捕获应用未处理的异常并记录日志
 */
class CrashHandler private constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    companion object {
        private var instance: CrashHandler? = null

        fun init(context: Context) {
            if (instance == null) {
                instance = CrashHandler(context.applicationContext)
                Thread.setDefaultUncaughtExceptionHandler(instance)
                LogManager.i("CrashHandler", "CrashHandler initialized")
            }
        }
    }

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        LogManager.e("CRASH", "Uncaught exception in thread: ${thread.name}", throwable)

        // 记录崩溃日志到文件
        LogManager.logCrash(throwable)

        // 获取设备信息
        val deviceInfo = getDeviceInfo()

        // 记录设备信息
        LogManager.e("CRASH", "Device Info: $deviceInfo")

        // 调用默认处理器（显示崩溃对话框）
        defaultHandler?.uncaughtException(thread, throwable) ?: run {
            // 如果没有默认处理器，杀死进程
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }

    /**
     * 获取设备信息
     */
    private fun getDeviceInfo(): String {
        return """
            |
            |Device Info:
            |Manufacturer: ${android.os.Build.MANUFACTURER}
            |Model: ${android.os.Build.MODEL}
            |OS Version: ${android.os.Build.VERSION.RELEASE}
            |SDK Version: ${android.os.Build.VERSION.SDK_INT}
            |App Version: ${getAppVersionName()}
        """.trimMargin()
    }

    /**
     * 获取应用版本号
     */
    private fun getAppVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * 获取异常堆栈信息
     */
    fun getStackTraceString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}