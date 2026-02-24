package ccb.crayalsokakamiee.healthylu

import android.view.View
import androidx.viewpager2.widget.ViewPager2

/**
 * 自定义ViewPager2页面切换动画 - 淡入淡出效果
 * 支持跟手滑动
 */
class FadePageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // position: -1 到 1
        // -1: 完全在左侧
        // 0: 完全可见
        // 1: 完全在右侧
        
        val absPosition = kotlin.math.abs(position)
        
        if (absPosition >= 1f) {
            // 完全不可见时
            page.alpha = 0f
            page.scaleX = 0.95f
            page.scaleY = 0.95f
        } else {
            // 淡入淡出：越靠近中心越不透明
            page.alpha = 1f - absPosition
            
            // 缩放效果
            val scale = 0.95f + (1f - absPosition) * 0.05f
            page.scaleX = scale
            page.scaleY = scale
        }
        
        // 轻微位移增强滑动感
        page.translationX = -position * page.width * 0.15f
    }
}
