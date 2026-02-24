package ccb.crayalsokakamiee.healthylu

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ccb.crayalsokakamiee.healthylu.R

class SettingsFragment : Fragment() {
    private lateinit var btnClearData: Button
    private lateinit var btnClearLogs: Button
    private lateinit var spinnerThemeMode: Spinner
    private lateinit var btnCheckPermissions: Button
    private lateinit var btnAuthor: Button
    private lateinit var btnGithub: Button
    private lateinit var cbBootStartup: CheckBox
    private lateinit var cbBackgroundService: CheckBox
    private lateinit var cbAppReminder: CheckBox
    private lateinit var tvVersion: TextView
    private lateinit var waterRecordManager: WaterRecordManager

    private var isSpinnerInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        waterRecordManager = WaterRecordManager(requireContext())

        btnClearData = view.findViewById(R.id.btnClearData)
        btnClearLogs = view.findViewById(R.id.btnClearLogs)
        btnCheckPermissions = view.findViewById(R.id.btnCheckPermissions)
        btnAuthor = view.findViewById(R.id.btnAuthor)
        btnGithub = view.findViewById(R.id.btnGithub)
        spinnerThemeMode = view.findViewById(R.id.spinnerThemeMode)
        cbBootStartup = view.findViewById(R.id.cbBootStartup)
        cbBackgroundService = view.findViewById(R.id.cbBackgroundService)
        cbAppReminder = view.findViewById(R.id.cbAppReminder)
        tvVersion = view.findViewById(R.id.tvVersion)
        
        // 设置版本号
        try {
            val versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            tvVersion.text = "版本 $versionName"
        } catch (e: Exception) {
            tvVersion.text = "版本 未知"
        }

        btnClearData.setOnClickListener {
            showClearDataDialog()
        }

        btnClearLogs.setOnClickListener {
            showClearLogsDialog()
        }

        btnCheckPermissions.setOnClickListener {
            checkAllPermissions()
        }

        btnAuthor.setOnClickListener {
            showAuthorInfo()
        }
        
        btnGithub.setOnClickListener {
            openGithubRepo()
        }
        
        // 设置功能开关
        setupFeatureToggles()
        
        // 设置主题模式选择器
        setupThemeModeSpinner()
        
        // 防止Spinner初始化时触发不必要的背景更新
        isSpinnerInitialized = false
        view.post {
            isSpinnerInitialized = true
        }

        // 加载已保存的设置
        loadSavedSettings()

