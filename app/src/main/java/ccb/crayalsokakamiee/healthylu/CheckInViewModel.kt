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
        val reminderText = when (weekCount) {
            0 -> "这周还没鹿管呢，快去🦌一发吧！"
            in 1..2 -> "本周第一次🦌，开启性福生活！"
            in 3..6 -> "不要再🦌了，舒服过后也是要休息一下的！"
            7 -> "小鹿燃尽了..."
            150 -> "！？内格夫？！"
            else -> "为了你的身体，这周不要再🦌了！"
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
        return "医学专家普遍认为，每周手淫两次或以上属于频繁手淫。过度手淫可能导致：\n生理问题：生殖系统长期充血，诱发前列腺炎、精囊炎、尿道炎等感染，增加早泄、阳痿风险。\n身心症状：精神萎靡、注意力不集中、腰膝酸软、免疫力下降。\n\n所以说疲了累了🦌一发的说法是不对的！"
    }
}
