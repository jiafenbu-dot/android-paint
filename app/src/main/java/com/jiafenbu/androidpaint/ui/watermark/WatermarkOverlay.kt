package com.jiafenbu.androidpaint.ui.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jiafenbu.androidpaint.command.WatermarkRenderer
import com.jiafenbu.androidpaint.model.WatermarkConfig
import com.jiafenbu.androidpaint.model.WatermarkTileMode
import com.jiafenbu.androidpaint.model.WatermarkType
import kotlin.math.cos
import kotlin.math.sin

/**
 * 水印覆盖层组件
 * 用于在画布上显示水印
 */
@Composable
fun WatermarkOverlay(
    watermarkConfig: WatermarkConfig?,
    canvasWidth: Int,
    canvasHeight: Int,
    modifier: Modifier = Modifier
) {
    val watermarkBitmap = remember(watermarkConfig, canvasWidth, canvasHeight) {
        if (watermarkConfig == null || !watermarkConfig.isEnabled) {
            null
        } else {
            WatermarkRenderer.renderWatermarkOverlay(
                watermarkConfig,
                canvasWidth,
                canvasHeight
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        watermarkBitmap?.let { bitmap ->
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                // 将 Bitmap 绘制到 Canvas 上
                drawContext.canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
            }
        }
    }
}

/**
 * 水印预览组件
 * 在工具面板中预览水印效果
 */
@Composable
fun WatermarkPreview(
    watermarkConfig: WatermarkConfig,
    previewWidth: Int,
    previewHeight: Int,
    modifier: Modifier = Modifier
) {
    val previewBitmap = remember(watermarkConfig, previewWidth, previewHeight) {
        if (!watermarkConfig.isEnabled) {
            null
        } else {
            createPreviewBitmap(watermarkConfig, previewWidth, previewHeight)
        }
    }

    Box(modifier = modifier) {
        previewBitmap?.let { bitmap ->
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawContext.canvas.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
            }
        }
    }
}

/**
 * 创建预览位图
 */
private fun createPreviewBitmap(config: WatermarkConfig, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 绘制透明背景
    canvas.drawColor(android.graphics.Color.TRANSPARENT)

    if (!config.isEnabled) return bitmap

    when (config.tileMode) {
        WatermarkTileMode.SINGLE -> {
            canvas.save()
            canvas.rotate(config.rotation, width / 2f, height / 2f)
            when (config.type) {
                WatermarkType.TEXT -> drawTextWatermark(canvas, config, width / 2f, height / 2f)
                WatermarkType.IMAGE -> drawImageWatermark(canvas, config, width / 2f, height / 2f)
            }
            canvas.restore()
        }

        WatermarkTileMode.DIAGONAL -> {
            canvas.save()
            canvas.rotate(config.rotation, width / 2f, height / 2f)

            val watermarkSize = getWatermarkSize(config)
            val spacing = watermarkSize.width * 1.5f

            var y = -watermarkSize.height
            while (y < height + watermarkSize.height) {
                var x = -watermarkSize.width
                while (x < width + watermarkSize.width) {
                    canvas.save()
                    canvas.translate(x + watermarkSize.width / 2, y + watermarkSize.height / 2)
                    canvas.rotate(-config.rotation)

                    when (config.type) {
                        WatermarkType.TEXT -> drawTextWatermark(canvas, config.copy(position = androidx.compose.ui.geometry.Offset(-watermarkSize.width / 2, -watermarkSize.height / 2)), 0f, 0f)
                        WatermarkType.IMAGE -> drawImageWatermark(canvas, config.copy(position = androidx.compose.ui.geometry.Offset(watermarkSize.width / 2, watermarkSize.height / 2)), watermarkSize.width / 2, watermarkSize.height / 2)
                    }

                    canvas.restore()
                    x += spacing
                }
                y += spacing
            }

            canvas.restore()
        }

        WatermarkTileMode.GRID -> {
            val watermarkSize = getWatermarkSize(config)
            val spacingX = watermarkSize.width * 1.5f
            val spacingY = watermarkSize.height * 1.5f

            var y = -watermarkSize.height
            while (y < height + watermarkSize.height) {
                var x = -watermarkSize.width
                while (x < width + watermarkSize.width) {
                    canvas.save()
                    canvas.translate(x + watermarkSize.width / 2, y + watermarkSize.height / 2)
                    canvas.rotate(config.rotation)

                    when (config.type) {
                        WatermarkType.TEXT -> drawTextWatermark(canvas, config.copy(position = androidx.compose.ui.geometry.Offset(-watermarkSize.width / 2, -watermarkSize.height / 2)), 0f, 0f)
                        WatermarkType.IMAGE -> drawImageWatermark(canvas, config.copy(position = androidx.compose.ui.geometry.Offset(watermarkSize.width / 2, watermarkSize.height / 2)), watermarkSize.width / 2, watermarkSize.height / 2)
                    }

                    canvas.restore()
                    x += spacingX
                }
                y += spacingY
            }
        }
    }

    return bitmap
}

/**
 * 绘制文字水印
 */
private fun drawTextWatermark(canvas: Canvas, config: WatermarkConfig, centerX: Float, centerY: Float) {
    val paint = Paint().apply {
        color = config.fontColor
        alpha = (config.opacity * 255).toInt()
        textSize = config.fontSize * 3
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val lines = config.text.split("\n")
    val lineHeight = paint.fontSpacing
    val totalHeight = lineHeight * lines.size
    var y = centerY - totalHeight / 2 + lineHeight / 2

    lines.forEach { line ->
        canvas.drawText(line, centerX, y, paint)
        y += lineHeight
    }
}

/**
 * 绘制图片水印
 */
private fun drawImageWatermark(canvas: Canvas, config: WatermarkConfig, centerX: Float, centerY: Float) {
    val bitmap = config.imageBitmap ?: return

    val paint = Paint().apply {
        alpha = (config.opacity * 255).toInt()
        isAntiAlias = true
    }

    val scaledWidth = bitmap.width * config.scale * 0.5f
    val scaledHeight = bitmap.height * config.scale * 0.5f

    canvas.drawBitmap(
        bitmap,
        null,
        android.graphics.RectF(
            centerX - scaledWidth / 2,
            centerY - scaledHeight / 2,
            centerX + scaledWidth / 2,
            centerY + scaledHeight / 2
        ),
        paint
    )
}

/**
 * 获取水印尺寸
 */
private fun getWatermarkSize(config: WatermarkConfig): android.graphics.RectF {
    return when (config.type) {
        WatermarkType.TEXT -> {
            val paint = Paint().apply {
                textSize = config.fontSize * 3
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val lines = config.text.split("\n")
            var maxWidth = 0f
            var totalHeight = 0f
            lines.forEach { line ->
                maxWidth = maxOf(maxWidth, paint.measureText(line))
                totalHeight += paint.fontSpacing
            }
            android.graphics.RectF(0f, 0f, maxWidth, totalHeight)
        }
        WatermarkType.IMAGE -> {
            val bitmap = config.imageBitmap ?: return android.graphics.RectF(0f, 0f, 100f, 100f)
            android.graphics.RectF(
                0f, 0f,
                bitmap.width * config.scale * 0.5f,
                bitmap.height * config.scale * 0.5f
            )
        }
    }
}
