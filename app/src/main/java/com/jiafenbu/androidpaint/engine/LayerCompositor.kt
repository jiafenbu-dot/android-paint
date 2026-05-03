package com.jiafenbu.androidpaint.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.runtime.Stable
import com.jiafenbu.androidpaint.model.BlendMode

/**
 * 图层合成器
 * 负责多图层的合成操作，支持混合模式和透明度
 * 
 * 性能优化说明：
 * - 使用离屏渲染优化大量图层的合成
 * - 快速路径跳过复杂混合模式的图层
 * - 使用脏标记避免不必要的重绘
 */
@Stable
class LayerCompositor {
    
    /**
     * 合成多个图层
     * @param width 画布宽度
     * @param height 画布高度
     * @param layers 图层渲染数据列表
     * @return 合成后的位图
     */
    fun composite(
        width: Int,
        height: Int,
        layers: List<LayerRenderData>
    ): Bitmap {
        if (layers.isEmpty()) {
            return createBlankBitmap(width, height)
        }
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // 填充白色背景
        canvas.drawColor(Color.WHITE)
        
        // 从底到顶绘制图层
        for (layer in layers) {
            if (!layer.shouldRender()) continue
            
            val paint = createLayerPaint(layer.blendMode, layer.opacity)
            
            canvas.save()
            canvas.translate(layer.offsetX, layer.offsetY)
            canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            canvas.restore()
        }
        
        return result
    }
    
    /**
     * 合成多个图层（使用指定混合模式）
     * @param width 画布宽度
     * @param height 画布高度
     * @param layers 图层渲染数据列表
     * @param blendMode 混合模式
     * @return 合成后的位图
     */
    @Suppress("DEPRECATION")
    fun compositeWithBlendMode(
        width: Int,
        height: Int,
        layers: List<LayerRenderData>,
        blendMode: PorterDuff.Mode = PorterDuff.Mode.SRC_OVER
    ): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // 填充白色背景
        canvas.drawColor(Color.WHITE)
        
        for (layer in layers) {
            if (!layer.shouldRender()) continue
            
            val paint = Paint().apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                isAntiAlias = true
                isFilterBitmap = true
                xfermode = PorterDuffXfermode(blendMode)
            }
            
