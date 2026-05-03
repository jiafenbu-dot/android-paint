package com.jiafenbu.androidpaint.brush

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Bitmap
import com.jiafenbu.androidpaint.model.StrokeData
import com.jiafenbu.androidpaint.model.StrokePoint
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 笔刷渲染器
 * 负责将笔画数据渲染到 Canvas 或位图上
 */
object BrushRenderer {

    /**
     * 渲染单个笔画到 Canvas
     * @param canvas 目标画布
     * @param strokeData 笔画数据
     */
    fun renderStroke(canvas: Canvas, strokeData: StrokeData) {
        val points = strokeData.points
        if (points.isEmpty()) return

        // 填充笔在 DrawingCanvas 中单独处理
        if (strokeData.brushDescriptor.type == BrushType.FILL) return

        when (strokeData.brushDescriptor.type) {
            BrushType.PENCIL -> renderPencilStroke(canvas, points, strokeData)
            BrushType.INK_PEN -> renderInkPenStroke(canvas, points, strokeData)
            BrushType.WATERCOLOR -> renderWatercolorStroke(canvas, points, strokeData)
            BrushType.MARKER -> renderMarkerStroke(canvas, points, strokeData)
            BrushType.SPRAY -> renderSprayStroke(canvas, points, strokeData)
            BrushType.OIL_BRUSH -> renderOilBrushStroke(canvas, points, strokeData)
            BrushType.CRAYON -> renderCrayonStroke(canvas, points, strokeData)
            BrushType.BLUR -> { /* BLUR/SMUDGE 通过 renderStrokeWithBitmap 处理 */ }
            BrushType.SMUDGE -> { /* SMUDGE 通过 renderStrokeWithBitmap 处理 */ }
            BrushType.FILL -> { /* FILL 在 DrawingCanvas 中处理 */ }
            BrushType.ERASER -> renderEraserStroke(canvas, points, strokeData)
        }
    }

    /**
     * 需要操作已有像素的笔刷渲染（模糊笔、涂抹笔）
     * @param canvas 目标画布
     * @param strokeData 笔画数据
     * @param layerBitmap 图层位图引用（用于读取已有像素）
     */
    fun renderStrokeWithBitmap(canvas: Canvas, strokeData: StrokeData, layerBitmap: Bitmap) {
        val points = strokeData.points
        if (points.isEmpty()) return

        when (strokeData.brushDescriptor.type) {
            BrushType.BLUR -> renderBlurStroke(canvas, points, strokeData, layerBitmap)
            BrushType.SMUDGE -> renderSmudgeStroke(canvas, points, strokeData, layerBitmap)
            else -> renderStroke(canvas, strokeData)
        }
    }

    // ==================== 基础笔刷渲染 ====================

