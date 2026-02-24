package ccb.crayalsokakamiee.healthylu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class TotalFragment : Fragment() {
    private lateinit var tvWeekVolume: TextView
    private lateinit var tvWeekCountInfo: TextView
    private lateinit var tvTodayVolume: TextView
    private lateinit var tvCountInfo: TextView
    private lateinit var waterRecordManager: WaterRecordManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_total, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        waterRecordManager = WaterRecordManager(requireContext())
        
        tvWeekVolume = view.findViewById(R.id.tvWeekVolume)
        tvWeekCountInfo = view.findViewById(R.id.tvWeekCountInfo)
        tvTodayVolume = view.findViewById(R.id.tvTodayVolume)
        tvCountInfo = view.findViewById(R.id.tvCountInfo)

        updateUI()
    }

    private fun updateUI() {
        // 本周统计
        val weekCount = waterRecordManager.getWeekCount()
        val weekVolume = weekCount * 4
        tvWeekVolume.text = "本周总量：${waterRecordManager.formatVolume(weekVolume)}"
        tvWeekCountInfo.text = "共 $weekCount 次"
        
        // 今日统计
        val todayCount = waterRecordManager.getTodayCount()
        val todayVolume = waterRecordManager.getTodayVolume()
        tvTodayVolume.text = "今日总量：${waterRecordManager.formatVolume(todayVolume)}"
        tvCountInfo.text = "共 $todayCount 次"
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }
}