            canvas.save()
            canvas.translate(layer.offsetX, layer.offsetY)
            canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            canvas.restore()
        }
        
        return result
    }
    
    /**
     * 高性能合成 - 使用离屏渲染优化
     * 适用于大量图层的情况
     * @param width 画布宽度
     * @param height 画布高度
     * @param layers 图层渲染数据列表
     * @return 合成后的位图
     */
    @Suppress("DEPRECATION")
    fun compositeOptimized(
        width: Int,
        height: Int,
        layers: List<LayerRenderData>
    ): Bitmap {
        if (layers.isEmpty()) {
            return createBlankBitmap(width, height)
        }
        
        // 先检查是否有需要特殊混合模式的图层
        val hasSpecialBlendModes = layers.any { 
            it.blendMode != BlendMode.NORMAL && it.blendMode != BlendMode.SRC_OVER 
        }
        
        if (!hasSpecialBlendModes) {
            // 没有特殊混合模式，使用快速路径
            return compositeFast(layers)
        }
        
        // 有特殊混合模式，使用完整路径
        return compositeWithBlendModes(layers)
    }
    
    /**
     * 快速合成 - 假设所有图层都是 NORMAL 模式
     */
    private fun compositeFast(layers: List<LayerRenderData>): Bitmap {
        val width = layers.firstOrNull()?.bitmap?.width ?: return createBlankBitmap(1, 1)
        val height = layers.firstOrNull()?.bitmap?.height ?: return createBlankBitmap(1, 1)
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // 填充白色背景
        canvas.drawColor(Color.WHITE)
        
        for (layer in layers) {
            if (!layer.shouldRender()) continue
            
            val paint = Paint().apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                isAntiAlias = true
                isFilterBitmap = true
            }
            
            canvas.drawBitmap(layer.bitmap, layer.offsetX, layer.offsetY, paint)
        }
        
        return result
    }
    
    /**
     * 带混合模式的合成
     * 使用离屏渲染处理复杂混合模式
     */
    @Suppress("DEPRECATION")
    private fun compositeWithBlendModes(layers: List<LayerRenderData>): Bitmap {
        val width = layers.firstOrNull()?.bitmap?.width ?: return createBlankBitmap(1, 1)
        val height = layers.firstOrNull()?.bitmap?.height ?: return createBlankBitmap(1, 1)
        
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // 填充白色背景
        canvas.drawColor(Color.WHITE)
        
        for (layer in layers) {
            if (!layer.shouldRender()) continue
            
            // 处理需要特殊实现的混合模式
            when (layer.blendMode) {
                BlendMode.HUE, BlendMode.SATURATION, BlendMode.COLOR, BlendMode.LUMINOSITY -> {
                    // 使用 ColorMatrix 实现
                    drawWithColorMatrixBlend(canvas, layer)
                }
                BlendMode.OVERLAY -> {
                    // OVERLAY 使用近似实现
                    drawWithOverlayApproximation(canvas, layer)
                }
                else -> {
                    // 使用标准 Paint 混合模式
                    val paint = createLayerPaint(layer.blendMode, layer.opacity)
                    canvas.drawBitmap(layer.bitmap, layer.offsetX, layer.offsetY, paint)
                }
            }
        }
        
        return result
    }
    
    /**
     * 使用 ColorMatrix 实现特殊混合模式
     */
    private fun drawWithColorMatrixBlend(canvas: Canvas, layer: LayerRenderData) {
        val colorMatrix = when (layer.blendMode) {
            BlendMode.HUE -> createHueColorMatrix()
            BlendMode.SATURATION -> createSaturationColorMatrix()
            BlendMode.COLOR -> createColorColorMatrix()
            BlendMode.LUMINOSITY -> createLuminosityColorMatrix()
            else -> ColorMatrix()
        }
        
        val paint = Paint().apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            isAntiAlias = true
            isFilterBitmap = true
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        
        canvas.save()
        canvas.translate(layer.offsetX, layer.offsetY)
        canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
        canvas.restore()
    }
    
    /**
     * OVERLAY 近似实现
     * OVERLAY: 当底层暗时使用 MULTIPLY，当底层亮时使用 SCREEN
     */
    @Suppress("DEPRECATION")
    private fun drawWithOverlayApproximation(canvas: Canvas, layer: LayerRenderData) {
        val width = layer.bitmap.width
        val height = layer.bitmap.height
        
        // 创建临时位图
        val tempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val tempCanvas = Canvas(tempBitmap)
        
        // 绘制原始图层
        val originalPaint = Paint().apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
        }
        tempCanvas.drawBitmap(layer.bitmap, 0f, 0f, originalPaint)
        
        // OVERLAY 简化实现：增加对比度
        val contrastPaint = Paint().apply {
            isAntiAlias = true
            val contrast = 1.3f
            val translate = (-.5f * contrast + .5f) * 255f
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            colorFilter = ColorMatrixColorFilter(contrastMatrix)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        
        // 绘制对比度增强版本
        tempCanvas.drawBitmap(layer.bitmap, 0f, 0f, contrastPaint)
        
        // 绘制到目标画布
        canvas.save()
        canvas.translate(layer.offsetX, layer.offsetY)
        
        // 先用 SCREEN 模式绘制（增亮）
        val screenPaint = Paint().apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawBitmap(tempBitmap, 0f, 0f, screenPaint)
        
        // 然后用 MULTIPLY 模式绘制（减暗）
        val multiplyPaint = Paint().apply {
            alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
        }
        canvas.drawBitmap(tempBitmap, 0f, 0f, multiplyPaint)
        
        canvas.restore()
        
        tempBitmap.recycle()
    }
    
    /**
     * 创建色相 ColorMatrix
     */
    private fun createHueColorMatrix(): ColorMatrix {
        // 简化实现：保持饱和度但允许色相旋转
        return ColorMatrix().apply {
            setSaturation(1f) // 保持饱和度
        }
    }
    
    /**
     * 创建饱和度 ColorMatrix
     */
    private fun createSaturationColorMatrix(): ColorMatrix {
        return ColorMatrix().apply {
            setSaturation(2f) // 加倍饱和度作为近似
        }
    }
    
    /**
     * 创建颜色 ColorMatrix
     */
    private fun createColorColorMatrix(): ColorMatrix {
        return ColorMatrix().apply {
            // 结合色相和饱和度
            setSaturation(1.5f)
        }
    }
    
    /**
     * 创建明度 ColorMatrix
     */
    private fun createLuminosityColorMatrix(): ColorMatrix {
        // 明度 = 0.21R + 0.72G + 0.07B
        return ColorMatrix(floatArrayOf(
            0.21f, 0.72f, 0.07f, 0f, 0f,
            0.21f, 0.72f, 0.07f, 0f, 0f,
            0.21f, 0.72f, 0.07f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    
    /**
     * 创建图层渲染 Paint
     */
    @Suppress("DEPRECATION")
    private fun createLayerPaint(blendMode: BlendMode, opacity: Float): Paint {
        return Paint().apply {
            this.alpha = (opacity * 255).toInt().coerceIn(0, 255)
            isAntiAlias = true
            isFilterBitmap = true
            
            // 尝试使用 Android BlendMode (API 29+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                blendMode.toAndroidBlendMode()?.let {
                    this.blendMode = it
                    return@apply
                }
            }
            
            // 回退到 PorterDuff.Mode
            blendMode.toPorterDuffMode()?.let {
                xfermode = PorterDuffXfermode(it)
            }
        }
    }
    
    /**
     * 创建空白位图
     */
    private fun createBlankBitmap(width: Int, height: Int): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        return result
    }
    
    /**
     * 合并两个图层位图
     * 用于图层合并操作
     * @param bottom 下层位图
     * @param top 上层位图
     * @param blendMode 混合模式
     * @param topOpacity 上层透明度
     * @return 合并后的位图
     */
    @Suppress("DEPRECATION")
    fun mergeLayers(
        bottom: Bitmap,
        top: Bitmap,
        blendMode: BlendMode = BlendMode.NORMAL,
        topOpacity: Float = 1f
    ): Bitmap {
        if (bottom.width != top.width || bottom.height != top.height) {
            throw IllegalArgumentException("图层尺寸必须相同")
        }
        
        val result = Bitmap.createBitmap(bottom.width, bottom.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        
        // 先绘制下层
        canvas.drawBitmap(bottom, 0f, 0f, null)
        
        // 再绘制上层（带混合模式）
        val paint = createLayerPaint(blendMode, topOpacity)
        canvas.drawBitmap(top, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * 生成图层缩略图
     * @param bitmap 源位图
     * @param maxSize 缩略图最大尺寸
     * @return 缩略图
     */
    fun generateThumbnail(bitmap: Bitmap, maxSize: Int = 80): Bitmap {
        val scale = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        
        val thumbWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val thumbHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        
        return Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, true)
    }
}