        // 更新日志大小显示
        updateClearLogsButtonText()
    }
    
    private fun setupThemeModeSpinner() {
        val themeOptions = arrayOf("白天模式", "黑夜模式", "跟随系统")
        val themeValues = arrayOf(
            ThemeManager.THEME_MODE_LIGHT,
            ThemeManager.THEME_MODE_DARK,
            ThemeManager.THEME_MODE_SYSTEM
        )
        
        // 先设置监听器，但暂时不处理选中事件
        spinnerThemeMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 只有在完全初始化后才处理选中事件
                if (isSpinnerInitialized) {
                    val selectedMode = themeValues[position]
                    // 避免重复设置相同的主题
                    val currentMode = ThemeManager.getThemeMode(requireContext())
                    if (selectedMode != currentMode) {
                        ThemeManager.setThemeMode(requireContext(), selectedMode)
                        // 重启Activity以完全应用主题
                        requireActivity().recreate()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // 然后设置适配器和选中项
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            themeOptions
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerThemeMode.adapter = adapter
        
        // 获取当前保存的主题模式
        val currentThemeMode = ThemeManager.getThemeMode(requireContext())
        val selectedIndex = themeValues.indexOf(currentThemeMode)
        if (selectedIndex >= 0) {
            spinnerThemeMode.setSelection(selectedIndex, false)  // false表示不触发监听器
        }
    }
    
    /**
     * 设置功能开关（开机启动、后台驻留、应用提醒）
     */
    private fun setupFeatureToggles() {
        // 加载已保存的设置
        cbBootStartup.isChecked = AppSettingsManager.isBootStartupEnabled(requireContext())
        cbBackgroundService.isChecked = AppSettingsManager.isBackgroundServiceEnabled(requireContext())
        cbAppReminder.isChecked = AppSettingsManager.isAppReminderEnabled(requireContext())
        
        // 设置监听器
        cbBootStartup.setOnCheckedChangeListener { _, isChecked ->
            AppSettingsManager.setBootStartupEnabled(requireContext(), isChecked)
            showToast(if (isChecked) "已开启开机启动" else "已关闭开机启动")
        }
        
        cbBackgroundService.setOnCheckedChangeListener { _, isChecked ->
            AppSettingsManager.setBackgroundServiceEnabled(requireContext(), isChecked)
            showToast(if (isChecked) "已开启后台驻留" else "已关闭后台驻留")
            
            // 如果关闭后台驻留，停止服务；如果开启，启动服务
            if (!isChecked) {
                stopBackgroundService()
            } else {
                startBackgroundService()
            }
        }
        
        cbAppReminder.setOnCheckedChangeListener { _, isChecked ->
            AppSettingsManager.setAppReminderEnabled(requireContext(), isChecked)
            showToast(if (isChecked) "已开启应用提醒" else "已关闭应用提醒")
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun stopBackgroundService() {
        try {
            val intent = Intent(requireContext(), WaterReminderService::class.java)
            requireContext().stopService(intent)
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Error stopping service: ${e.message}")
        }
    }
    
    private fun startBackgroundService() {
        try {
            val intent = Intent(requireContext(), WaterReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent)
            } else {
                requireContext().startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "Error starting service: ${e.message}")
        }
    }
    
    private fun checkAllPermissions() {
        val hasUsageStats = hasUsageStatsPermission()
        val hasNotificationPermission = hasNotificationPermission()
        // 自启动权限无法直接检测，默认提示用户去检查
        
        val missingPermissions = mutableListOf<String>()
        
        if (!hasNotificationPermission) {
            missingPermissions.add("通知权限 - 用于显示鹿管提醒通知")
        }
        if (!hasUsageStats) {
            missingPermissions.add("使用情况访问权限 - 用于检测前台应用，在打开H软件时提醒打卡")
        }
        missingPermissions.add("自启动权限 - 用于开机后自动运行后台服务")
        
        if (missingPermissions.isNotEmpty()) {
            val message = buildString {
                append("以下权限需要开启以获得最佳体验：\n\n")
                missingPermissions.forEachIndexed { index, perm ->
                    append("${index + 1}. $perm\n\n")
                }
                append("点击\"开始设置\"将依次引导您开启各项权限。")
            }
            
            AlertDialog.Builder(requireContext())
                .setTitle("需要权限")
                .setMessage(message)
                .setPositiveButton("开始设置") { _, _ ->
                    // 开始依次引导用户设置权限
                    startPermissionSetupFlow()
                }
                .setNegativeButton("稍后再说", null)
                .show()
        } else {
            Toast.makeText(requireContext(), "所有权限已开启", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 依次引导用户开启各项权限
     */
    private var currentPermissionStep = 0
    
    private fun startPermissionSetupFlow() {
        currentPermissionStep = 0
        requestNextPermission()
    }
    
    private fun requestNextPermission() {
        when (currentPermissionStep) {
            0 -> {
                // 第一步：通知权限
                if (!hasNotificationPermission()) {
                    showPermissionGuideDialog(
                        "通知权限",
                        "请开启通知权限以接收鹿管提醒通知。",
                        "去开启"
                    ) {
                        openNotificationSettings()
                    }
                } else {
                    currentPermissionStep++
                    requestNextPermission()
                }
            }
            1 -> {
                // 第二步：使用情况访问权限
                if (!hasUsageStatsPermission()) {
                    showPermissionGuideDialog(
                        "使用情况访问权限",
                        "请开启使用情况访问权限以检测前台应用。\n\n在列表中找到「健康鹿管」并开启权限。",
                        "去开启"
                    ) {
                        openUsageStatsSettings()
                    }
                } else {
                    currentPermissionStep++
                    requestNextPermission()
                }
            }
            2 -> {
                // 第三步：自启动权限（引导用户去应用详情页）
                showPermissionGuideDialog(
                    "自启动权限",
                    "请在应用设置中开启自启动权限。\n\n不同手机设置方式不同：\n• 小米：应用设置 → 自启动管理\n• 华为：应用启动管理\n• OPPO/VIVO：自启动管理\n\n开启后应用将在开机时自动运行。",
                    "去设置"
                ) {
                    openAppDetailSettings()
                }
            }
            else -> {
                // 所有权限设置完成
                Toast.makeText(requireContext(), "权限设置完成！", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showPermissionGuideDialog(title: String, message: String, buttonText: String, action: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { _, _ ->
                action()
            }
            .setNegativeButton("跳过") { _, _ ->
                currentPermissionStep++
                requestNextPermission()
            }
            .setOnDismissListener {
                // 对话框关闭后，自动进入下一步
                // 注意：只有用户点击"跳过"或完成设置后才进入下一步
            }
            .show()
    }
    
    private fun openNotificationSettings() {
        try {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("android.provider.extra.APP_PACKAGE", requireContext().packageName)
            startActivity(intent)
        } catch (e: Exception) {
            // 如果失败，尝试打开应用详情页
            openAppDetailSettings()
        }
    }
    
    private fun openUsageStatsSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法打开设置页面", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openAppDetailSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + requireContext().packageName)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "无法打开设置页面", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = requireContext().getSystemService(android.app.NotificationManager::class.java)
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("确认清除")
            .setMessage("确定要清除所有数据吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                waterRecordManager.clearAllData()
                LogManager.clearLogs()
                Toast.makeText(requireContext(), "数据和日志已清除", Toast.LENGTH_SHORT).show()
                updateClearLogsButtonText()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadSavedSettings() {
        // 加载已保存的设置（目前没有需要加载的设置）
    }

    private fun showAuthorInfo() {
        val randomStatements = listOf(
            "做这个软件呢，有以下几个原因\n一是为了帮助我的好兄弟养成健康的习惯\n二是感觉这个项目挺有意思的\n\n你不觉得这种软件好像真的有点用吗？",
            "我去你的吧司马矛石玉伟书记\n放寒假13天我说白了比他妈的游戏新年活动都短\n戾气太重了真是对不起\n把这个关了再开一次看看别的？",
            "猪鼻李乃王也是出生\n没必要写的征文硬要我们写\nWhat can I say?"
        )
        val randomStatement = randomStatements[kotlin.random.Random.nextInt(randomStatements.size)]
        
        // 获取版本号
        val versionName = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) {
            "未知"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("关于作者")
            .setMessage("版本：$versionName\n\n作者：HengryCray(Also Kakamiee and MekoNacho)。\n\n这个软件终归是作用有限的，它能做到的只有监督你自律，而不是控制你自律\n\n$randomStatement")
            .setPositiveButton("去我的B站主页看看？顺便点个关注吧") { _, _ ->
                // 点击确定后跳转到 rickroll 网页
                val rickrollUrl = "https://space.bilibili.com/474494752"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rickrollUrl))
                startActivity(intent)
            }
            .setNegativeButton("算了算了白嫖软件挺好的", null)
            .show()
    }

    private fun openGithubRepo() {
        val githubUrl = "https://github.com/2123ABC/HealthyLu"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
        startActivity(intent)
    }

    /**
     * 更新清除日志按钮的文本，显示当前日志大小
     */
    private fun updateClearLogsButtonText() {
        val logSize = LogManager.getFormattedLogSize()
        btnClearLogs.text = "清除日志 ($logSize)"
    }

    /**
     * 显示清除日志确认对话框
     */
    private fun showClearLogsDialog() {
        val logSize = LogManager.getFormattedLogSize()

        AlertDialog.Builder(requireContext())
            .setTitle("确认清除日志")
            .setMessage("当前日志大小：$logSize\n\n确定要清除所有日志吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                LogManager.clearLogs()
                Toast.makeText(requireContext(), "日志已清除", Toast.LENGTH_SHORT).show()
                updateClearLogsButtonText()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}