package ccb.crayalsokakamiee.healthylu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import java.io.InputStream

class BackgroundManager(private val context: Context) {
    
    companion object {
        private const val PREF_NAME = "background_settings"
        private const val KEY_BG_IMAGE_URI = "background_image_uri"
        private const val KEY_OVERLAY_COLOR = "overlay_color"
        private const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        private const val KEY_BG_TILING_MODE = "background_tiling_mode"
        private const val KEY_BG_TILING_AREA = "background_tiling_area" // 添加平铺区域设置
    }
    
    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    fun setBackgroundImage(container: FrameLayout) {
        // 获取保存的背景图片URI
        val imageUriString = sharedPreferences.getString(KEY_BG_IMAGE_URI, null)
        
        if (imageUriString != null) {
            try {
                val imageUri = Uri.parse(imageUriString)
                // 使用优化的Bitmap加载方法
                val bitmap = decodeSampledBitmapFromUri(imageUri, container.width, container.height)
                
                if (bitmap != null) {
                    // 根据设置的平铺模式创建 BitmapDrawable
                    val bitmapDrawable = createBitmapDrawableByMode(bitmap)
                    
                    // 应用遮罩到背景图片本身，而不是覆盖在UI上
                    val combinedDrawable = applyOverlayToBackground(bitmapDrawable)
                    
                    // 设置背景图片
                    container.background = combinedDrawable
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 如果没有图片或加载失败，使用默认背景
        container.background = ContextCompat.getDrawable(context, android.R.color.transparent)
    }
    
    /**
     * 优化的Bitmap加载方法，使用采样压缩和RGB_565配置降低内存占用
     */
    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            
            // 如果容器尺寸为0，使用屏幕尺寸
            val displayMetrics = context.resources.displayMetrics
            val actualReqWidth = if (reqWidth > 0) reqWidth else displayMetrics.widthPixels
            val actualReqHeight = if (reqHeight > 0) reqHeight else displayMetrics.heightPixels
            
            options.inSampleSize = calculateInSampleSize(options, actualReqWidth, actualReqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565  // 降低内存占用
            
            context.contentResolver.openInputStream(uri)?.use { decodedStream ->
                BitmapFactory.decodeStream(decodedStream, null, options)
            }
        }
    }
    
    /**
     * 计算合适的采样率
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    
    private fun createBitmapDrawableByMode(bitmap: Bitmap): BitmapDrawable {
        // 始终使用拉伸模式
        return BitmapDrawable(context.resources, bitmap)
    }
    
    private fun applyOverlayToBackground(backgroundDrawable: BitmapDrawable): Drawable {
        val overlayColor = sharedPreferences.getInt(KEY_OVERLAY_COLOR, -1)
        
        android.util.Log.d("BackgroundManager", "Overlay color: $overlayColor, alpha: ${Color.alpha(overlayColor)}")
        
        return if (overlayColor != -1) {
            // 直接使用颜色的 alpha 值，不再使用单独的 overlay_opacity
            val overlayDrawable = ColorDrawable(overlayColor)
            TintedBitmapDrawable(backgroundDrawable, overlayDrawable)
        } else {
            // 如果没有设置遮罩颜色，只返回背景图片
            android.util.Log.d("BackgroundManager", "No overlay color set, returning background only")
            backgroundDrawable
        }
    }
    
    fun clearBackgroundImage() {
        // 删除内部存储中的背景图片文件
        val imageUriString = sharedPreferences.getString(KEY_BG_IMAGE_URI, null)
        if (imageUriString != null && imageUriString.startsWith("file://")) {
            val filePath = imageUriString.substring(7) // 移除 "file://" 前缀
            val file = java.io.File(filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        
        // 清除 SharedPreferences 中的背景图片URI
        with(sharedPreferences.edit()) {
            remove(KEY_BG_IMAGE_URI)
            apply()
        }
    }
    
    fun clearBackgroundSettings() {
        with(sharedPreferences.edit()) {
            remove(KEY_BG_IMAGE_URI)
            remove(KEY_OVERLAY_COLOR)
            remove(KEY_OVERLAY_OPACITY)
            remove(KEY_BG_TILING_MODE)
            remove(KEY_BG_TILING_AREA)
            apply()
        }
    }
    
    fun getBackgroundImageUri(): String? {
        return sharedPreferences.getString(KEY_BG_IMAGE_URI, null)
    }
    
    fun getOverlayColor(): Int {
        return sharedPreferences.getInt(KEY_OVERLAY_COLOR, -1)
    }
    
    fun getOverlayOpacity(): Int {
        return sharedPreferences.getInt(KEY_OVERLAY_OPACITY, 50)
    }
    
    fun setBackgroundImageUri(uri: String?) {
        with(sharedPreferences.edit()) {
            putString(KEY_BG_IMAGE_URI, uri)
            apply()
        }
    }
    
    fun setOverlayColor(color: Int) {
        with(sharedPreferences.edit()) {
            putInt(KEY_OVERLAY_COLOR, color)
            apply()
        }
    }
    
    fun setOverlayOpacity(opacity: Int) {
        with(sharedPreferences.edit()) {
            putInt(KEY_OVERLAY_OPACITY, opacity)
            apply()
        }
    }
    
    fun setTilingMode(mode: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_BG_TILING_MODE, mode)
            apply()
        }
    }
    
    fun getTilingMode(): String {
        return sharedPreferences.getString(KEY_BG_TILING_MODE, "stretch") ?: "stretch"
    }
    
    fun setTilingArea(area: String) {
        with(sharedPreferences.edit()) {
            putString(KEY_BG_TILING_AREA, area)
            apply()
        }
    }
    
    fun getTilingArea(): String {
        return sharedPreferences.getString(KEY_BG_TILING_AREA, "fullscreen") ?: "fullscreen"
    }
}

// 自定义Drawable，将颜色遮罩应用到背景图片上
class TintedBitmapDrawable(
    private val bitmapDrawable: BitmapDrawable,
    private val overlayDrawable: ColorDrawable
) : Drawable() {
    
    private val paint = Paint().apply {
        isAntiAlias = true
    }
    
    override fun draw(canvas: Canvas) {
        // 绘制原始位图
        bitmapDrawable.setBounds(bounds)
        bitmapDrawable.draw(canvas)
        
        // 在位图上绘制颜色遮罩
        overlayDrawable.setBounds(bounds)
        overlayDrawable.draw(canvas)
    }
    
    override fun setAlpha(alpha: Int) {
        // 只设置遮罩的透明度，不影响背景图片
        overlayDrawable.alpha = alpha
    }
    
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        // 只设置遮罩的颜色过滤器，不影响背景图片
        overlayDrawable.colorFilter = colorFilter
    }
    
    override fun getOpacity(): Int {
        return android.graphics.PixelFormat.TRANSLUCENT
    }
}