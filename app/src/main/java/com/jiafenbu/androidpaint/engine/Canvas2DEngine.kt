package com.jiafenbu.androidpaint.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.jiafenbu.androidpaint.brush.BrushRenderer
import com.jiafenbu.androidpaint.brush.BrushType
import com.jiafenbu.androidpaint.model.BlendMode
import com.jiafenbu.androidpaint.model.StrokeData

/**
 * Canvas 2D 渲染引擎实现
 * 使用 Android 的 Canvas API 进行位图渲染
 */
class Canvas2DEngine : DrawEngine {

    private var width: Int = 0
    private var height: Int = 0

    override fun initialize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun renderStroke(bitmap: Bitmap, stroke: StrokeData) {
        val canvas = Canvas(bitmap)
        BrushRenderer.renderStroke(canvas, stroke)
    }

    override fun rerenderLayer(bitmap: Bitmap, strokes: List<StrokeData>) {
        // 清空位图为透明
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // 重绘所有笔画
        for (stroke in strokes) {
            // 模糊笔和涂抹笔需要特殊处理
            if (stroke.brushDescriptor.type == BrushType.BLUR ||
                stroke.brushDescriptor.type == BrushType.SMUDGE) {
                BrushRenderer.renderStrokeWithBitmap(canvas, stroke, bitmap)
            } else {
                BrushRenderer.renderStroke(canvas, stroke)
            }
        }
    }

    override fun addStroke(bitmap: Bitmap, stroke: StrokeData) {
        val brushType = stroke.brushDescriptor.type

        // 模糊笔和涂抹笔需要操作已有像素
        if (brushType == BrushType.BLUR || brushType == BrushType.SMUDGE) {
            val canvas = Canvas(bitmap)
            BrushRenderer.renderStrokeWithBitmap(canvas, stroke, bitmap)
        } else {
            val canvas = Canvas(bitmap)
            BrushRenderer.renderStroke(canvas, stroke)
        }
    }

    override fun addStrokeWithLock(bitmap: Bitmap, layerBitmap: Bitmap, stroke: StrokeData) {
        if (bitmap.width != layerBitmap.width || bitmap.height != layerBitmap.height) {
            // 尺寸不匹配，fallback 到普通渲染
            addStroke(bitmap, stroke)
            return
        }

        // 1. 创建临时位图用于渲染新笔画
        val tempBitmap = createBlankBitmap(bitmap.width, bitmap.height)
        val tempCanvas = Canvas(tempBitmap)

        // 2. 在临时位图上渲染笔画
        BrushRenderer.renderStroke(tempCanvas, stroke)

        // 3. 使用原图层作为 mask，只保留已有像素区域的笔画
        val resultBitmap = createBlankBitmap(bitmap.width, bitmap.height)
        val resultCanvas = Canvas(resultBitmap)

        // 先清空
        resultCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // 使用 DST_IN 模式，用原图层的 alpha 通道作为 mask
        val maskPaint = Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }

        // 先绘制临时位图（带新笔画）
        resultCanvas.drawBitmap(tempBitmap, 0f, 0f, null)
        // 再用 mask 处理，只保留原图层已有像素区域
        resultCanvas.drawBitmap(layerBitmap, 0f, 0f, maskPaint)

        // 4. 将结果合并回原位图
        val mergeCanvas = Canvas(bitmap)

