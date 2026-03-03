package ccb.crayalsokakamiee.healthylu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
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

class ThirdPartyAppConfigActivity : AppCompatActivity() {
    
    private lateinit var configListContainer: LinearLayout
    private lateinit var tvEmptyHint: TextView
    private lateinit var button: View
    private lateinit var btnAdd: Button
    private lateinit var btnExport: Button
    private lateinit var btnImport: Button
    
    private val configs = mutableListOf<ThirdPartyAppConfig>()
    
    companion object {
        private const val REQUEST_CODE_IMPORT = 1001
        private const val REQUEST_CODE_EXPORT = 1002
    }
    
    override fun attachBaseContext(newBase: Context) {
        val context = LanguageManager.wrapContext(newBase)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // 应用主题
        ThemeManager.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third_party_app_config)
        
        initViews()
        loadData()
    }
    
    private fun initViews() {
        configListContainer = findViewById(R.id.configListContainer)
        tvEmptyHint = findViewById(R.id.tvEmptyHint)
        button = findViewById(R.id.button)
        btnAdd = findViewById(R.id.btnAdd)
        btnExport = findViewById(R.id.btnExport)
        btnImport = findViewById(R.id.btnImport)
        
        button.setOnClickListener {
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
        configs.addAll(ThirdPartyAppConfigManager.getAllConfigs(this))
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
    
    private fun addItemView(config: ThirdPartyAppConfig) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_app_config, configListContainer, false)
        
        val btnConfigItem = itemView.findViewById<Button>(R.id.btnConfigItem)
        btnConfigItem.text = "${config.displayName} : ${config.packageName}"
        
        btnConfigItem.setOnClickListener {
            showEditConfigDialog(config)
        }
        
        // 长按删除
        btnConfigItem.setOnLongClickListener {
            showDeleteConfirmDialog(config)
            true
        }
        
        configListContainer.addView(itemView, configListContainer.childCount - 1)
    }
    
    /**
     * 显示添加配置弹窗
     */
    private fun showAddConfigDialog() {
        val newConfig = ThirdPartyAppConfig("", "", mutableListOf(""))
        showEditConfigDialog(newConfig, isNew = true)
    }
    
    /**
     * 显示编辑配置弹窗
     */
    private fun showEditConfigDialog(config: ThirdPartyAppConfig, isNew: Boolean = false) {
        val themeResId = if (isDarkMode()) {
            R.style.ScaleFadeDialog_Dark
        } else {
            R.style.ScaleFadeDialog
        }
        
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_app_config_edit, null)
        
        val etDisplayName = dialogView.findViewById<EditText>(R.id.etDisplayName)
        val etPackageName = dialogView.findViewById<EditText>(R.id.etPackageName)
        val customInfoContainer = dialogView.findViewById<LinearLayout>(R.id.customInfoContainer)
        val btnAddInfo = dialogView.findViewById<Button>(R.id.btnAddInfo)
        val btnRemoveInfo = dialogView.findViewById<Button>(R.id.btnRemoveInfo)
        
        // 填充现有数据
        etDisplayName.setText(config.displayName)
        etPackageName.setText(config.packageName)
        
        // 填充自定义信息
        val infoList = config.customInfos.toMutableList()
        infoList.forEachIndexed { index, info ->
            addCustomInfoItem(customInfoContainer, index + 1, info)
        }
        
        // 添加信息按钮
        btnAddInfo.setOnClickListener {
            val newIndex = customInfoContainer.childCount + 1
            addCustomInfoItem(customInfoContainer, newIndex, "")
        }
        
        // 删除信息按钮
        btnRemoveInfo.setOnClickListener {
            if (customInfoContainer.childCount > 1) {
                customInfoContainer.removeViewAt(customInfoContainer.childCount - 1)
            } else {
                Toast.makeText(this, R.string.keep_one_info, Toast.LENGTH_SHORT).show()
            }
        }
        
        val dialog = AlertDialog.Builder(this, themeResId)
            .setTitle(if (isNew) R.string.btn_add_config else R.string.btn_edit_config)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                // 收集数据
                val displayName = etDisplayName.text.toString().trim()
                val packageName = etPackageName.text.toString().trim()
                
                if (displayName.isEmpty()) {
                    Toast.makeText(this, R.string.enter_app_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                if (packageName.isEmpty()) {
                    Toast.makeText(this, R.string.enter_package_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // 收集自定义信息
                val customInfos = mutableListOf<String>()
                for (i in 0 until customInfoContainer.childCount) {
                    val child = customInfoContainer.getChildAt(i)
                    val etInfo = child.findViewById<EditText>(R.id.etCustomInfo)
                    customInfos.add(etInfo.text.toString().trim())
                }
                
                val newConfig = ThirdPartyAppConfig(packageName, displayName, customInfos)
                
                if (isNew) {
                    // 检查包名是否已存在
                    if (configs.any { it.packageName == packageName }) {
                        Toast.makeText(this, R.string.package_exists, Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    configs.add(newConfig)
                } else {
                    val index = configs.indexOfFirst { it.packageName == config.packageName }
                    if (index >= 0) {
                        configs[index] = newConfig
                    }
                }
                
                ThirdPartyAppConfigManager.saveAllConfigs(this, configs)
                refreshList()
                Toast.makeText(this, R.string.config_saved, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        
        dialog.show()
    }
    
    /**
     * 添加一条自定义信息输入项
     */
    private fun addCustomInfoItem(container: LinearLayout, index: Int, content: String) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_custom_info, container, false)
        
        val tvInfoLabel = itemView.findViewById<TextView>(R.id.tvInfoLabel)
        val etCustomInfo = itemView.findViewById<EditText>(R.id.etCustomInfo)
        
        tvInfoLabel.text = "${getString(R.string.custom_info_prefix)}$index"
        etCustomInfo.setText(content)
        
        container.addView(itemView)
    }
    
    /**
     * 显示删除确认弹窗
     */
    private fun showDeleteConfirmDialog(config: ThirdPartyAppConfig) {
        val themeResId = if (isDarkMode()) {
            R.style.ScaleFadeDialog_Dark
        } else {
            R.style.ScaleFadeDialog
        }
        
        AlertDialog.Builder(this, themeResId)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_msg, config.displayName))
            .setPositiveButton(R.string.confirm) { _, _ ->
                configs.removeAll { it.packageName == config.packageName }
                ThirdPartyAppConfigManager.saveAllConfigs(this, configs)
                refreshList()
                Toast.makeText(this, R.string.config_deleted, Toast.LENGTH_SHORT).show()
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
        
        // 使用系统文件选择器保存文件
        val fileName = "healthylu_config_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
        
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
            val jsonArray = org.json.JSONArray()
            configs.forEach { config ->
                jsonArray.put(config.toJson())
            }
            
            val rootJson = org.json.JSONObject().apply {
                put("version", 1)
                put("appName", "HealthyLu")
                put("configs", jsonArray)
            }
            
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            }
            
            Toast.makeText(this, R.string.config_exported, Toast.LENGTH_SHORT).show()
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
            
            val rootJson = org.json.JSONObject(content)
            val configsArray = rootJson.getJSONArray("configs")
            
            val importedConfigs = mutableListOf<ThirdPartyAppConfig>()
            for (i in 0 until configsArray.length()) {
                importedConfigs.add(ThirdPartyAppConfig.fromJson(configsArray.getJSONObject(i)))
            }
            
            // 显示导入选项
            showImportOptionsDialog(importedConfigs)
            
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示导入选项弹窗
     */
    private fun showImportOptionsDialog(importedConfigs: List<ThirdPartyAppConfig>) {
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
                        var added = 0
                        var updated = 0
                        importedConfigs.forEach { imported ->
                            val existingIndex = configs.indexOfFirst { it.packageName == imported.packageName }
                            if (existingIndex >= 0) {
                                configs[existingIndex] = imported
                                updated++
                            } else {
                                configs.add(imported)
                                added++
                            }
                        }
                        ThirdPartyAppConfigManager.saveAllConfigs(this, configs)
                        refreshList()
                        Toast.makeText(this, getString(R.string.import_result, added, updated), Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        // 替换
                        configs.clear()
                        configs.addAll(importedConfigs)
                        ThirdPartyAppConfigManager.saveAllConfigs(this, configs)
                        refreshList()
                        Toast.makeText(this, getString(R.string.import_replaced, importedConfigs.size), Toast.LENGTH_SHORT).show()
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