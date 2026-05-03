package com.jiafenbu.androidpaint.selection

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Region
import android.os.Build
import androidx.compose.ui.geometry.Offset
import com.jiafenbu.androidpaint.model.Selection
import com.jiafenbu.androidpaint.model.SelectionShape
import com.jiafenbu.androidpaint.model.SelectionType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 选区计算引擎
 * 提供选区的创建、计算和操作功能
 */
object SelectionEngine {
    
    /**
     * 颜色容差（RGB空间）
     * 默认容差值
     */
    const val DEFAULT_TOLERANCE = 32
    
    /**
     * 创建一个矩形选区
     * @param left 左边
     * @param top 顶边
     * @param right 右边
     * @param bottom 底边
     * @param featherRadius 羽化半径
     * @return 选区数据
     */
    fun createRectangleSelection(
        left: Float, top: Float, right: Float, bottom: Float,
        featherRadius: Float = 0f
    ): Selection {
        val bounds = RectF(
            min(left, right),
            min(top, bottom),
            max(left, right),
            max(top, bottom)
        )
        
        return Selection.create(
            type = SelectionType.RECTANGLE,
            shape = SelectionShape.Rectangle(bounds),
            featherRadius = featherRadius
        )
    }
    
    /**
     * 创建一个椭圆选区
     * @param bounds 边界矩形
     * @param featherRadius 羽化半径
     * @return 选区数据
     */
    fun createEllipseSelection(
        bounds: RectF,
        featherRadius: Float = 0f
    ): Selection {
        return Selection.create(
            type = SelectionType.ELLIPSE,
            shape = SelectionShape.Ellipse(bounds),
            featherRadius = featherRadius
        )
    }
    
