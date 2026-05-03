package com.jiafenbu.androidpaint.command

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import com.jiafenbu.androidpaint.model.WatermarkConfig
import com.jiafenbu.androidpaint.model.WatermarkTileMode
import com.jiafenbu.androidpaint.model.WatermarkType

/**
 * 添加水印命令
 */
class AddWatermarkCommand(
    private val watermarkConfig: WatermarkConfig,
    private val watermarkList: MutableList<WatermarkConfig>
) : DrawCommand {

    override fun execute() {
        watermarkList.add(watermarkConfig)
    }

    override fun undo() {
        watermarkList.removeAll { it.id == watermarkConfig.id }
    }
}

/**
 * 修改水印命令
 */
class ModifyWatermarkCommand(
    private val oldConfig: WatermarkConfig,
    private val newConfig: WatermarkConfig,
    private val watermarkList: MutableList<WatermarkConfig>
) : DrawCommand {

    private var index: Int = -1

    override fun execute() {
        index = watermarkList.indexOfFirst { it.id == oldConfig.id }
        if (index >= 0) {
            watermarkList[index] = newConfig
        }
    }

    override fun undo() {
        if (index >= 0) {
            watermarkList[index] = oldConfig
        }
    }
}

/**
 * 删除水印命令
 */
class DeleteWatermarkCommand(
    private val watermarkConfig: WatermarkConfig,
    private val watermarkList: MutableList<WatermarkConfig>
) : DrawCommand {

    private var index: Int = -1

    override fun execute() {
        index = watermarkList.indexOfFirst { it.id == watermarkConfig.id }
        watermarkList.removeAll { it.id == watermarkConfig.id }
    }

    override fun undo() {
        if (index >= 0) {
            watermarkList.add(index, watermarkConfig)
        } else {
            watermarkList.add(watermarkConfig)
        }
    }
}

/**
 * 水印渲染工具类
 * 负责将水印渲染到画布上
 */
object WatermarkRenderer {

    /**
     * 渲染水印覆盖层
     *
     * @param config 水印配置
     * @param canvasWidth 画布宽度
     * @param canvasHeight 画布高度
     * @return 包含水印的位图
     */
    fun renderWatermarkOverlay(
        config: WatermarkConfig,
        canvasWidth: Int,
        canvasHeight: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (!config.isEnabled) return bitmap

        when (config.tileMode) {
            WatermarkTileMode.SINGLE -> renderSingleWatermark(canvas, config, canvasWidth, canvasHeight)
            WatermarkTileMode.DIAGONAL -> renderDiagonalWatermarks(canvas, config, canvasWidth, canvasHeight)
            WatermarkTileMode.GRID -> renderGridWatermarks(canvas, config, canvasWidth, canvasHeight)
        }

        return bitmap
    }

    /**
     * 渲染单个水印
     */
    private fun renderSingleWatermark(
        canvas: Canvas,
        config: WatermarkConfig,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        canvas.save()
        canvas.rotate(config.rotation, config.position.x, config.position.y)

        when (config.type) {
            WatermarkType.TEXT -> renderTextWatermark(canvas, config)
            WatermarkType.IMAGE -> renderImageWatermark(canvas, config)
        }

        canvas.restore()
    }

    /**
     * 渲染对角线平铺水印
     */
    private fun renderDiagonalWatermarks(
        canvas: Canvas,
        config: WatermarkConfig,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        // 计算水印尺寸
        val watermarkWidth = getWatermarkWidth(config)
        val watermarkHeight = getWatermarkHeight(config)

        // 旋转画布 30-45 度
        canvas.save()
        canvas.rotate(config.rotation, canvasWidth / 2f, canvasHeight / 2f)

        // 计算行列间距
        val spacing = watermarkWidth * 1.5f

        // 计算起始偏移
        val startX = -watermarkWidth
        val startY = -watermarkHeight

        // 绘制平铺水印
        var y = startY
        while (y < canvasHeight + watermarkHeight) {
            var x = startX
            while (x < canvasWidth + watermarkWidth) {
                canvas.save()
                canvas.translate(x, y)

                when (config.type) {
                    WatermarkType.TEXT -> renderTextWatermark(canvas, config.copy(position = Offset(0f, 0f)))
                    WatermarkType.IMAGE -> renderImageWatermark(canvas, config.copy(position = Offset(0f, 0f)))
                }

                canvas.restore()
                x += spacing
            }
            y += spacing
        }

        canvas.restore()
    }