        // 简化实现：遍历像素，只在原图层有像素的位置写入新像素
        val pixels = IntArray(bitmap.width * bitmap.height)
        val tempPixels = IntArray(bitmap.width * bitmap.height)
        val resultPixels = IntArray(bitmap.width * bitmap.height)

        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        tempBitmap.getPixels(tempPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        resultBitmap.getPixels(resultPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (i in pixels.indices) {
            val existingPixel = pixels[i]
            val existingAlpha = (existingPixel shr 24) and 0xFF

            if (existingAlpha > 0) {
                // 原图层有像素，保留原像素
                resultPixels[i] = existingPixel
            }
            // 如果原图层没有像素，则保持透明（不添加新像素）
        }

        // 将结果设置到位图
        bitmap.setPixels(resultPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        // 清理临时位图
        tempBitmap.recycle()
        resultBitmap.recycle()
    }

    override fun compositeLayers(layers: List<LayerRenderData>): Bitmap {
        if (layers.isEmpty()) {
            return createBlankBitmap(width, height)
        }

        val result = createBlankBitmap(width, height)
        val canvas = Canvas(result)

        // 先填充白色背景
        canvas.drawColor(Color.WHITE)

        // 从底到顶绘制每个可见图层
        for (layer in layers) {
            if (!layer.shouldRender()) continue

            val paint = Paint().apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
                isAntiAlias = true
                isFilterBitmap = true

                // 应用混合模式
                applyBlendMode(this, layer.blendMode)
            }

            canvas.save()
            canvas.translate(layer.offsetX, layer.offsetY)
            canvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
            canvas.restore()
        }

        return result
    }

    override fun createBlankBitmap(width: Int, height: Int): Bitmap {
        val config = Bitmap.Config.ARGB_8888
        return Bitmap.createBitmap(width, height, config).apply {
            // 确保位图是透明的
            val canvas = Canvas(this)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }
    }

    override fun clearBitmap(bitmap: Bitmap) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    override fun cleanup() {
        // Canvas2D 不需要特别清理
    }

    /**
     * 为 Paint 应用混合模式
     * 兼容 API 26-29
     */
    @Suppress("DEPRECATION")
    private fun applyBlendMode(paint: Paint, blendMode: BlendMode) {
        // 清除之前的设置
        paint.xfermode = null
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            paint.blendMode = null
        }

        when (blendMode) {
            BlendMode.NORMAL, BlendMode.SRC_OVER -> {
                // 默认就是 SRC_OVER，不需要额外设置
            }
            else -> {
                // 尝试使用 Android BlendMode (API 29+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    blendMode.toAndroidBlendMode()?.let {
                        paint.blendMode = it
                        return
                    }
                }

                // 回退到 PorterDuff.Mode
                blendMode.toPorterDuffMode()?.let {
                    paint.xfermode = PorterDuffXfermode(it)
                }
            }
        }
    }

    /**
     * 为 BlendMode 转换为 Android BlendMode (API 29+)
     */
    private fun BlendMode.toAndroidBlendMode(): android.graphics.BlendMode? {
        return when (this) {
            BlendMode.NORMAL -> android.graphics.BlendMode.SRC_OVER
            BlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
            BlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
            BlendMode.OVERLAY -> android.graphics.BlendMode.OVERLAY
            BlendMode.SOFT_LIGHT -> android.graphics.BlendMode.SOFT_LIGHT
            BlendMode.HARD_LIGHT -> android.graphics.BlendMode.HARD_LIGHT
            BlendMode.COLOR_DODGE -> android.graphics.BlendMode.COLOR_DODGE
            BlendMode.COLOR_BURN -> android.graphics.BlendMode.COLOR_BURN
            BlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
            BlendMode.LIGHTEN -> android.graphics.BlendMode.LIGHTEN
            BlendMode.DIFFERENCE -> android.graphics.BlendMode.DIFFERENCE
            // HUE, SATURATION, COLOR, LUMINOSITY 需要使用 ColorMatrix 实现
            else -> null
        }
    }

    /**
     * 为 BlendMode 转换为 PorterDuff.Mode（用于旧版 API）
     */
    private fun BlendMode.toPorterDuffMode(): PorterDuff.Mode? {
        return when (this) {
            BlendMode.NORMAL, BlendMode.SRC_OVER -> PorterDuff.Mode.SRC_OVER
            BlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
            BlendMode.SCREEN -> PorterDuff.Mode.SCREEN
            BlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
            BlendMode.DARKEN -> PorterDuff.Mode.DARKEN
            BlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
            else -> null
        }
    }

    /**
     * 创建用于离屏渲染的 Paint
     * 支持混合模式和透明度
     */
    fun createLayerPaint(blendMode: BlendMode, opacity: Float): Paint {
        return Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
            applyBlendMode(this, blendMode)
        }
    }
}