    /**
     * 创建自由套索选区
     * @param points 路径上的点
     * @param featherRadius 羽化半径
     * @return 选区数据
     */
    fun createLassoSelection(
        points: List<Offset>,
        featherRadius: Float = 0f
    ): Selection {
        val path = Path()
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                path.lineTo(points[i].x, points[i].y)
            }
            path.close()
        }
        
        val bounds = calculatePathBounds(path)
        
        return Selection.create(
            type = SelectionType.LASSO,
            shape = SelectionShape.Lasso(path, bounds),
            featherRadius = featherRadius
        )
    }
    
    /**
     * 创建魔棒选区
     * 使用 BFS（广度优先搜索）算法，从起始点开始扩散
     * 
     * @param sourceBitmap 源位图
     * @param startX 起始 X 坐标
     * @param startY 起始 Y 坐标
     * @param tolerance 容差值（0-255）
     * @param featherRadius 羽化半径
     * @return 选区数据
     */
    fun createMagicWandSelection(
        sourceBitmap: Bitmap,
        startX: Int,
        startY: Int,
        tolerance: Int = DEFAULT_TOLERANCE,
        featherRadius: Float = 0f
    ): Selection? {
        if (startX < 0 || startY < 0 || 
            startX >= sourceBitmap.width || 
            startY >= sourceBitmap.height) {
            return null
        }
        
        // 获取起始颜色
        val startColor = getPixelColor(sourceBitmap, startX, startY)
        
        // 创建掩码位图
        val maskBitmap = Bitmap.createBitmap(
            sourceBitmap.width, 
            sourceBitmap.height, 
            Bitmap.Config.ARGB_8888
        )
        
        // BFS 扩散
        val visited = Array(sourceBitmap.width) { BooleanArray(sourceBitmap.height) }
        val queue = ArrayDeque<Point>()
        queue.add(Point(startX, startY))
        visited[startX][startY] = true
        
        var minX = startX
        var minY = startY
        var maxX = startX
        var maxY = startY
        
        while (queue.isNotEmpty()) {
            val point = queue.removeFirst()
            val x = point.x
            val y = point.y
            
            // 获取当前像素颜色
            if (x < 0 || y < 0 || x >= sourceBitmap.width || y >= sourceBitmap.height) {
                continue
            }
            
            val currentColor = getPixelColor(sourceBitmap, x, y)
            
            // 检查颜色是否在容差范围内
            if (isColorWithinTolerance(startColor, currentColor, tolerance)) {
                // 设置掩码
                maskBitmap.setPixel(x, y, Color.WHITE)
                
                // 更新边界
                minX = min(minX, x)
                minY = min(minY, y)
                maxX = max(maxX, x)
                maxY = max(maxY, y)
                
                // 添加相邻像素到队列
                val neighbors = listOf(
                    Point(x - 1, y),
                    Point(x + 1, y),
                    Point(x, y - 1),
                    Point(x, y + 1)
                )
                
                for (neighbor in neighbors) {
                    if (neighbor.x >= 0 && neighbor.y >= 0 &&
                        neighbor.x < sourceBitmap.width &&
                        neighbor.y < sourceBitmap.height &&
                        !visited[neighbor.x][neighbor.y]) {
                        visited[neighbor.x][neighbor.y] = true
                        queue.add(neighbor)
                    }
                }
            }
        }
        
        // 创建边界矩形
        val bounds = RectF(
            minX.toFloat(),
            minY.toFloat(),
            (maxX + 1).toFloat(),
            (maxY + 1).toFloat()
        )
        
        return Selection.create(
            type = SelectionType.MAGIC_WAND,
            shape = SelectionShape.MagicWand(maskBitmap, bounds),
            featherRadius = featherRadius
        )
    }
    
    /**
     * 获取像素颜色
     */
    private fun getPixelColor(bitmap: Bitmap, x: Int, y: Int): Int {
        val pixels = IntArray(1)
        bitmap.getPixels(pixels, 0, 1, x, y, 1, 1)
        return pixels[0]
    }
    
    /**
     * 判断两个颜色是否在容差范围内
     * 使用 RGB 空间的欧几里得距离
     */
    private fun isColorWithinTolerance(color1: Int, color2: Int, tolerance: Int): Boolean {
        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)
        
        val distance = sqrt(
            (r1 - r2).toFloat().pow(2) +
            (g1 - g2).toFloat().pow(2) +
            (b1 - b2).toFloat().pow(2)
        )
        
        return distance <= tolerance
    }
    
    /**
     * 计算路径边界
     */
    private fun calculatePathBounds(path: Path): RectF {
        val bounds = RectF()
        path.computeBounds(bounds, true)
        return bounds
    }
    
    /**
     * 反选选区
     * 注意：这个操作需要访问画布位图来创建反向选区
     * 
     * @param originalSelection 原始选区
     * @param canvasWidth 画布宽度
     * @param canvasHeight 画布高度
     * @return 反向选区（简化为矩形，因为精确反选计算复杂）
     */
    fun invertSelection(
        originalSelection: Selection,
        canvasWidth: Int,
        canvasHeight: Int
    ): Selection {
        // 精确的反向选区需要复杂的路径运算
        // 这里简化为返回包含整个画布的矩形选区
        return createRectangleSelection(
            0f, 0f,
            canvasWidth.toFloat(), canvasHeight.toFloat()
        )
    }
    
    /**
     * 羽化选区
     * 对选区边缘进行高斯模糊处理，生成 alpha 渐变
     * 
     * @param selection 原始选区
     * @param radius 羽化半径
     * @return 羽化后的选区
     */
    fun featherSelection(
        selection: Selection,
        radius: Float
    ): Selection {
        return selection.setFeather(radius)
    }
    
    /**
     * 扩展选区
     * 
     * @param selection 原始选区
     * @param amount 扩展像素数
     * @return 扩展后的选区
     */
    fun expandSelection(selection: Selection, amount: Float): Selection {
        val bounds = selection.shape.bounds
        val newBounds = RectF(
            bounds.left - amount,
            bounds.top - amount,
            bounds.right + amount,
            bounds.bottom + amount
        )
        
        return when (selection.type) {
            SelectionType.RECTANGLE -> selection.copy(
                shape = SelectionShape.Rectangle(newBounds),
                originalBounds = newBounds
            )
            SelectionType.ELLIPSE -> selection.copy(
                shape = SelectionShape.Ellipse(newBounds),
                originalBounds = newBounds
            )
            else -> selection
        }
    }
    
    /**
     * 收缩选区
     * 
     * @param selection 原始选区
     * @param amount 收缩像素数
     * @return 收缩后的选区
     */
    fun shrinkSelection(selection: Selection, amount: Float): Selection {
        return expandSelection(selection, -amount)
    }
    
    /**
     * 移动选区
     * 
     * @param selection 原始选区
     * @param deltaX X 方向偏移
     * @param deltaY Y 方向偏移
     * @return 移动后的选区
     */
    fun moveSelection(
        selection: Selection,
        deltaX: Float,
        deltaY: Float
    ): Selection {
        val newBounds = RectF(selection.originalBounds)
        newBounds.offset(deltaX, deltaY)
        
        val newMatrix = android.graphics.Matrix()
        newMatrix.postTranslate(deltaX, deltaY)
        
        return selection.copy(
            shape = when (selection.shape) {
                is SelectionShape.Rectangle -> SelectionShape.Rectangle(newBounds)
                is SelectionShape.Ellipse -> SelectionShape.Ellipse(newBounds)
                is SelectionShape.Lasso -> {
                    val lasso = selection.shape as SelectionShape.Lasso
                    val newPath = Path(lasso.path)
                    val matrix = android.graphics.Matrix()
                    matrix.postTranslate(deltaX, deltaY)
                    newPath.transform(matrix)
                    SelectionShape.Lasso(newPath, newBounds)
                }
                is SelectionShape.MagicWand -> {
                    val magic = selection.shape as SelectionShape.MagicWand
                    SelectionShape.MagicWand(magic.maskBitmap, newBounds)
                }
            },
            originalBounds = newBounds,
            transformMatrix = newMatrix
        )
    }
    
    /**
     * 应用羽化效果到位图掩码
     * 使用简单的高斯模糊近似算法
     * 
     * @param maskBitmap 掩码位图
     * @param radius 羽化半径
     * @return 应用羽化后的掩码位图
     */
    fun applyFeatherToMask(maskBitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0) return maskBitmap
        
        val width = maskBitmap.width
        val height = maskBitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 简单的模糊处理（箱式模糊近似）
        val kernelSize = (radius * 2 + 1).toInt().coerceIn(1, 15)
        val halfKernel = kernelSize / 2
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                
                for (ky in -halfKernel..halfKernel) {
                    for (kx in -halfKernel..halfKernel) {
                        val sx = (x + kx).coerceIn(0, width - 1)
                        val sy = (y + ky).coerceIn(0, height - 1)
                        
                        val pixel = maskBitmap.getPixel(sx, sy)
                        val alpha = (pixel shr 24) and 0xFF
                        
                        // 高斯权重（简化）
                        val weight = if (abs(kx) + abs(ky) <= kernelSize) 1 else 0
                        sum += alpha * weight
                        count += weight
                    }
                }
                
                val avgAlpha = if (count > 0) sum / count else 0
                val newAlpha = avgAlpha.coerceIn(0, 255)
                result.setPixel(x, y, (newAlpha shl 24) or 0x00FFFFFF)
            }
        }
        
        return result
    }
    
    /**
     * 从选区创建路径
     * 用于绘制选区轮廓
     * 
     * @param selection 选区
     * @return 路径
     */
    fun selectionToPath(selection: Selection): Path {
        return when (val shape = selection.shape) {
            is SelectionShape.Rectangle -> {
                Path().apply {
                    addRect(shape.bounds, Path.Direction.CW)
                }
            }
            is SelectionShape.Ellipse -> {
                Path().apply {
                    addOval(shape.bounds, Path.Direction.CW)
                }
            }
            is SelectionShape.Lasso -> {
                Path(shape.path)
            }
            is SelectionShape.MagicWand -> {
                // 魔棒选区转换为路径需要额外处理
                // 简化处理：返回边界矩形
                Path().apply {
                    addRect(shape.bounds, Path.Direction.CW)
                }
            }
        }
    }
    
    /**
     * 获取选区边缘点列表
     * 用于描边等功能
     * 
     * @param selection 选区
     * @param step 采样步长
     * @return 边缘点列表
     */
    fun getSelectionEdgePoints(selection: Selection, step: Float = 2f): List<Offset> {
        val path = selectionToPath(selection)
        val points = mutableListOf<Offset>()
        
        val pm = PathMeasure(path, false)
        var length = pm.length
        var distance = 0f
        
        while (distance < length) {
            val point = FloatArray(2)
            pm.getPosTan(distance, point, null)
            points.add(Offset(point[0], point[1]))
            distance += step
        }
        
        return points
    }
}
