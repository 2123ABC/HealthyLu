package ccb.crayalsokakamiee.healthylu

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer

class CheckInFragment : Fragment() {
    private lateinit var tvWeekCount: TextView
    private lateinit var tvTodayCount: TextView
    private lateinit var ivPhase: ImageView
    private lateinit var tvReminder: TextView
    private lateinit var btnDrink: Button
    private lateinit var btnUndo: Button
    private lateinit var btnInfo: Button
    private lateinit var fireworksContainer: FrameLayout

    // 使用ViewModel来管理数据和逻辑
    private val viewModel: CheckInViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_checkin, container, false)
        
        // 创建烟花容器
        fireworksContainer = FrameLayout(requireContext())
        fireworksContainer.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        (view as ViewGroup).addView(fireworksContainer, 0)
        
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvWeekCount = view.findViewById(R.id.tvWeekCount)
        tvTodayCount = view.findViewById(R.id.tvTodayCount)
        ivPhase = view.findViewById(R.id.ivPhase)
        tvReminder = view.findViewById(R.id.tvReminder)
        btnDrink = view.findViewById(R.id.btnDrink)
        btnUndo = view.findViewById(R.id.btnUndo)
        btnInfo = view.findViewById(R.id.btnInfo)

        // 观察LiveData变化并更新UI
        observeData()

        btnDrink.setOnClickListener {
            viewModel.addWaterRecord()
            // 显示烟花效果
            showFireworks()
        }

        btnUndo.setOnClickListener {
            viewModel.undoWaterRecord()
        }
        
        btnInfo.setOnClickListener {
            showWaterBenefitsDialog()
        }
    }

    /**
     * 观察ViewModel中的LiveData变化
     */
    private fun observeData() {
        // 观察本周计数
        viewModel.weekCount.observe(viewLifecycleOwner, Observer { weekCount ->
            tvWeekCount.text = "本周已鹿管：$weekCount 次"
        })

        // 观察今日计数
        viewModel.todayCount.observe(viewLifecycleOwner, Observer { todayCount ->
            tvTodayCount.text = "今日已鹿管：$todayCount 次"
        })

        // 观察提醒文本
        viewModel.reminderText.observe(viewLifecycleOwner, Observer { text ->
            tvReminder.text = text
        })

        // 观察阶段图片
        viewModel.phaseRes.observe(viewLifecycleOwner, Observer { resId ->
            ivPhase.setImageResource(resId)
            // 根据深色模式应用反色滤镜
            applyInvertFilterIfDarkMode()
        })
    }

    private fun showWaterBenefitsDialog() {
        // 根据深色模式选择主题
        val themeResId = if (isDarkMode()) {
            R.style.ScaleFadeDialog_Dark
        } else {
            R.style.ScaleFadeDialog
        }
        
        AlertDialog.Builder(requireContext(), themeResId)
            .setTitle("鹿管过多的坏处")
            .setMessage(viewModel.getWaterBenefitsMessage())
            .setPositiveButton("知道了", null)
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

    /**
     * 如果是深色模式，给图片应用反色滤镜
     */
    private fun applyInvertFilterIfDarkMode() {
        if (isDarkMode()) {
            // 创建反色的ColorMatrix
            val invertMatrix = ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            val invertFilter = ColorMatrixColorFilter(invertMatrix)
            ivPhase.colorFilter = invertFilter
        } else {
            ivPhase.colorFilter = null
        }
    }

    private val fireworksHandler = Handler(Looper.getMainLooper())
    private val fragmentRef = java.lang.ref.WeakReference(this)
    private val cleanupRunnable = mutableListOf<Runnable>()

    private fun showFireworks() {
        val colors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#81C784"),
            Color.parseColor("#A5D6A7")
        )
        
        val btnLocation = IntArray(2)
        btnDrink.getLocationOnScreen(btnLocation)
        val centerX = btnLocation[0] + btnDrink.width / 2
        val centerY = btnLocation[1] + btnDrink.height / 2
        
        for (i in 0 until 3) {
            val ring = FireworkRing(requireContext())
            ring.setRingColor(colors[i])
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            ring.layoutParams = layoutParams
            ring.setCenterPoint(centerX, centerY)
            fireworksContainer.addView(ring)
            
            // 使用WeakReference避免内存泄漏
            val startRunnable = object : Runnable {
                override fun run() {
                    val fragment = fragmentRef.get()
                    if (fragment != null && fragment.isAdded) {
                        ring.startAnimation()
                    }
                }
            }
            fireworksHandler.postDelayed(startRunnable, i * 100L)
            cleanupRunnable.add(startRunnable)
            
            val removeRunnable = object : Runnable {
                override fun run() {
                    val fragment = fragmentRef.get()
                    if (fragment != null && fragment.isAdded && fireworksContainer.childCount > 0) {
                        fireworksContainer.removeView(ring)
                    }
                }
            }
            fireworksHandler.postDelayed(removeRunnable, 1200L)
            cleanupRunnable.add(removeRunnable)
        }
    }

    override fun onResume() {
        super.onResume()
        // 刷新数据
        viewModel.loadData()
        // 应用反色滤镜（如果需要）
        applyInvertFilterIfDarkMode()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 清理所有Handler回调
        cleanupRunnable.forEach { fireworksHandler.removeCallbacks(it) }
        cleanupRunnable.clear()
    }
}