    /**
     * 渲染网格平铺水印
     */
    private fun renderGridWatermarks(
        canvas: Canvas,
        config: WatermarkConfig,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        val watermarkWidth = getWatermarkWidth(config)
        val watermarkHeight = getWatermarkHeight(config)

        val spacingX = watermarkWidth * 1.5f
        val spacingY = watermarkHeight * 1.5f

        val startX = -watermarkWidth
        val startY = -watermarkHeight

        var y = startY
        while (y < canvasHeight + watermarkHeight) {
            var x = startX
            while (x < canvasWidth + watermarkWidth) {
                canvas.save()
                canvas.translate(x, y)

                when (config.type) {
                    WatermarkType.TEXT -> renderTextWatermark(canvas, config.copy(position = Offset(0f, 0f)))
                    WatermarkType.IMAGE -> renderImageWatermark(canvas, config.copy(position = Offset(0f, 0f)))
                }

                canvas.restore()
                x += spacingX
            }
            y += spacingY
        }
    }

    /**
     * 获取水印宽度
     */
    private fun getWatermarkWidth(config: WatermarkConfig): Float {
        return when (config.type) {
            WatermarkType.TEXT -> {
                val paint = Paint().apply {
                    textSize = config.fontSize * 3
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val lines = config.text.split("\n")
                var maxWidth = 0f
                lines.forEach { line ->
                    maxWidth = maxOf(maxWidth, paint.measureText(line))
                }
                maxWidth
            }
            WatermarkType.IMAGE -> {
                val bitmap = config.imageBitmap ?: return 100f
                (bitmap.width * config.scale).toFloat()
            }
        }
    }

    /**
     * 获取水印高度
     */
    private fun getWatermarkHeight(config: WatermarkConfig): Float {
        return when (config.type) {
            WatermarkType.TEXT -> {
                val paint = Paint().apply {
                    textSize = config.fontSize * 3
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val lines = config.text.split("\n")
                lines.size * paint.fontSpacing
            }
            WatermarkType.IMAGE -> {
                val bitmap = config.imageBitmap ?: return 100f
                (bitmap.height * config.scale).toFloat()
            }
        }
    }

    /**
     * 渲染文字水印
     */
    private fun renderTextWatermark(canvas: Canvas, config: WatermarkConfig) {
        val paint = Paint().apply {
            color = config.fontColor
            alpha = (config.opacity * 255).toInt()
            textSize = config.fontSize * 3 // 转换为 px
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val lines = config.text.split("\n")
        var y = config.position.y
        lines.forEach { line ->
            canvas.drawText(line, config.position.x, y, paint)
            y += paint.fontSpacing
        }
    }

    /**
     * 渲染图片水印
     */
    private fun renderImageWatermark(canvas: Canvas, config: WatermarkConfig) {
        val bitmap = config.imageBitmap ?: return

        val paint = Paint().apply {
            alpha = (config.opacity * 255).toInt()
            isAntiAlias = true
        }

        val scaledWidth = bitmap.width * config.scale
        val scaledHeight = bitmap.height * config.scale

        val left = config.position.x - scaledWidth / 2
        val top = config.position.y - scaledHeight / 2

        canvas.save()
        canvas.translate(config.position.x, config.position.y)
        canvas.rotate(config.rotation)
        canvas.translate(-config.position.x, -config.position.y)
        canvas.drawBitmap(
            bitmap,
            null,
            android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight),
            paint
        )
        canvas.restore()
    }
}
