package ccb.crayalsokakamiee.healthylu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CheckInViewModel - 管理打卡页面的数据和逻辑
 * 使用ViewModel在配置更改（如屏幕旋转）时保持数据
 */
class CheckInViewModel(
    application: Application,
    val savedStateHandle: SavedStateHandle? = null  // 改为可选参数
) : AndroidViewModel(application) {

    companion object {
        private const val KEY_WEEK_COUNT = "week_count"
        private const val KEY_TODAY_COUNT = "today_count"
        private const val KEY_REMINDER_TEXT = "reminder_text"
        private const val KEY_PHASE_RES = "phase_res"
    }

    private val waterRecordManager: WaterRecordManager by lazy {
        WaterRecordManager(application.applicationContext)
    }

    // 使用LiveData观察数据变化
    private val _weekCount = MutableLiveData<Int>()
    val weekCount: LiveData<Int> = _weekCount

    private val _todayCount = MutableLiveData<Int>()
    val todayCount: LiveData<Int> = _todayCount

    private val _reminderText = MutableLiveData<String>()
    val reminderText: LiveData<String> = _reminderText

    private val _phaseRes = MutableLiveData<Int>()
    val phaseRes: LiveData<Int> = _phaseRes

    init {
        // 尝试从SavedStateHandle恢复数据（如果可用）
        if (savedStateHandle != null) {
            _weekCount.value = savedStateHandle.get<Int>(KEY_WEEK_COUNT) ?: 0
            _todayCount.value = savedStateHandle.get<Int>(KEY_TODAY_COUNT) ?: 0
            _reminderText.value = savedStateHandle.get<String>(KEY_REMINDER_TEXT) ?: ""
            _phaseRes.value = savedStateHandle.get<Int>(KEY_PHASE_RES) ?: R.drawable.hl_phase1
        }
        
        // 如果是首次加载（SavedStateHandle为null或没有保存的数据），从数据库读取
        val hasSavedData = savedStateHandle?.contains(KEY_WEEK_COUNT) == true
        if (!hasSavedData) {
            loadData()
        }
    }

    /**
     * 从数据库加载数据
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                val todayCount = withContext(Dispatchers.IO) {
                    waterRecordManager.getTodayCount()
                }
                val weekCount = withContext(Dispatchers.IO) {
                    waterRecordManager.getWeekCount()
                }

                _todayCount.value = todayCount
                _weekCount.value = weekCount

                // 根据本周鹿管次数设置提醒语和图片
                updateReminderAndPhase(weekCount)
                
                // 保存到SavedStateHandle（如果可用）
                if (savedStateHandle != null) {
                    savedStateHandle.set(KEY_TODAY_COUNT, todayCount)
                    savedStateHandle.set(KEY_WEEK_COUNT, weekCount)
                }
            } catch (e: Exception) {
                android.util.Log.e("CheckInViewModel", "Error loading data: ${e.message}", e)
            }
        }
    }

    /**
     * 添加一次鹿管记录
     */
    fun addWaterRecord() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    waterRecordManager.addWaterRecord()
                }
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("CheckInViewModel", "Error adding record: ${e.message}", e)
            }
        }
    }

    /**
     * 撤销最后一次鹿管记录
     */
    fun undoWaterRecord() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    waterRecordManager.undoWaterRecord()
                }
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("CheckInViewModel", "Error undoing record: ${e.message}", e)
            }
        }
    }

    /**
     * 根据本周鹿管次数更新提醒语和图片
     */
    private fun updateReminderAndPhase(weekCount: Int) {
        val context = getApplication<Application>()
        val reminderText = when (weekCount) {
            0 -> context.getString(R.string.reminder_0)
            1 -> context.getString(R.string.reminder_1)
            2 -> context.getString(R.string.reminder_2)
            in 3..6 -> context.getString(R.string.reminder_3_6)
            7 -> context.getString(R.string.reminder_7)
            42 -> context.getString(R.string.reminder_42)
            in 8..149 -> context.getString(R.string.reminder_8_149)
            150 -> context.getString(R.string.reminder_150)
            233 -> context.getString(R.string.reminder_233)
            300 -> context.getString(R.string.reminder_300)
            520 -> context.getString(R.string.reminder_520)
            924 -> context.getString(R.string.reminder_924)
            1919 -> context.getString(R.string.reminder_1919)
            114514 -> context.getString(R.string.reminder_114514)
            260224 -> context.getString(R.string.reminder_260224)
            else -> context.getString(R.string.reminder_default)
        }

        val phaseRes = when (weekCount) {
            0 -> R.drawable.hl_phase1
            in 1..2 -> R.drawable.hl_phase2
            in 3..6 -> R.drawable.hl_phase3
            else -> R.drawable.hl_phase4
        }

        _reminderText.value = reminderText
        _phaseRes.value = phaseRes

        // 保存到SavedStateHandle（如果可用）
        if (savedStateHandle != null) {
            savedStateHandle.set(KEY_REMINDER_TEXT, reminderText)
            savedStateHandle.set(KEY_PHASE_RES, phaseRes)
        }
    }

    /**
     * 获取鹿管好处对话框的描述文本
     */
    fun getWaterBenefitsMessage(): String {
        return getApplication<Application>().getString(R.string.water_harm_content)
    }
}
