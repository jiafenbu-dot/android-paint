package com.jiafenbu.androidpaint.command

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.jiafenbu.androidpaint.model.LayerModel
import com.jiafenbu.androidpaint.model.LayerType
import com.jiafenbu.androidpaint.model.TextAlignment
import com.jiafenbu.androidpaint.model.TextLayerModel

/**
 * 添加文字图层命令
 */
class AddTextLayerCommand(
    private val layers: MutableList<LayerModel>,
    private val cachedBitmaps: MutableMap<Long, Bitmap>,
    private val activeLayerIndex: Int,
    private val textModel: TextLayerModel,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
    private val createBlankBitmap: (Int, Int) -> Bitmap,
    private val layerIdGenerator: () -> Long
) : DrawCommand {

    private var newLayer: LayerModel? = null
    private var insertedIndex: Int = -1
    private var newLayerId: Long = -1

    override fun execute() {
        newLayerId = layerIdGenerator()
        insertedIndex = activeLayerIndex + 1

        // 创建新的文字图层
        val textLayer = LayerModel(
            id = newLayerId,
            name = "文字 ${layers.size + 1}",
            strokes = mutableListOf(),
            isVisible = true,
            opacity = 1f,
            blendMode = com.jiafenbu.androidpaint.model.BlendMode.NORMAL,
            isLocked = false,
            thumbnail = null,
            layerType = LayerType.TEXT,
            textLayerModel = textModel
        )

        newLayer = textLayer
        layers.add(insertedIndex, textLayer)

        // 创建图层位图
        val layerBitmap = createBlankBitmap(canvasWidth, canvasHeight)
        cachedBitmaps[newLayerId] = layerBitmap

        // 将文字光栅化到位图
        rasterizeTextToBitmap(layerBitmap, textModel)
    }

    override fun undo() {
        newLayer?.let { layer ->
            layers.remove(layer)
            cachedBitmaps[layer.id]?.let {
                if (!it.isRecycled) it.recycle()
            }
            cachedBitmaps.remove(layer.id)
        }
    }

    /**
     * 将文字光栅化到位图
     */
    private fun rasterizeTextToBitmap(bitmap: Bitmap, textModel: TextLayerModel) {
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        val paint = createTextPaint(textModel)
        val lines = textModel.text.split("\n")

        var x = textModel.position.x
        val y = textModel.position.y

        // 根据对齐方式调整 X 坐标
        x = when (textModel.alignment) {
            TextAlignment.LEFT -> textModel.position.x
            TextAlignment.CENTER -> textModel.position.x - paint.measureText(textModel.text) / 2
            TextAlignment.RIGHT -> textModel.position.x - paint.measureText(textModel.text)
        }

        var currentY = y
        lines.forEach { line ->
            canvas.save()
            canvas.rotate(textModel.rotation, textModel.position.x, textModel.position.y)
            canvas.drawText(line, x, currentY, paint)
            canvas.restore()
            currentY += paint.fontSpacing
        }
    }

    /**
     * 创建文字 Paint
     */
    private fun createTextPaint(textModel: TextLayerModel): Paint {
        val paint = Paint().apply {
            color = textModel.textColor
            textSize = textModel.fontSize * 3 // 转换为 px
            isAntiAlias = true
            textAlign = textModel.alignment.toAndroidAlign()

            // 设置字体样式
            val typefaceStyle = when {
                textModel.isBold && textModel.isItalic -> Typeface.BOLD_ITALIC
                textModel.isBold -> Typeface.BOLD
                textModel.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            // 设置字体
            typeface = if (!textModel.customFontPath.isNullOrEmpty()) {
                try {
                    Typeface.createFromFile(textModel.customFontPath)
                } catch (e: Exception) {
                    Typeface.create(textModel.fontFamily, typefaceStyle)
                }
            } else {
                Typeface.create(textModel.fontFamily, typefaceStyle)
            }

            // 设置描边
            if (textModel.strokeEnabled) {
                this.style = android.graphics.Paint.Style.STROKE
                strokeWidth = textModel.strokeWidth * 3
                color = textModel.strokeColor
            }

            // 设置阴影
            if (textModel.shadowEnabled) {
                setShadowLayer(
                    textModel.shadowBlurRadius * 3,
                    textModel.shadowOffsetX * 3,
                    textModel.shadowOffsetY * 3,
                    textModel.shadowColor
                )
            }
        }

        return paint
    }
}

/**
 * 修改文字属性命令
 */
class ModifyTextCommand(
    private val layer: LayerModel,
    private val cachedBitmap: Bitmap,
    private val oldTextModel: TextLayerModel,
    private val newTextModel: TextLayerModel,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
    private val createBlankBitmap: (Int, Int) -> Bitmap
) : DrawCommand {

    private var wasRasterized = false

    override fun execute() {
        // 更新图层中的文字模型
        layer.textLayerModel = newTextModel

        // 重新光栅化
        wasRasterized = rasterizeText()
    }

    override fun undo() {
        // 恢复旧的文字模型
        layer.textLayerModel = oldTextModel

        // 重新光栅化
        rasterizeText()
    }

    /**
     * 光栅化文字到位图
     */
    private fun rasterizeText(): Boolean {
        return try {
            val canvas = Canvas(cachedBitmap)

            // 清除位图
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

            val paint = createTextPaint(newTextModel)
            val lines = newTextModel.text.split("\n")

            var x = newTextModel.position.x
            val y = newTextModel.position.y

            // 根据对齐方式调整 X 坐标
            x = when (newTextModel.alignment) {
                TextAlignment.LEFT -> newTextModel.position.x
                TextAlignment.CENTER -> {
                    var maxWidth = 0f
                    lines.forEach { line ->
                        maxWidth = maxOf(maxWidth, paint.measureText(line))
                    }
                    newTextModel.position.x - maxWidth / 2
                }
                TextAlignment.RIGHT -> {
                    var maxWidth = 0f
                    lines.forEach { line ->
                        maxWidth = maxOf(maxWidth, paint.measureText(line))
                    }
                    newTextModel.position.x - maxWidth
                }
            }

            var currentY = y
            canvas.save()
            canvas.rotate(newTextModel.rotation, newTextModel.position.x, newTextModel.position.y)

            // 绘制描边（如果启用）
            if (newTextModel.strokeEnabled) {
                val strokePaint = Paint(paint).apply {
                    this.style = android.graphics.Paint.Style.STROKE
                    strokeWidth = newTextModel.strokeWidth * 3
                    color = newTextModel.strokeColor
                    clearShadowLayer()
                }
                lines.forEach { line ->
                    canvas.drawText(line, x, currentY, strokePaint)
                    currentY += paint.fontSpacing
                }
                currentY = y
            }

            // 绘制填充
            lines.forEach { line ->
                canvas.drawText(line, x, currentY, paint)
                currentY += paint.fontSpacing
            }

            canvas.restore()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 创建文字 Paint
     */
    private fun createTextPaint(textModel: TextLayerModel): Paint {
        return Paint().apply {
            color = textModel.textColor
            textSize = textModel.fontSize * 3
            isAntiAlias = true
            textAlign = textModel.alignment.toAndroidAlign()

            val typefaceStyle = when {
                textModel.isBold && textModel.isItalic -> Typeface.BOLD_ITALIC
                textModel.isBold -> Typeface.BOLD
                textModel.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            typeface = if (!textModel.customFontPath.isNullOrEmpty()) {
                try {
                    Typeface.createFromFile(textModel.customFontPath)
                } catch (e: Exception) {
                    Typeface.create(textModel.fontFamily, style)
                }
            } else {
                Typeface.create(textModel.fontFamily, style)
            }

            // 设置阴影
            if (textModel.shadowEnabled) {
                setShadowLayer(
                    textModel.shadowBlurRadius * 3,
                    textModel.shadowOffsetX * 3,
                    textModel.shadowOffsetY * 3,
                    textModel.shadowColor
                )
            }
        }
    }
}

/**
 * 光栅化文字图层命令
 * 将文字图层转换为普通图层
 */
class RasterizeTextLayerCommand(
    private val layer: LayerModel,
    private val cachedBitmap: Bitmap,
    private val textModel: TextLayerModel,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
    private val createBlankBitmap: (Int, Int) -> Bitmap
) : DrawCommand {

    private var rasterizedBitmap: Bitmap? = null

    override fun execute() {
        // 保存当前位图
        rasterizedBitmap = cachedBitmap.copy(cachedBitmap.config, true)

        // 重新光栅化文字
        rasterizeText()

        // 将图层转换为普通图层
        layer.layerType = LayerType.NORMAL
        layer.textLayerModel = null
    }

    override fun undo() {
        // 恢复位图
        rasterizedBitmap?.let {
            if (!cachedBitmap.isRecycled) {
                val canvas = Canvas(cachedBitmap)
                canvas.drawBitmap(it, 0f, 0f, null)
            }
            it.recycle()
        }

        // 恢复文字图层
        layer.layerType = LayerType.TEXT
        layer.textLayerModel = textModel
    }

    /**
     * 光栅化文字
     */
    private fun rasterizeText() {
        val canvas = Canvas(cachedBitmap)
        canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        val paint = createTextPaint(textModel)
        val lines = textModel.text.split("\n")

        var x = textModel.position.x
        val y = textModel.position.y

        x = when (textModel.alignment) {
            TextAlignment.LEFT -> textModel.position.x
            TextAlignment.CENTER -> {
                var maxWidth = 0f
                lines.forEach { line ->
                    maxWidth = maxOf(maxWidth, paint.measureText(line))
                }
                textModel.position.x - maxWidth / 2
            }
            TextAlignment.RIGHT -> {
                var maxWidth = 0f
                lines.forEach { line ->
                    maxWidth = maxOf(maxWidth, paint.measureText(line))
                }
                textModel.position.x - maxWidth
            }
        }

        var currentY = y
        canvas.save()
        canvas.rotate(textModel.rotation, textModel.position.x, textModel.position.y)

        // 绘制描边
        if (textModel.strokeEnabled) {
            val strokePaint = Paint(paint).apply {
                this.style = android.graphics.Paint.Style.STROKE
                strokeWidth = textModel.strokeWidth * 3
                color = textModel.strokeColor
                clearShadowLayer()
            }
            lines.forEach { line ->
                canvas.drawText(line, x, currentY, strokePaint)
                currentY += paint.fontSpacing
            }
            currentY = y
        }

        // 绘制填充
        lines.forEach { line ->
            canvas.drawText(line, x, currentY, paint)
            currentY += paint.fontSpacing
        }

        canvas.restore()
    }

    private fun createTextPaint(textModel: TextLayerModel): Paint {
        return Paint().apply {
            color = textModel.textColor
            textSize = textModel.fontSize * 3
            isAntiAlias = true
            textAlign = textModel.alignment.toAndroidAlign()

            val typefaceStyle = when {
                textModel.isBold && textModel.isItalic -> Typeface.BOLD_ITALIC
                textModel.isBold -> Typeface.BOLD
                textModel.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            typeface = if (!textModel.customFontPath.isNullOrEmpty()) {
                try {
                    Typeface.createFromFile(textModel.customFontPath)
                } catch (e: Exception) {
                    Typeface.create(textModel.fontFamily, style)
                }
            } else {
                Typeface.create(textModel.fontFamily, style)
            }

            if (textModel.shadowEnabled) {
                setShadowLayer(
                    textModel.shadowBlurRadius * 3,
                    textModel.shadowOffsetX * 3,
                    textModel.shadowOffsetY * 3,
                    textModel.shadowColor
                )
            }
        }
    }
}
