package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/**
 * 日志管理类 - 管理应用的日志记录和文件写入
 * 日志文件保存在Download/HealthyLu目录下
 */
object LogManager {

    private const val LOG_DIR_NAME = "HealthyLu"
    private const val LOG_FILE_NAME = "app_log.txt"
    private const val CRASH_FILE_NAME = "crash_log.txt"
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB

    private var logFile: File? = null
    private var crashLogFile: File? = null
    private var isInitialized = false

    // 使用队列和线程池异步写入日志，避免阻塞主线程
    private val logQueue = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadExecutor()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /**
     * 初始化日志管理器
     */
    fun init(context: Context) {
        if (isInitialized) return

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val logDir = File(downloadsDir, LOG_DIR_NAME)

            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            logFile = File(logDir, LOG_FILE_NAME)
            crashLogFile = File(logDir, CRASH_FILE_NAME)

            // 检查日志文件大小，如果超过限制则备份
            checkLogFileSize()

            // 启动日志写入线程
            startLogWriter()

            // 记录应用启动
            i("LogManager", "=== Application Started ===")
            i("LogManager", "Log directory: ${logDir.absolutePath}")
            i("LogManager", "Log file: ${logFile?.absolutePath}")
            i("LogManager", "Crash log file: ${crashLogFile?.absolutePath}")

            isInitialized = true
        } catch (e: Exception) {
            Log.e("LogManager", "Failed to initialize LogManager: ${e.message}", e)
        }
    }

    /**
     * 检查日志文件大小，超过限制则备份
     */
    private fun checkLogFileSize() {
        try {
            logFile?.let { file ->
                if (file.exists() && file.length() > MAX_LOG_SIZE) {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val backupFile = File(file.parent, "${file.nameWithoutExtension}_$timestamp.txt")
                    file.renameTo(backupFile)
                    i("LogManager", "Log file backed up to: ${backupFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Error checking log file size: ${e.message}", e)
        }
    }

    /**
     * 启动日志写入线程
     */
    private fun startLogWriter() {
        executor.submit {
            while (true) {
                try {
                    val logMessage = logQueue.poll()
                    if (logMessage != null) {
                        writeToFile(logFile, logMessage)
                    } else {
                        Thread.sleep(100) // 队列为空时休眠100ms
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e("LogManager", "Error in log writer: ${e.message}", e)
                }
            }
        }
    }

    /**
     * 写入日志到文件
     */
    private fun writeToFile(file: File?, message: String) {
        if (file == null) return

        try {
            FileWriter(file, true).use { writer ->
                writer.append(message)
                writer.append("\n")
                writer.flush()
            }
        } catch (e: Exception) {
            Log.e("LogManager", "Error writing to log file: ${e.message}", e)
        }
    }

    /**
     * 记录DEBUG级别日志
     */
    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("D", tag, message)
    }

    /**
     * 记录INFO级别日志
     */
    fun i(tag: String, message: String) {
        Log.i(tag, message)
        addLog("I", tag, message)
    }

    /**
     * 记录WARNING级别日志
     */
    fun w(tag: String, message: String) {
        Log.w(tag, message)
        addLog("W", tag, message)
    }

    /**
     * 记录ERROR级别日志
     */
    fun e(tag: String, message: String) {
        Log.e(tag, message)
        addLog("E", tag, message)
    }

    /**
     * 记录ERROR级别日志（带异常）
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        val stackTrace = android.util.Log.getStackTraceString(throwable)
        addLog("E", tag, "$message\n$stackTrace")
    }

    /**
     * 添加日志到队列
     */
    private fun addLog(level: String, tag: String, message: String) {
        if (!isInitialized) return

        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] [$level] [$tag] $message"
        logQueue.offer(logMessage)
    }

    /**
     * 记录崩溃日志
     */
    fun logCrash(throwable: Throwable) {
        val timestamp = dateFormat.format(Date())
        val crashMessage = """
            |
            |========================================
            |CRASH REPORT - $timestamp
            |========================================
            |${android.util.Log.getStackTraceString(throwable)}
            |========================================
            |
        """.trimMargin()

        try {
            crashLogFile?.let { file ->
                FileWriter(file, true).use { writer ->
                    writer.append(crashMessage)
                    writer.append("\n")
                    writer.flush()
                }
            }

            // 同时写入普通日志
            e("CRASH", "Application crashed!", throwable)
        } catch (e: Exception) {
            Log.e("LogManager", "Error writing crash log: ${e.message}", e)
        }
    }

    /**
     * 清空所有日志文件
     */
    fun clearLogs() {
        try {
            logFile?.delete()
            crashLogFile?.delete()
            i("LogManager", "All logs cleared")
        } catch (e: Exception) {
            Log.e("LogManager", "Error clearing logs: ${e.message}", e)
        }
    }

    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? = logFile?.absolutePath

    /**
     * 获取崩溃日志文件路径
     */
    fun getCrashLogFilePath(): String? = crashLogFile?.absolutePath

    /**
     * 获取日志总大小（字节）
     */
    fun getTotalLogSize(): Long {
        var totalSize = 0L
        logFile?.let { if (it.exists()) totalSize += it.length() }
        crashLogFile?.let { if (it.exists()) totalSize += it.length() }
        return totalSize
    }

    /**
     * 获取格式化的日志大小字符串
     */
    fun getFormattedLogSize(): String {
        val sizeInBytes = getTotalLogSize()
        return formatFileSize(sizeInBytes)
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes < 1024) {
            return "$sizeInBytes B"
        }
        val sizeInKB = sizeInBytes / 1024.0
        if (sizeInKB < 1024) {
            return String.format("%.2f KB", sizeInKB)
        }
        val sizeInMB = sizeInKB / 1024.0
        if (sizeInMB < 1024) {
            return String.format("%.2f MB", sizeInMB)
        }
        val sizeInGB = sizeInMB / 1024.0
        return String.format("%.2f GB", sizeInGB)
    }
}