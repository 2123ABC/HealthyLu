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
    private lateinit var spinnerLanguage: Spinner
    private lateinit var btnCheckPermissions: Button
    private lateinit var btnThirdPartyConfig: Button
    private lateinit var btnReminderConfig: Button
    private lateinit var btnAuthor: Button
    private lateinit var btnGithub: Button
    private lateinit var cbBootStartup: CheckBox
    private lateinit var cbBackgroundService: CheckBox
    private lateinit var cbAppReminder: CheckBox
    private lateinit var etReminderInterval: android.widget.EditText
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
        btnThirdPartyConfig = view.findViewById(R.id.btnThirdPartyConfig)
        btnReminderConfig = view.findViewById(R.id.btnReminderConfig)
        btnAuthor = view.findViewById(R.id.btnAuthor)
        btnGithub = view.findViewById(R.id.btnGithub)
        spinnerThemeMode = view.findViewById(R.id.spinnerThemeMode)
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        cbBootStartup = view.findViewById(R.id.cbBootStartup)
        cbBackgroundService = view.findViewById(R.id.cbBackgroundService)
        cbAppReminder = view.findViewById(R.id.cbAppReminder)
        etReminderInterval = view.findViewById(R.id.etReminderInterval)
        tvVersion = view.findViewById(R.id.tvVersion)
        
        // 设置版本号
        try {
            val versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            tvVersion.text = getString(R.string.version_format, versionName)
        } catch (e: Exception) {
            tvVersion.text = getString(R.string.version_unknown)
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

        btnThirdPartyConfig.setOnClickListener {
            openThirdPartyConfig()
        }

        btnReminderConfig.setOnClickListener {
            openReminderConfig()
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
        
        // 设置语言选择器
        setupLanguageSpinner()
        
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
        val themeOptions = arrayOf(
            getString(R.string.theme_light),
            getString(R.string.theme_dark),
            getString(R.string.theme_system)
        )
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
                        // 重启Activity以应用主题
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
    
    private fun setupLanguageSpinner() {
        val languageOptions = arrayOf(
            getString(R.string.language_system),
            getString(R.string.language_chinese),
            getString(R.string.language_english)
        )
        val languageValues = arrayOf(
            LanguageManager.LANGUAGE_SYSTEM,
            LanguageManager.LANGUAGE_CHINESE,
            LanguageManager.LANGUAGE_ENGLISH
        )
        
        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isSpinnerInitialized) {
                    val selectedLanguage = languageValues[position]
                    val currentLanguage = LanguageManager.getSavedLanguage(requireContext())
                    if (selectedLanguage != currentLanguage) {
                        LanguageManager.setLanguage(requireContext(), selectedLanguage)
                        showToast(getString(R.string.language_changed))
                        // 重启Activity以应用语言
                        requireActivity().recreate()
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            languageOptions
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
        
        // 获取当前保存的语言设置
        val currentLanguage = LanguageManager.getSavedLanguage(requireContext())
        val selectedIndex = languageValues.indexOf(currentLanguage)
        if (selectedIndex >= 0) {
            spinnerLanguage.setSelection(selectedIndex, false)
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
            showToast(if (isChecked) getString(R.string.boot_startup_enabled) else getString(R.string.boot_startup_disabled))
        }
        
        cbBackgroundService.setOnCheckedChangeListener { _, isChecked ->
            AppSettingsManager.setBackgroundServiceEnabled(requireContext(), isChecked)
            showToast(if (isChecked) getString(R.string.background_service_enabled) else getString(R.string.background_service_disabled))
            
            // 如果关闭后台驻留，停止服务；如果开启，启动服务
            if (!isChecked) {
                stopBackgroundService()
            } else {
                startBackgroundService()
            }
        }
        
        cbAppReminder.setOnCheckedChangeListener { _, isChecked ->
            AppSettingsManager.setAppReminderEnabled(requireContext(), isChecked)
            showToast(if (isChecked) getString(R.string.app_reminder_enabled) else getString(R.string.app_reminder_disabled))
        }
        
        // 提醒间隔设置
        val savedInterval = AppSettingsManager.getReminderIntervalMinutes(requireContext())
        etReminderInterval.setText(savedInterval.toString())
        
        etReminderInterval.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveReminderInterval()
            }
        }
    }
    
    /**
     * 保存提醒间隔设置
     */
    private fun saveReminderInterval() {
        val text = etReminderInterval.text.toString()
        val interval = text.toFloatOrNull()
        
        if (interval == null || interval <= 0) {
            showToast(getString(R.string.invalid_interval))
            etReminderInterval.setText(AppSettingsManager.getReminderIntervalMinutes(requireContext()).toString())
            return
        }
        
        AppSettingsManager.setReminderIntervalMinutes(requireContext(), interval)
        
        // 重新调度提醒
        WaterReminderReceiver.cancelReminder(requireContext())
        WaterReminderReceiver.scheduleHourlyReminder(requireContext())
        
        showToast(getString(R.string.reminder_interval_set, interval.toString()))
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    private fun stopBackgroundService() {
        try {
            val intent = Intent(requireContext(), WaterReminderService::class.java)
            requireContext().stopService(intent)
        } catch (e: Exception) {
            android.util.Log.e("SettingsFragment", "负责控制服务的玉伟书记说关闭有问题，理由是: ${e.message}")
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
            android.util.Log.e("SettingsFragment", "负责控制服务的玉伟书记说启动有问题，理由是: ${e.message}")
        }
    }
    
    private fun checkAllPermissions() {
        val hasUsageStats = hasUsageStatsPermission()
        val hasNotificationPermission = hasNotificationPermission()
        val hasExactAlarmPermission = hasExactAlarmPermission()
        
        val missingPermissions = mutableListOf<String>()
        
        if (!hasNotificationPermission) {
            missingPermissions.add(getString(R.string.permission_notification))
        }
        if (!hasExactAlarmPermission) {
            missingPermissions.add(getString(R.string.permission_exact_alarm))
        }
        if (!hasUsageStats) {
            missingPermissions.add(getString(R.string.permission_usage_stats))
        }
        
        if (missingPermissions.isNotEmpty()) {
            val message = buildString {
                append(getString(R.string.permissions_needed_desc))
                append("\n\n")
                missingPermissions.forEachIndexed { index, perm ->
                    append("${index + 1}. $perm\n\n")
                }
                append(getString(R.string.permissions_setup_guide))
            }
            
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.permissions_needed)
                .setMessage(message)
                .setPositiveButton(R.string.start_setup) { _, _ ->
                    // 开始依次引导用户设置权限
                    startPermissionSetupFlow()
                }
                .setNegativeButton(R.string.later, null)
                .show()
        } else {
            Toast.makeText(requireContext(), R.string.all_permissions_enabled, Toast.LENGTH_SHORT).show()
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
                        getString(R.string.notification_permission_title),
                        getString(R.string.notification_permission_desc),
                        getString(R.string.go_enable)
                    ) {
                        openNotificationSettings()
                    }
                } else {
                    currentPermissionStep++
                    requestNextPermission()
                }
            }
            1 -> {
                // 第二步：精确闹钟权限
                if (!hasExactAlarmPermission()) {
                    showPermissionGuideDialog(
                        getString(R.string.exact_alarm_permission_title),
                        getString(R.string.exact_alarm_permission_desc),
                        getString(R.string.go_enable)
                    ) {
                        openExactAlarmSettings()
                    }
                } else {
                    currentPermissionStep++
                    requestNextPermission()
                }
            }
            2 -> {
                // 第三步：使用情况访问权限
                if (!hasUsageStatsPermission()) {
                    showPermissionGuideDialog(
                        getString(R.string.usage_stats_permission_title),
                        getString(R.string.usage_stats_permission_desc),
                        getString(R.string.go_enable)
                    ) {
                        openUsageStatsSettings()
                    }
                } else {
                    currentPermissionStep++
                    requestNextPermission()
                }
            }
            else -> {
                // 所有权限设置完成
                Toast.makeText(requireContext(), R.string.permissions_setup_complete, Toast.LENGTH_SHORT).show()
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
            .setNegativeButton(R.string.skip) { _, _ ->
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
            Toast.makeText(requireContext(), R.string.cannot_open_settings, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openAppDetailSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + requireContext().packageName)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.cannot_open_settings, Toast.LENGTH_SHORT).show()
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
    
    /**
     * 检查是否有精确闹钟权限
     */
    private fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
    
    /**
     * 打开精确闹钟设置页面
     */
    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:" + requireContext().packageName)
                startActivity(intent)
            } catch (e: Exception) {
                // 如果失败，尝试打开应用详情页
                openAppDetailSettings()
            }
        }
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_clear)
            .setMessage(R.string.confirm_clear_data)
            .setPositiveButton(R.string.confirm) { _, _ ->
                waterRecordManager.clearAllData()
                LogManager.clearLogs()
                Toast.makeText(requireContext(), R.string.data_logs_cleared, Toast.LENGTH_SHORT).show()
                updateClearLogsButtonText()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadSavedSettings() {
        // 加载已保存的设置（目前没有需要加载的设置）
    }

    private fun showAuthorInfo() {
        val randomStatements = listOf(
            getString(R.string.author_statement_1),
            getString(R.string.author_statement_2),
            getString(R.string.author_statement_3)
        )
        val randomStatement = randomStatements[kotlin.random.Random.nextInt(randomStatements.size)]
        
        // 获取版本号
        val versionName = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (e: Exception) {
            getString(R.string.version_unknown)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.author_title)
            .setMessage("${getString(R.string.version)}: $versionName\n\n${getString(R.string.author_info)}\n\n${getString(R.string.author_desc)}\n\n$randomStatement")
            .setPositiveButton(R.string.author_visit_bilibili) { _, _ ->
                // 点击确定后跳转到 rickroll 网页
                val rickrollUrl = "https://space.bilibili.com/474494752"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rickrollUrl))
                startActivity(intent)
            }
            .setNegativeButton(R.string.author_skip, null)
            .show()
    }

    private fun openGithubRepo() {
        val githubUrl = "https://github.com/2123ABC/HealthyLu"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
        startActivity(intent)
    }

    private fun openThirdPartyConfig() {
        val intent = Intent(requireContext(), ThirdPartyAppConfigActivity::class.java)
        startActivity(intent)
    }

    private fun openReminderConfig() {
        val intent = Intent(requireContext(), ReminderConfigActivity::class.java)
        startActivity(intent)
    }

    /**
     * 更新清除日志按钮的文本，显示当前日志大小
     */
    private fun updateClearLogsButtonText() {
        val logSize = LogManager.getFormattedLogSize()
        btnClearLogs.text = "${getString(R.string.clear_logs)} ($logSize)"
    }

    /**
     * 显示清除日志确认对话框
     */
    private fun showClearLogsDialog() {
        val logSize = LogManager.getFormattedLogSize()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_clear_logs_title)
            .setMessage("${getString(R.string.current_log_size, logSize)}\n\n${getString(R.string.confirm_clear_logs_msg)}")
            .setPositiveButton(R.string.confirm) { _, _ ->
                LogManager.clearLogs()
                Toast.makeText(requireContext(), R.string.logs_cleared, Toast.LENGTH_SHORT).show()
                updateClearLogsButtonText()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}