package ccb.crayalsokakamiee.healthylu

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FireworkRing @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        alpha = 255
        isAntiAlias = true
    }

    private val glowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 16f
        alpha = 100
        isAntiAlias = true
        maskFilter = android.graphics.BlurMaskFilter(20f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }

    private var currentRadius = 60f
    private var currentAlpha = 255
    private var currentStrokeWidth = 8f
    private var centerX = 0f
    private var centerY = 0f

    fun setRingColor(color: Int) {
        paint.color = color
        glowPaint.color = color
    }

    fun setCenterPoint(x: Int, y: Int) {
        centerX = x.toFloat()
        centerY = y.toFloat()
    }

    fun startAnimation() {
        ValueAnimator.ofFloat(60f, 300f).apply {
            duration = 1000
            addUpdateListener { animator ->
                currentRadius = animator.animatedValue as Float
                currentAlpha = ((255f * (1 - animator.animatedFraction)).toInt())
                currentStrokeWidth = 8f * (1 - animator.animatedFraction * 0.875f)
                
                paint.alpha = currentAlpha
                paint.strokeWidth = currentStrokeWidth
                glowPaint.alpha = (currentAlpha * 0.5f).toInt()
                glowPaint.strokeWidth = currentStrokeWidth * 2f
                
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制发光效果
        canvas.drawCircle(centerX, centerY, currentRadius, glowPaint)
        
        // 绘制圆圈
        canvas.drawCircle(centerX, centerY, currentRadius, paint)
    }
}