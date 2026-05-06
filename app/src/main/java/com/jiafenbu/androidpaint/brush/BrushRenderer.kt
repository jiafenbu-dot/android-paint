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
            // 新增笔刷渲染
            BrushType.CALLIGRAPHY -> renderCalligraphyStroke(canvas, points, strokeData)
            BrushType.AIRBRUSH -> renderAirbrushStroke(canvas, points, strokeData)
            BrushType.PIXEL -> renderPixelStroke(canvas, points, strokeData)
            BrushType.NEON -> renderNeonStroke(canvas, points, strokeData)
            BrushType.PATTERN_BRUSH -> renderPatternStroke(canvas, points, strokeData)
            BrushType.HAIR -> renderHairStroke(canvas, points, strokeData)
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

    // ==================== 新增笔刷渲染方法 ====================

    /**
     * 渲染书法笔画
     * 特点：变宽线条，根据笔触方向改变粗细
     * 斜角笔头效果，模拟毛笔/平头笔的压感效果
     * 关键实现：根据路径方向计算当前宽度，垂直于运动方向时宽度最大
     */
    private fun renderCalligraphyStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val opacity = descriptor.opacity

        if (points.size < 2) return

        val random = Random(points.hashCode())

        // 基础 Paint（宽线条作为笔画主体）
        val basePaint = Paint().apply {
            this.color = color
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.SQUARE // 方形笔触模拟书法笔
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeMiter = 1f
        }

        var lastX = points[0].x
        var lastY = points[0].y

        for (i in 1 until points.size) {
            val current = points[i]
            var dx = current.x - lastX
            var dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            // 计算运动方向角度（用于计算压感）
            val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())

            // 书法笔效果：根据角度调整宽度
            // 模拟斜角笔头，运动方向与笔头角度不同时宽度变化
            val pressureFactor = kotlin.math.abs(kotlin.math.cos(angle * 2))
            val width = size * (0.4f + pressureFactor * 0.6f)

            // 添加轻微抖动
            var jitterX = dx
            var jitterY = dy
            if (descriptor.jitter > 0) {
                jitterX += (random.nextFloat() - 0.5f) * descriptor.jitter * size * 0.3f
                jitterY += (random.nextFloat() - 0.5f) * descriptor.jitter * size * 0.3f
            }

            val targetX = lastX + jitterX
            val targetY = lastY + jitterY

            // 绘制变宽的线段
            basePaint.strokeWidth = width
            canvas.drawLine(lastX, lastY, targetX, targetY, basePaint)

            lastX = targetX
            lastY = targetY
        }
    }

    /**
     * 渲染气笔（柔喷笔）笔画
     * 特点：比喷枪更柔和的喷雾效果，中心浓边缘淡的高斯分布
     * 适合做渐变和柔和阴影
     * 关键实现：每个点绘制多个透明度递减的圆形点，形成柔和的喷雾效果
     */
    private fun renderAirbrushStroke(
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

        // 计算气笔密度（比喷枪更密集）
        val density = (size * 0.8f).toInt().coerceIn(5, 300)

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
            val steps = (dist / spacing.coerceAtLeast(1f)).toInt().coerceIn(1, 30)
            for (s in 0..steps) {
                val t = if (steps == 0) 0f else s.toFloat() / steps
                val px = lastX + (targetX - lastX) * t
                val py = lastY + (targetY - lastY) * t

                // 气笔效果：多层透明度递减的圆点（高斯分布）
                // 中心最浓，边缘最淡
                for (layer in 0 until 4) {
                    val layerRatio = layer / 4f
                    val layerSize = size * (1f - layerRatio * 0.7f) // 外层更大但更淡
                    val layerAlpha = opacity * (1f - layerRatio * 0.8f) * 0.6f

                    val paint = Paint().apply {
                        this.color = color
                        alpha = (layerAlpha * 255).toInt().coerceIn(0, 255)
                        isAntiAlias = true
                        // 添加轻微模糊效果使边缘更柔和
                        maskFilter = BlurMaskFilter(layerSize * 0.3f, BlurMaskFilter.Blur.NORMAL)
                    }

                    // 在该点周围散布随机点
                    for (dotIndex in 0 until (density / 4)) {
                        val angle = random.nextFloat() * 2f * Math.PI.toFloat()
                        val radius = random.nextFloat() * layerSize * 0.5f * (1f + layerRatio * 0.5f)
                        val dotX = px + kotlin.math.cos(angle) * radius
                        val dotY = py + kotlin.math.sin(angle) * radius
                        val dotSize = random.nextFloat() * layerSize * 0.15f + 0.5f
                        canvas.drawCircle(dotX, dotY, dotSize, paint)
                    }
                }
            }

            lastX = targetX
            lastY = targetY
        }
    }

    /**
     * 渲染像素笔画
     * 特点：硬边方形笔触，像素画工具
     * 不做抗锯齿，每个"点"是方块而不是圆形
     * 关键实现：使用矩形绘制，禁用抗锯齿
     */
    private fun renderPixelStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val opacity = descriptor.opacity

        val spacing = size * descriptor.spacing
        // 像素笔的抖动通常设为0，保持硬边像素效果
        val random = Random(points.hashCode())

        // 像素笔 Paint：方形笔触，无抗锯齿
        val paint = Paint().apply {
            this.color = color
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
            isAntiAlias = false // 关键：禁用抗锯齿
        }

        var lastX = points[0].x
        var lastY = points[0].y

        for (i in 1 until points.size) {
            val current = points[i]
            var dx = current.x - lastX
            var dy = current.y - lastY
            val dist = sqrt(dx * dx + dy * dy)

            if (dist < 0.01f) continue

            // 添加轻微抖动（可选，像素笔通常抖动较小）
            if (descriptor.jitter > 0) {
                dx += (random.nextFloat() - 0.5f) * descriptor.jitter * size * 0.2f
                dy += (random.nextFloat() - 0.5f) * descriptor.jitter * size * 0.2f
            }

            val targetX = lastX + dx
            val targetY = lastY + dy

            // 计算沿路径需要放置多少个像素点
            val pixelCount = (dist / spacing).toInt().coerceIn(1, 50)
            for (p in 0 until pixelCount) {
                val t = if (pixelCount == 1) 0.5f else p.toFloat() / pixelCount
                val px = lastX + (targetX - lastX) * t
                val py = lastY + (targetY - lastY) * t

                // 绘制方形像素（矩形，中心对齐）
                val halfSize = size / 2f
                // 确保像素对齐到网格（像素风格需要整数坐标或指定网格）
                val gridX = kotlin.math.floor(px).toFloat()
                val gridY = kotlin.math.floor(py).toFloat()
                canvas.drawRect(
                    gridX - halfSize,
                    gridY - halfSize,
                    gridX + halfSize,
                    gridY + halfSize,
                    paint
                )
            }

            lastX = targetX
            lastY = targetY
        }
    }

    /**
     * 渲染霓虹笔画
     * 特点：发光效果，中间亮外圈有光晕
     * 用 BlurMaskFilter 创建外发光
     * 适合在深色背景上使用
     * 关键实现：多层叠加，外层模糊光晕 + 内层高亮核心
     */
    private fun renderNeonStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val opacity = descriptor.opacity

        if (points.size < 2) return

        val spacing = size * descriptor.spacing

        // 霓虹效果Paint配置
        val paint = Paint().apply {
            this.color = color
            this.alpha = (opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
            strokeMiter = 1f
        }

        // 创建路径
        val path = createSmoothPath(points)

        // 外层：最模糊的光晕（霓虹扩散效果）
        val outerPaint = Paint(paint).apply {
            strokeWidth = size * 3f
            alpha = ((opacity * 0.15f) * 255).toInt().coerceIn(0, 255)
            maskFilter = BlurMaskFilter(size * 2f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, outerPaint)

        // 中层：中度模糊的光晕
        val midPaint = Paint(paint).apply {
            strokeWidth = size * 1.8f
            alpha = ((opacity * 0.3f) * 255).toInt().coerceIn(0, 255)
            maskFilter = BlurMaskFilter(size * 0.8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, midPaint)

        // 内层：较清晰的内部光晕
        val innerPaint = Paint(paint).apply {
            strokeWidth = size * 1.2f
            alpha = ((opacity * 0.6f) * 255).toInt().coerceIn(0, 255)
            maskFilter = BlurMaskFilter(size * 0.2f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, innerPaint)

        // 核心层：最亮最清晰
        val corePaint = Paint(paint).apply {
            strokeWidth = size * 0.6f
            alpha = ((opacity * 0.95f) * 255).toInt().coerceIn(0, 255)
            // 核心可以有轻微模糊使其更柔和
            maskFilter = BlurMaskFilter(size * 0.1f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(path, corePaint)
    }

    /**
     * 渲染图案笔画
     * 特点：沿路径绘制重复图案（圆点/星星/心形等）
     * 按间距排列，适合做装饰边框
     * 关键实现：沿路径按固定间距放置图案，使用 PathMeasure 计算位置
     */
    private fun renderPatternStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val opacity = descriptor.opacity

        if (points.size < 2) return

        val random = Random(points.hashCode())
        val spacing = size * descriptor.spacing
        val patternSize = size * 0.4f

        val paint = Paint().apply {
            this.color = color
            alpha = (opacity * 255).toInt().coerceIn(0, 255)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 创建路径
        val path = createSmoothPath(points)

        // 使用 PathMeasure 获取路径长度
        val pathMeasure = android.graphics.PathMeasure(path, false)
        val pathLength = pathMeasure.length

        if (pathLength <= 0) return

        // 沿路径按间距放置图案
        var distance = 0f
        while (distance < pathLength) {
            val position = FloatArray(2)
            val tangent = FloatArray(2)
            pathMeasure.getPosTan(distance, position, tangent)

            val px = position[0]
            val py = position[1]

            // 计算图案类型（使用随机种子使图案一致）
            val patternType = (distance / spacing).toInt() % 4

            // 添加轻微抖动
            val jitterX = if (descriptor.jitter > 0) {
                (random.nextFloat() - 0.5f) * descriptor.jitter * size * 0.3f
            } else 0f
            val jitterY = if (descriptor.jitter > 0) {
                (random.nextFloat() - 0.5f) * descriptor.jitter * size * 0.3f
            } else 0f

            // 根据 patternType 绘制不同图案
            when (patternType) {
                0 -> {
                    // 圆点
                    canvas.drawCircle(px + jitterX, py + jitterY, patternSize, paint)
                }
                1 -> {
                    // 方形
                    val halfSize = patternSize
                    canvas.drawRect(
                        px - halfSize + jitterX,
                        py - halfSize + jitterY,
                        px + halfSize + jitterX,
                        py + halfSize + jitterY,
                        paint
                    )
                }
                2 -> {
                    // 菱形（旋转45度的方形）
                    val halfSize = patternSize * 0.7f
                    val path2 = Path()
                    path2.moveTo(px + jitterX, py - halfSize + jitterY)
                    path2.lineTo(px + halfSize + jitterX, py + jitterY)
                    path2.lineTo(px + jitterX, py + halfSize + jitterY)
                    path2.lineTo(px - halfSize + jitterX, py + jitterY)
                    path2.close()
                    canvas.drawPath(path2, paint)
                }
                3 -> {
                    // 三角形
                    val halfSize = patternSize * 0.8f
                    val path2 = Path()
                    path2.moveTo(px + jitterX, py - halfSize + jitterY)
                    path2.lineTo(px + halfSize + jitterX, py + halfSize * 0.7f + jitterY)
                    path2.lineTo(px - halfSize + jitterX, py + halfSize * 0.7f + jitterY)
                    path2.close()
                    canvas.drawPath(path2, paint)
                }
            }

            distance += spacing
        }
    }

    /**
     * 渲染毛发笔画
     * 特点：多条细线组成一笔，模拟毛笔/排笔效果
     * 线条有轻微随机偏移和粗细变化
     * 关键实现：绘制多条并行的细线，每条有不同的抖动和宽度
     */
    private fun renderHairStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        strokeData: StrokeData
    ) {
        val descriptor = strokeData.brushDescriptor
        val color = strokeData.color
        val size = descriptor.size
        val opacity = descriptor.opacity

        if (points.size < 2) return

        val random = Random(points.hashCode())
        val spacing = size * descriptor.spacing
        val jitterAmount = descriptor.jitter * size * 0.4f

        // 毛发数量（模拟多根笔毛）
        val hairCount = 5

        for (hairIndex in 0 until hairCount) {
            // 每根毛发的独立随机种子
            val hairRandom = Random(points.hashCode() + hairIndex * 1000)

            // 计算毛发偏移（垂直于运动方向）
            val hairOffsetRange = size * 0.3f
            val hairOffset = (hairRandom.nextFloat() - 0.5f) * hairOffsetRange * 2

            // 每根毛发的宽度变化
            val hairWidth = size * (0.15f + hairRandom.nextFloat() * 0.15f)

            // 每根毛发的透明度微调
            val hairOpacity = opacity * (0.7f + hairRandom.nextFloat() * 0.3f)

            val paint = Paint().apply {
                this.color = color
                alpha = (hairOpacity * 255).toInt().coerceIn(0, 255)
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
                strokeMiter = 1f
                this.strokeWidth = hairWidth
            }

            val path = Path()
            path.moveTo(points[0].x, points[0].y)

            var accumulatedDist = 0f
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
                    dx += (hairRandom.nextFloat() - 0.5f) * jitterAmount
                    dy += (hairRandom.nextFloat() - 0.5f) * jitterAmount
                }

                // 添加毛发特有的横向偏移
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    // 垂直于运动方向的偏移
                    dx += -dy / len * hairOffset
                    dy += dx / len * hairOffset
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

            canvas.drawPath(path, paint)
        }
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
