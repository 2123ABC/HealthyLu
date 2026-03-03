package ccb.crayalsokakamiee.healthylu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderConfigActivity : AppCompatActivity() {
    
    private lateinit var configListContainer: LinearLayout
    private lateinit var tvEmptyHint: TextView
    private lateinit var btnBack: View
    private lateinit var btnAdd: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
    
    private val configs = mutableListOf<ReminderConfig>()
    
    companion object {
        private const val REQUEST_CODE_IMPORT = 2001
        private const val REQUEST_CODE_EXPORT = 2002
    }
    
    override fun attachBaseContext(newBase: Context) {
        val context = LanguageManager.wrapContext(newBase)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_config)
        
        initViews()
        loadData()
    }
    
    private fun initViews() {
        configListContainer = findViewById(R.id.configListContainer)
        tvEmptyHint = findViewById(R.id.tvEmptyHint)
        btnBack = findViewById(R.id.btnBack)
        btnAdd = findViewById(R.id.btnAdd)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        
        btnBack.setOnClickListener {
            finish()
        }
        
        btnAdd.setOnClickListener {
            showAddConfigDialog()
        }
        
        btnExport.setOnClickListener {
            exportConfig()
        }
        
        btnImport.setOnClickListener {
            importConfig()
        }
    }
    
    private fun loadData() {
        configs.clear()
        configs.addAll(ReminderConfigManager.getAllConfigs(this))
        refreshList()
    }
    
    private fun refreshList() {
        // 移除除空提示外的所有视图
        val childCount = configListContainer.childCount
        for (i in childCount - 1 downTo 0) {
            val child = configListContainer.getChildAt(i)
            if (child.id != R.id.tvEmptyHint) {
                configListContainer.removeViewAt(i)
            }
        }
        
        // 显示/隐藏空提示
        tvEmptyHint.visibility = if (configs.isEmpty()) View.VISIBLE else View.GONE
        
        // 添加配置项
        configs.forEach { config ->
            addItemView(config)
        }
    }
    
    private fun addItemView(config: ReminderConfig) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_reminder_config, configListContainer, false)
        
        val cbEnabled = itemView.findViewById<CheckBox>(R.id.cbEnabled)
        val tvMessage = itemView.findViewById<TextView>(R.id.tvMessage)
        val itemContent = itemView.findViewById<LinearLayout>(R.id.itemContent)
        
        cbEnabled.isChecked = config.enabled
        tvMessage.text = config.message
        
        // 启用开关
        cbEnabled.setOnCheckedChangeListener { _, isChecked ->
            val updatedConfig = config.copy(enabled = isChecked)
            ReminderConfigManager.updateConfig(this, updatedConfig)
            // 更新本地列表
            val index = configs.indexOfFirst { it.id == config.id }
            if (index >= 0) {
                configs[index] = updatedConfig
            }
        }
        
        // 点击编辑
        itemContent.setOnClickListener {
            showEditConfigDialog(config)
        }
        
        // 长按删除
        itemContent.setOnLongClickListener {
            showDeleteConfirmDialog(config)
            true
        }
        
        configListContainer.addView(itemView, configListContainer.childCount - 1)
    }
    
    /**
     * 显示添加配置弹窗
     */
    private fun showAddConfigDialog() {
        val newConfig = ReminderConfig(
            id = ReminderConfig.generateId(),
            message = getString(R.string.reminder_default_message),
            enabled = true
        )
        showEditConfigDialog(newConfig, isNew = true)
    }
    
    /**
     * 显示编辑配置弹窗
     */
    private fun showEditConfigDialog(config: ReminderConfig, isNew: Boolean = false) {
        val themeResId = if (isDarkMode()) {
            R.style.ScaleFadeDialog_Dark
        } else {
            R.style.ScaleFadeDialog
        }
        
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_reminder_config_edit, null)
        
        val etMessage = dialogView.findViewById<EditText>(R.id.etMessage)
        
        // 设置当前值
        etMessage.setText(config.message)
        
        val dialog = AlertDialog.Builder(this, themeResId)
            .setTitle(if (isNew) R.string.btn_add_reminder else R.string.btn_edit_reminder)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val message = etMessage.text.toString().trim()
                
                if (message.isEmpty()) {
                    Toast.makeText(this, R.string.reminder_message_empty, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                val newConfig = ReminderConfig(
                    id = config.id,
                    message = message,
                    enabled = config.enabled
                )
                
                if (isNew) {
                    configs.add(newConfig)
                } else {
                    val index = configs.indexOfFirst { it.id == config.id }
                    if (index >= 0) {
                        configs[index] = newConfig
                    }
                }
                
                ReminderConfigManager.saveAllConfigs(this, configs)
                refreshList()
                Toast.makeText(this, R.string.reminder_config_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        
        dialog.show()
    }
    
    /**
     * 显示删除确认弹窗
     */
    private fun showDeleteConfirmDialog(config: ReminderConfig) {
        val themeResId = if (isDarkMode()) {
            R.style.ScaleFadeDialog_Dark
        } else {
            R.style.ScaleFadeDialog
        }
        
        AlertDialog.Builder(this, themeResId)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_reminder_msg, config.message))
            .setPositiveButton(R.string.confirm) { _, _ ->
                configs.removeAll { it.id == config.id }
                ReminderConfigManager.saveAllConfigs(this, configs)
                refreshList()
                Toast.makeText(this, R.string.reminder_config_deleted, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    /**
     * 导出配置
     */
    private fun exportConfig() {
        if (configs.isEmpty()) {
            Toast.makeText(this, R.string.no_config_to_export, Toast.LENGTH_SHORT).show()
            return
        }
        
        val fileName = "healthylu_reminder_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
        
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        
        try {
            startActivityForResult(intent, REQUEST_CODE_EXPORT)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.cannot_open_file_saver, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 导入配置
     */
    private fun importConfig() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        
        try {
            startActivityForResult(intent, REQUEST_CODE_IMPORT)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.cannot_open_file_picker, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (resultCode != Activity.RESULT_OK || data == null) return
        
        when (requestCode) {
            REQUEST_CODE_EXPORT -> {
                val uri = data.data
                if (uri != null) {
                    exportConfigToUri(uri)
                }
            }
            REQUEST_CODE_IMPORT -> {
                val uri = data.data
                if (uri != null) {
                    importConfigFromUri(uri)
                }
            }
        }
    }
    
    /**
     * 导出配置到指定Uri
     */
    private fun exportConfigToUri(uri: Uri) {
        try {
            val json = ReminderConfigManager.exportConfigToJson(this)
            
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray(Charsets.UTF_8))
            }
            
            Toast.makeText(this, R.string.reminder_config_exported, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 从指定Uri导入配置
     */
    private fun importConfigFromUri(uri: Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: throw Exception("无法读取文件")
            
            val result = ReminderConfigManager.importConfigFromJson(this, content, replaceExisting = false)
            
            if (result.success) {
                showImportOptionsDialog(content)
            } else {
                Toast.makeText(this, getString(R.string.import_failed, result.errorMessage), Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示导入选项弹窗
     */
    private fun showImportOptionsDialog(jsonContent: String) {
        val themeResId = if (isDarkMode()) {
            R.style.ScaleFadeDialog_Dark
        } else {
            R.style.ScaleFadeDialog
        }
        
        val options = arrayOf(
            getString(R.string.import_option_merge),
            getString(R.string.import_option_replace)
        )
        
        AlertDialog.Builder(this, themeResId)
            .setTitle(R.string.import_config_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 合并
                        val result = ReminderConfigManager.importConfigFromJson(this, jsonContent, replaceExisting = false)
                        loadData()
                        Toast.makeText(this, getString(R.string.import_result, result.addedCount, result.updatedCount), Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        // 替换
                        val result = ReminderConfigManager.importConfigFromJson(this, jsonContent, replaceExisting = true)
                        loadData()
                        Toast.makeText(this, getString(R.string.import_replaced, result.addedCount), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    /**
     * 检查当前是否是深色模式
     */
    private fun isDarkMode(): Boolean {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        return nightMode == AppCompatDelegate.MODE_NIGHT_YES ||
               (nightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM &&
                (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
    }
}