    /**
     * 渲染铅笔笔画
     * 特点：细线硬边，有抖动效果
     */
    private fun renderPencilStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val paint = createBasePaint(strokeData).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeMiter = 1f
            maskFilter = BlurMaskFilter(0.5f, BlurMaskFilter.Blur.NORMAL)
        }

        val path = createJitterPath(points, strokeData.brushDescriptor)
        canvas.drawPath(path, paint)
    }

    /**
     * 渲染钢笔笔画
     * 特点：平滑曲线，无抖动，线条稳定
     */
    private fun renderInkPenStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val paint = createBasePaint(strokeData).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeMiter = 1f
        }

        val path = createSmoothPath(points)
        canvas.drawPath(path, paint)
    }

    /**
     * 渲染橡皮擦笔画
     * 使用 DestinationOut 混合模式实现擦除
     */
    private fun renderEraserStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeMiter = 1f
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            strokeWidth = strokeData.brushDescriptor.size
        }

        val path = createJitterPath(points, strokeData.brushDescriptor)
        canvas.drawPath(path, paint)
    }

    // ==================== 新增笔刷渲染 ====================

    /**
     * 渲染水彩笔画
     * 特点：半透明 + 边缘柔化 + 多层叠加变深效果
     */
    private fun renderWatercolorStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size

        // 外层：大、极透明、柔和模糊
        val outerPaint = Paint().apply {
            this.color = color
            alpha = ((descriptor.opacity * 0.2f) * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = size * 1.8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            maskFilter = BlurMaskFilter(size * 0.4f, BlurMaskFilter.Blur.NORMAL)
        }

        // 中层：中等透明、中等模糊
        val midPaint = Paint().apply {
            this.color = color
            alpha = ((descriptor.opacity * 0.5f) * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = size * 1.2f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            maskFilter = BlurMaskFilter(size * 0.2f, BlurMaskFilter.Blur.NORMAL)
        }

        // 内层：小、高透明、轻微模糊
        val innerPaint = Paint().apply {
            this.color = color
            alpha = ((descriptor.opacity * 0.8f) * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = size * 0.8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            maskFilter = BlurMaskFilter(size * 0.1f, BlurMaskFilter.Blur.NORMAL)
        }

        val spacing = size * descriptor.spacing
        val jitterAmount = descriptor.jitter * size * 0.3f
        val random = Random(points.hashCode())

        // 绘制多层水彩效果
        for ((paint, widthMult, alphaMult) in listOf(
            Triple(outerPaint, 1.8f, 1f),
            Triple(midPaint, 1.2f, 1f),
            Triple(innerPaint, 0.8f, 1f)
        )) {
            val layerPaint = Paint(paint).apply {
                strokeWidth = size * widthMult
            }
            val path = createJitterPathWithRandom(points, spacing, jitterAmount, random, layerPaint.strokeWidth)
            canvas.drawPath(path, layerPaint)
        }
    }

    /**
     * 渲染马克笔笔画
     * 特点：较粗半透明线条，同一笔画内叠加不变深
     * 关键实现：在离屏位图上绘制完全不透明的笔画，然后用指定透明度合成到目标
     */
    private fun renderMarkerStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val opacity = descriptor.opacity

        // 不透明度（马克笔同一笔画重叠区域不应叠加透明度）
        // 先在离屏位图上以完全不透明绘制笔画，再以 opacity 合成到目标
        // 从画布 clipBounds 获取尺寸
        val clipBounds = canvas.clipBounds
        val width = if (clipBounds.width() > 0) clipBounds.width().toInt() else 1080
        val height = if (clipBounds.height() > 0) clipBounds.height().toInt() else 1920

        if (width <= 0 || height <= 0) return

        val offscreen: Bitmap
        try {
            offscreen = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            // 内存不足时回退到普通渲染
            renderMarkerStrokeFallback(canvas, points, strokeData)
            return
        }

        val offCanvas = Canvas(offscreen)
        offCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // 在离屏画布上绘制完全不透明的马克笔笔画
        val fullPaint = Paint().apply {
            this.color = color
            alpha = 255
            style = Paint.Style.STROKE
            strokeWidth = descriptor.size
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeMiter = 1f
        }

        val spacing = descriptor.size * descriptor.spacing
        if (spacing <= 0) {
            // 无间距：直接画线
            val path = createSmoothPath(points)
            offCanvas.drawPath(path, fullPaint)
        } else {
            // 有间距：画断续线段
            val path = createSpacedPath(points, spacing)
            offCanvas.drawPath(path, fullPaint)
        }

        // 用指定透明度将离屏位图合成到目标
        val compositePaint = Paint().apply {
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
            isAntiAlias = true
        }
        canvas.drawBitmap(offscreen, 0f, 0f, compositePaint)

        offscreen.recycle()
    }

    /**
     * 马克笔回退渲染（内存不足时使用）
     */
    private fun renderMarkerStrokeFallback(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val paint = createBasePaint(strokeData).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeMiter = 1f
        }
        val path = createSmoothPath(points)
        canvas.drawPath(path, paint)
    }

    /**
     * 渲染喷枪笔画
     * 特点：在每个点周围散布随机小点，密度由 spacing 控制
     */
    private fun renderSprayStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val alpha = (descriptor.opacity * 255).toInt().coerceIn(0, 255)

        val spacing = size * descriptor.spacing
        val jitterAmount = descriptor.jitter * size * 0.5f
        val random = Random(points.hashCode())

        val paint = Paint().apply {
            this.color = color
            this.alpha = alpha
            isAntiAlias = true
        }

        // 计算散点密度：size * 0.3 为参考密度
        val density = (size * 0.3f).toInt().coerceIn(3, 200)

        var lastX = points[0].x
        var lastY = points[0].y

        for (i in 1 until points.size) {
            val current = points[i]
            var dx = current.x - lastX
            var dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            // 添加抖动
            if (jitterAmount > 0) {
                dx += (random.nextFloat() - 0.5f) * jitterAmount
                dy += (random.nextFloat() - 0.5f) * jitterAmount
            }

            val targetX = lastX + dx
            val targetY = lastY + dy

            // 在当前点到目标点之间插值
            val steps = (dist / spacing.coerceAtLeast(1f)).toInt().coerceIn(1, 50)
            for (s in 0..steps) {
                val t = if (steps == 0) 0f else s.toFloat() / steps
                val px = lastX + (targetX - lastX) * t
                val py = lastY + (targetY - lastY) * t

                // 在该点周围散布随机小点
                for (dotIndex in 0 until density) {
                    val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                    val radius = random.nextFloat() * size * 0.5f
                    val dotX = px + kotlin.math.cos(angle) * radius
                    val dotY = py + kotlin.math.sin(angle) * radius
                    val dotSize = random.nextFloat() * 2f + 0.5f
                    canvas.drawCircle(dotX, dotY, dotSize, paint)
                }
            }

            lastX = targetX
            lastY = targetY
        }
    }

    /**
     * 渲染油画笔笔画
     * 特点：粗线条 + 纹理感（使用噪点模拟画布纹理）
     * 笔触边缘粗糙，用多个偏移的细线模拟笔毛
     */
    private fun renderOilBrushStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val opacity = descriptor.opacity

        val random = Random(points.hashCode())
        val spacing = size * descriptor.spacing
        val jitterAmount = descriptor.jitter * size * 0.4f

        // 绘制多条偏移的细线模拟笔毛纹理
        val strokeCount = 3
        for (strokeIdx in 0 until strokeCount) {
            val strokeJitter = if (jitterAmount > 0) {
                val jx = (random.nextFloat() - 0.5f) * jitterAmount
                val jy = (random.nextFloat() - 0.5f) * jitterAmount
                Pair(jx, jy)
            } else Pair(0f, 0f)

            val paint = Paint().apply {
                this.color = color
                alpha = ((opacity * (0.7f + random.nextFloat() * 0.3f)) * 255).toInt().coerceIn(0, 255)
                style = Paint.Style.STROKE
                // 每条细线比主宽度略细
                strokeWidth = size * (0.6f + random.nextFloat() * 0.3f)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
                strokeMiter = 1f
            }

            val path = Path()
            if (points.size < 2) continue

            path.moveTo(points[0].x + strokeJitter.first, points[0].y + strokeJitter.second)

            var accumulatedDist = 0f
            var lastX = points[0].x
            var lastY = points[0].y

            for (i in 1 until points.size) {
                val current = points[i]
                var dx = current.x - lastX
                var dy = current.y - lastY
                val dist = sqrt(dx * dx + dy * dy)

                if (dist < 0.01f) continue

                if (jitterAmount > 0) {
                    dx += (random.nextFloat() - 0.5f) * jitterAmount
                    dy += (random.nextFloat() - 0.5f) * jitterAmount
                }

                accumulatedDist += dist

                if (accumulatedDist >= spacing || i == points.size - 1) {
                    val targetX = lastX + dx
                    val targetY = lastY + dy
                    path.lineTo(targetX + strokeJitter.first, targetY + strokeJitter.second)
                    lastX = targetX
                    lastY = targetY
                    accumulatedDist = 0f
                }
            }

            canvas.drawPath(path, paint)
        }

        // 添加画布纹理噪点：在笔画路径周围添加一些随机小斑点
        if (points.size >= 2) {
            val noisePaint = Paint().apply {
                this.color = color
                alpha = ((opacity * 0.3f) * 255).toInt().coerceIn(0, 255)
                isAntiAlias = true
            }

            var lastX = points[0].x
            var lastY = points[0].y
            for (i in 1 until points.size) {
                val current = points[i]
                val dist = sqrt(
                    (current.x - lastX) * (current.x - lastX) +
                    (current.y - lastY) * (current.y - lastY)
                )
                if (dist < 0.01f) continue

                val targetX = lastX + (current.x - lastX)
                val targetY = lastY + (current.y - lastY)
                val steps = (dist / (size * 0.2f)).toInt().coerceIn(1, 10)
                for (s in 0 until steps) {
                    val t = s.toFloat() / steps
                    val px = lastX + (targetX - lastX) * t
                    val py = lastY + (targetY - lastY) * t
                    // 在路径周围撒噪点
                    for (n in 0 until 3) {
                        val ox = (random.nextFloat() - 0.5f) * size * 0.6f
                        val oy = (random.nextFloat() - 0.5f) * size * 0.6f
                        val dotSize = random.nextFloat() * 2f + 0.5f
                        canvas.drawCircle(px + ox, py + oy, dotSize, noisePaint)
                    }
                }
                lastX = targetX
                lastY = targetY
            }
        }
    }

    /**
     * 渲染蜡笔笔画
     * 特点：粗糙颗粒感，使用随机间隔的短线段
     * 模拟蜡笔在纸上留下的断续痕迹
     */
    private fun renderCrayonStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val opacity = descriptor.opacity

        val random = Random(points.hashCode())
        val spacing = size * descriptor.spacing
        val jitterAmount = descriptor.jitter * size * 0.6f

        val paint = Paint().apply {
            this.color = color
            this.alpha = (opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE // 蜡笔用方形笔触更真实
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeMiter = 1f
        }

        if (points.size < 2) return

        var lastX = points[0].x
        var lastY = points[0].y

        for (i in 1 until points.size) {
            val current = points[i]
            var dx = current.x - lastX
            var dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            // 添加较大抖动
            if (jitterAmount > 0) {
                dx += (random.nextFloat() - 0.5f) * jitterAmount
                dy += (random.nextFloat() - 0.5f) * jitterAmount
            }

            val targetX = lastX + dx
            val targetY = lastY + dy

            // 将长线段分成多个随机短段
            val segments = (dist / spacing.coerceAtLeast(size * 0.5f)).toInt().coerceIn(1, 8)
            var segX = lastX
            var segY = lastY

            for (seg in 0 until segments) {
                val t = if (segments == 1) 1f else (seg + random.nextFloat() * 0.5f + 0.5f).coerceAtMost(segments.toFloat()) / segments
                val ex = lastX + (targetX - lastX) * t
                val ey = lastY + (targetY - lastY) * t

                // 每段宽度有随机变化
                paint.strokeWidth = size * (0.5f + random.nextFloat() * 0.5f)

                // 画短线段
                canvas.drawLine(segX, segY, ex, ey, paint)

                // 跳过间隙（蜡笔断续效果）
                segX = ex
                segY = ey
            }

            lastX = targetX
            lastY = targetY
        }
    }

    /**
     * 渲染模糊笔画
     * 特点：取笔触区域的已有颜色做多次均值模糊（模拟高斯模糊）
     * 实现：取笔触区域像素，用 BoxBlur（多次均值模糊）近似高斯，再写回
     */
    private fun renderBlurStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData,
        layerBitmap: Bitmap
    ) {
        val descriptor = strokeData.brushDescriptor
        val size = descriptor.size
        val radius = (size * 0.5f).toInt().coerceAtLeast(2)

        if (points.isEmpty()) return

        // 获取位图像素
        val width = layerBitmap.width
        val height = layerBitmap.height
        if (width <= 0 || height <= 0) return

        val pixels = IntArray(width * height)
        layerBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // 计算笔触覆盖的矩形范围（加边距）
        var minX = points[0].x.toInt()
        var minY = points[0].y.toInt()
        var maxX = minX
        var maxY = minY
        for (p in points) {
            minX = minX.coerceAtMost(p.x.toInt())
            minY = minY.coerceAtMost(p.y.toInt())
            maxX = maxX.coerceAtLeast(p.x.toInt())
            maxY = maxY.coerceAtLeast(p.y.toInt())
        }

        val margin = radius + 2
        val x0 = (minX - margin).coerceIn(0, width - 1)
        val y0 = (minY - margin).coerceIn(0, height - 1)
        val x1 = (maxX + margin).coerceIn(0, width - 1)
        val y1 = (maxY + margin).coerceIn(0, height - 1)

        // 限制处理区域大小，防止性能问题
        val maxArea = 200 * 200
        val area = (x1 - x0 + 1) * (y1 - y0 + 1)
        if (area > maxArea) return

        // 对覆盖区域进行 BoxBlur（3次迭代近似高斯模糊）
        val blurred = pixels.copyOf()
        repeat(3) {
            boxBlurRowColumn(blurred, pixels, width, height, x0, y0, x1, y1, radius)
        }

        // 将模糊结果写回原数组
        for (y in y0..y1) {
            for (x in x0..x1) {
                pixels[y * width + x] = blurred[y * width + x]
            }
        }

        layerBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * BoxBlur 实现：先横向再纵向均值模糊
     */
    private fun boxBlurRowColumn(
        output: IntArray,
        input: IntArray,
        width: Int,
        height: Int,
        x0: Int,
        y0: Int,
        x1: Int,
        y1: Int,
        radius: Int
    ) {
        val w = width
        val h = height
        val temp = input.copyOf()

        // 横向模糊
        for (y in y0..y1) {
            for (x in x0..x1) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, w - 1)
                    val pixel = temp[y * w + nx]
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    count++
                }
                val idx = y * w + x
                output[idx] = (0xFF shl 24) or
                        ((r / count) shl 16) or
                        ((g / count) shl 8) or
                        (b / count)
            }
        }

        // 纵向模糊
        for (y in y0..y1) {
            for (x in x0..x1) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, h - 1)
                    val pixel = output[ny * w + x]
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    count++
                }
                val idx = y * w + x
                output[idx] = (0xFF shl 24) or
                        ((r / count) shl 16) or
                        ((g / count) shl 8) or
                        (b / count)
            }
        }
    }

    /**
     * 渲染涂抹笔画（简单版）
     * 特点：取笔触起始位置的颜色，沿拖拽方向"拖"出去
     * 简单实现：取前一个点的周围像素颜色，在当前点用该颜色绘制（带透明度递减）
     */
    private fun renderSmudgeStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData,
        layerBitmap: Bitmap
    ) {
        val descriptor = strokeData.brushDescriptor
        val size = descriptor.size
        val opacity = descriptor.opacity

        if (points.size < 2) return

        val width = layerBitmap.width
        val height = layerBitmap.height
        if (width <= 0 || height <= 0) return

        val pixels = IntArray(width * height)
        layerBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val sampleRadius = (size * 0.5f).toInt().coerceAtLeast(2)
        val random = Random(points.hashCode())

        var lastX = points[0].x
        var lastY = points[0].y

        // 涂抹笔沿路径逐步衰减透明度
        var fadeProgress = 0f

        for (i in 1 until points.size) {
            val current = points[i]
            val dx = current.x - lastX
            val dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            // 从前一个点周围取颜色
            val sampleX = lastX.toInt().coerceIn(0, width - 1)
            val sampleY = lastY.toInt().coerceIn(0, height - 1)
            val sampledColor = sampleColorAround(pixels, width, height, sampleX, sampleY, sampleRadius)

            // 绘制涂抹点：透明度沿拖拽方向递减
            fadeProgress = (fadeProgress + dist / (size * 2f)).coerceAtMost(1f)
            val pointOpacity = opacity * (1f - fadeProgress * 0.5f)

            val paint = Paint().apply {
                color = sampledColor
                alpha = (pointOpacity * 255).toInt().coerceIn(0, 255)
                isAntiAlias = true
            }

            // 画多个偏移点模拟涂抹拖尾
            val trailCount = 3
            for (t in 0 until trailCount) {
                val tRatio = t.toFloat() / trailCount
                val tx = lastX + dx * tRatio + (random.nextFloat() - 0.5f) * size * 0.3f
                val ty = lastY + dy * tRatio + (random.nextFloat() - 0.5f) * size * 0.3f
                val trailSize = size * (1f - tRatio * 0.5f)
                canvas.drawCircle(tx, ty, trailSize * 0.5f, paint)
            }

            lastX = current.x
            lastY = current.y
        }
    }

    /**
     * 在指定点周围采样颜色（取平均）
     */
    private fun sampleColorAround(
        pixels: IntArray,
        width: Int,
        height: Int,
        cx: Int,
        cy: Int,
        radius: Int
    ): Int {
        var r = 0
        var g = 0
        var b = 0
        var count = 0

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue
                val pixel = pixels[ny * width + nx]
                val alpha = (pixel shr 24) and 0xFF
                if (alpha > 10) { // 忽略完全透明的像素
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    count++
                }
            }
        }

        return if (count > 0) {
            (0xFF shl 24) or
                    ((r / count) shl 16) or
                    ((g / count) shl 8) or
                    (b / count)
        } else {
            0xFF808080.toInt() // 默认灰色
        }
    }

    // ==================== 填充笔 FloodFill ====================

    /**
     * 执行 FloodFill（洪水填充）算法
     * 从点击位置开始，填充相同颜色的连通区域为当前选择的颜色
     * @param bitmap 目标位图
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param fillColor 填充颜色（ARGB格式）
     * @param tolerance 容差（0-255），相近颜色也算连通
     */
    fun floodFill(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        fillColor: Int,
        tolerance: Int = 32
    ) {
        val width = bitmap.width
        val height = bitmap.height

        if (startX < 0 || startX >= width || startY < 0 || startY >= height) return

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val targetColor = pixels[startY * width + startX]

        // 如果颜色相同，无需填充
        if (targetColor == fillColor) return

        // 性能保护：限制最大填充像素数（位图面积的1/4）
        val maxPixels = (width * height) / 4
        var filledCount = 0

        val stack = ArrayDeque<Int>()
        stack.add(startX)
        stack.add(startY)

        val visited = BooleanArray(width * height)

        while (stack.isNotEmpty() && filledCount < maxPixels) {
            if (stack.size < 2) break
            val y = stack.removeLast()
            val x = stack.removeLast()

            if (x < 0 || x >= width || y < 0 || y >= height) continue
            val index = y * width + x
            if (visited[index]) continue

            val pixel = pixels[index]
            if (!colorMatch(pixel, targetColor, tolerance)) continue

            visited[index] = true
            pixels[index] = fillColor
            filledCount++

            // 添加四连通邻居
            stack.addLast(x + 1)
            stack.addLast(y)
            stack.addLast(x - 1)
            stack.addLast(y)
            stack.addLast(x)
            stack.addLast(y + 1)
            stack.addLast(x)
            stack.addLast(y - 1)
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * 判断两个颜色是否在容差范围内匹配
     */
    private fun colorMatch(color1: Int, color2: Int, tolerance: Int): Boolean {
        val dr = ((color1 shr 16) and 0xFF) - ((color2 shr 16) and 0xFF)
        val dg = ((color1 shr 8) and 0xFF) - ((color2 shr 8) and 0xFF)
        val db = (color1 and 0xFF) - (color2 and 0xFF)
        val da = ((color1 shr 24) and 0xFF) - ((color2 shr 24) and 0xFF)

        return kotlin.math.abs(dr) <= tolerance &&
                kotlin.math.abs(dg) <= tolerance &&
                kotlin.math.abs(db) <= tolerance &&
                kotlin.math.abs(da) <= tolerance
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建基础 Paint 对象
     */
    private fun createBasePaint(strokeData: StrokeData): Paint {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color

        return Paint().apply {
            this.color = color
            alpha = (descriptor.opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeWidth = descriptor.size
        }
    }

    /**
     * 创建带抖动的路径
     */
    private fun createJitterPath(
        points: List<StrokePoint>,
        descriptor: BrushDescriptor
    ): Path {
        val path = Path()
        if (points.size < 2) return path

        val spacing = descriptor.size * descriptor.spacing
        val jitterAmount = descriptor.jitter * descriptor.size * 0.5f
        val random = Random(points.hashCode())

        var lastX = points[0].x
        var lastY = points[0].y
        path.moveTo(lastX, lastY)

        var accumulatedDist = 0f

        for (i in 1 until points.size) {
            val current = points[i]
            var dx = current.x - lastX
            var dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            if (jitterAmount > 0) {
                dx += (random.nextFloat() - 0.5f) * jitterAmount
                dy += (random.nextFloat() - 0.5f) * jitterAmount
            }

            accumulatedDist += dist

            if (accumulatedDist >= spacing || i == points.size - 1) {
                val targetX = lastX + dx
                val targetY = lastY + dy
                path.lineTo(targetX, targetY)
                lastX = targetX
                lastY = targetY
                accumulatedDist = 0f
            }
        }

        return path
    }

    /**
     * 创建带抖动的路径（使用指定的 Random 对象和笔触宽度）
     */
    private fun createJitterPathWithRandom(
        points: List<StrokePoint>,
        spacing: Float,
        jitterAmount: Float,
        random: Random,
        strokeWidth: Float
    ): Path {
        val path = Path()
        if (points.size < 2) return path

        var lastX = points[0].x
        var lastY = points[0].y
        path.moveTo(lastX, lastY)

        var accumulatedDist = 0f

        for (i in 1 until points.size) {
            val current = points[i]
            var dx = current.x - lastX
            var dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            if (jitterAmount > 0) {
                dx += (random.nextFloat() - 0.5f) * jitterAmount
                dy += (random.nextFloat() - 0.5f) * jitterAmount
            }

            accumulatedDist += dist

            if (accumulatedDist >= spacing || i == points.size - 1) {
                val targetX = lastX + dx
                val targetY = lastY + dy
                path.lineTo(targetX, targetY)
                lastX = targetX
                lastY = targetY
                accumulatedDist = 0f
            }
        }

        return path
    }

    /**
     * 创建平滑曲线路径（使用贝塞尔曲线）
     */
    private fun createSmoothPath(points: List<StrokePoint>): Path {
        val path = Path()
        if (points.size < 2) return path

        path.moveTo(points[0].x, points[0].y)

        if (points.size == 2) {
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        for (i in 1 until points.size - 1) {
            val prev = points[i - 1]
            val current = points[i]
            val next = points[i + 1]

            val midX = (prev.x + next.x) / 2
            val midY = (prev.y + next.y) / 2

            path.quadTo(prev.x, prev.y, midX, midY)
        }

        val lastPoint = points.last()
        path.lineTo(lastPoint.x, lastPoint.y)

        return path
    }

    /**
     * 创建带间距的路径（马克笔等用）
     */
    private fun createSpacedPath(points: List<StrokePoint>, spacing: Float): Path {
        val path = Path()
        if (points.size < 2) return path

        path.moveTo(points[0].x, points[0].y)

        var accumulatedDist = 0f
        var lastX = points[0].x
        var lastY = points[0].y

        for (i in 1 until points.size) {
            val current = points[i]
            val dx = current.x - lastX
            val dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            accumulatedDist += dist

            if (accumulatedDist >= spacing || i == points.size - 1) {
                val targetX = lastX + dx
                val targetY = lastY + dy
                path.lineTo(targetX, targetY)
                lastX = targetX
                lastY = targetY
                accumulatedDist = 0f
            }
        }

        return path
    }
}
