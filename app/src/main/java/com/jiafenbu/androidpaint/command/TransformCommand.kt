package com.jiafenbu.androidpaint.command

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.jiafenbu.androidpaint.model.Selection
import com.jiafenbu.androidpaint.model.SelectionShape
import com.jiafenbu.androidpaint.model.TransformMode
import com.jiafenbu.androidpaint.model.TransformTarget
import com.jiafenbu.androidpaint.selection.TransformEngine
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 变形命令基类
 * 所有变形操作都继承此类
 */
abstract class TransformCommand(
    protected val target: TransformTarget,
    protected val selection: Selection?
) : DrawCommand {
    
    /** 备份的图层数据 */
    protected var backupData: MutableMap<Int, BitmapBackup>? = null
    
    /**
     * 位图备份数据
     */
    protected data class BitmapBackup(
        val pixels: IntArray,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is BitmapBackup) return false
            return x == other.x && y == other.y && 
                   width == other.width && height == other.height &&
                   pixels.contentEquals(other.pixels)
        }
        
        override fun hashCode(): Int {
            var result = pixels.contentHashCode()
            result = 31 * result + x
            result = 31 * result + y
            result = 31 * result + width
            result = 31 * result + height
            return result
        }
    }
    
    /**
     * 从图层备份像素
     */
    protected fun backupLayerPixels(
        layers: List<Pair<Int, Bitmap>>,
        bounds: RectF
    ) {
        backupData = mutableMapOf()
        
        val left = bounds.left.toInt().coerceAtLeast(0)
        val top = bounds.top.toInt().coerceAtLeast(0)
        val width = bounds.width().toInt().coerceIn(1, 4096)
        val height = bounds.height().toInt().coerceIn(1, 4096)
        
        for ((index, bitmap) in layers) {
            val actualWidth = minOf(width, bitmap.width - left)
            val actualHeight = minOf(height, bitmap.height - top)
            
            if (actualWidth > 0 && actualHeight > 0) {
                val pixels = IntArray(actualWidth * actualHeight)
                bitmap.getPixels(pixels, 0, actualWidth, left, top, actualWidth, actualHeight)
                backupData!![index] = BitmapBackup(pixels, left, top, actualWidth, actualHeight)
            }
        }
    }
    
    /**
     * 恢复图层像素
     */
    protected fun restoreLayerPixels(layers: List<Pair<Int, Bitmap>>) {
        backupData?.forEach { (index, backup) ->
            val bitmap = layers.find { it.first == index }?.second ?: return@forEach
            bitmap.setPixels(backup.pixels, 0, backup.width, backup.x, backup.y, backup.width, backup.height)
        }
    }
    
    /**
     * 创建选区掩码
     */
    protected fun createSelectionMask(selection: Selection, width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        
        when (val shape = selection.shape) {
            is SelectionShape.Rectangle -> canvas.drawRect(shape.bounds, paint)
            is SelectionShape.Ellipse -> canvas.drawOval(shape.bounds, paint)
            is SelectionShape.Lasso -> canvas.drawPath(shape.path, paint)
            is SelectionShape.MagicWand -> canvas.drawBitmap(shape.maskBitmap, 0f, 0f, paint)
        }
        
        return mask
    }
}

/**
 * 自由缩放命令
 * 
 * @param layerBitmap 图层位图
 * @param selection 选区（可选）
 * @param scaleX X 方向缩放
 * @param scaleY Y 方向缩放
 * @param pivotX 缩放中心 X
 * @param pivotY 缩放中心 Y
 */
class FreeScaleCommand(
    private val layerBitmap: Bitmap,
    private val selection: Selection?,
    private val scaleX: Float,
    private val scaleY: Float,
    private val pivotX: Float,
    private val pivotY: Float
) : TransformCommand(TransformTarget.CURRENT_LAYER, selection) {
    
    private var backupPixels: IntArray? = null
    private var scaledBitmap: Bitmap? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        val bounds = selection?.getCurrentBounds() ?: RectF(
            0f, 0f, layerBitmap.width.toFloat(), layerBitmap.height.toFloat()
        )
        
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        // 备份原始像素
        backupPixels = IntArray(width * height)
        layerBitmap.getPixels(backupPixels!!, 0, width, left, top, width, height)
        
        // 提取选区内容
        val contentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mask = selection?.let { createSelectionMask(it, width, height) }
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (mask != null) {
                    val maskAlpha = (mask.getPixel(x, y) shr 24) and 0xFF
                    if (maskAlpha > 0) {
                        contentBitmap.setPixel(x, y, backupPixels!![y * width + x])
                    }
                } else {
                    contentBitmap.setPixel(x, y, backupPixels!![y * width + x])
                }
            }
        }
        
        // 计算新尺寸
        val newWidth = (width * abs(scaleX)).toInt().coerceIn(1, layerBitmap.width)
        val newHeight = (height * abs(scaleY)).toInt().coerceIn(1, layerBitmap.height)
        
        // 缩放
        scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap!!)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        
        canvas.save()
        canvas.translate(newWidth / 2f, newHeight / 2f)
        canvas.scale(scaleX, scaleY)
        canvas.translate(-width / 2f, -height / 2f)
        canvas.drawBitmap(contentBitmap, 0f, 0f, paint)
        canvas.restore()
        
        // 清除原始区域
        for (y in 0 until height) {
            for (x in 0 until width) {
                layerBitmap.setPixel(left + x, top + y, Color.TRANSPARENT)
            }
        }
        
        // 绘制缩放后的内容
        val destLeft = (pivotX - newWidth / 2f).toInt().coerceIn(0, layerBitmap.width - 1)
        val destTop = (pivotY - newHeight / 2f).toInt().coerceIn(0, layerBitmap.height - 1)
        
        scaledBitmap?.let { scaled ->
            for (y in 0 until minOf(scaled.height, layerBitmap.height - destTop)) {
                for (x in 0 until minOf(scaled.width, layerBitmap.width - destLeft)) {
                    val pixel = scaled.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xFF
                    if (alpha > 0) {
                        layerBitmap.setPixel(destLeft + x, destTop + y, pixel)
                    }
                }
            }
        }
        
        contentBitmap.recycle()
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || backupPixels == null) return
        
        val bounds = selection?.getCurrentBounds() ?: RectF(
            0f, 0f, layerBitmap.width.toFloat(), layerBitmap.height.toFloat()
        )
        
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        layerBitmap.setPixels(backupPixels!!, 0, width, left, top, width, height)
        
        scaledBitmap?.recycle()
        scaledBitmap = null
        isExecuted = false
    }
}

/**
 * 自由旋转命令
 * 
 * @param layerBitmap 图层位图
 * @param selection 选区（可选）
 * @param angle 旋转角度（弧度）
 * @param pivotX 旋转中心 X
 * @param pivotY 旋转中心 Y
 */
class FreeRotateCommand(
    private val layerBitmap: Bitmap,
    private val selection: Selection?,
    private val angle: Float,
    private val pivotX: Float,
    private val pivotY: Float
) : TransformCommand(TransformTarget.CURRENT_LAYER, selection) {
    
    private var backupPixels: IntArray? = null
    private var rotatedBitmap: Bitmap? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        val bounds = selection?.getCurrentBounds() ?: RectF(
            0f, 0f, layerBitmap.width.toFloat(), layerBitmap.height.toFloat()
        )
        
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        // 备份原始像素
        backupPixels = IntArray(width * height)
        layerBitmap.getPixels(backupPixels!!, 0, width, left, top, width, height)
        
        // 提取选区内容
        val contentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                contentBitmap.setPixel(x, y, backupPixels!![y * width + x])
            }
        }
        
        // 计算旋转后的尺寸
        val cosA = abs(cos(angle.toDouble())).toFloat()
        val sinA = abs(sin(angle.toDouble())).toFloat()
        val newWidth = (width * cosA + height * sinA).toInt().coerceIn(1, layerBitmap.width)
        val newHeight = (width * sinA + height * cosA).toInt().coerceIn(1, layerBitmap.height)
        
        // 旋转
        rotatedBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rotatedBitmap!!)
        
        canvas.save()
        canvas.translate(newWidth / 2f, newHeight / 2f)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())
        canvas.translate(-width / 2f, -height / 2f)
        canvas.drawBitmap(contentBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.restore()
        
        // 清除原始区域
        for (y in 0 until height) {
            for (x in 0 until width) {
                layerBitmap.setPixel(left + x, top + y, Color.TRANSPARENT)
            }
        }
        
        // 绘制旋转后的内容
        val destLeft = (pivotX - newWidth / 2f).toInt().coerceIn(0, layerBitmap.width - 1)
        val destTop = (pivotY - newHeight / 2f).toInt().coerceIn(0, layerBitmap.height - 1)
        
        rotatedBitmap?.let { rotated ->
            for (y in 0 until minOf(rotated.height, layerBitmap.height - destTop)) {
                for (x in 0 until minOf(rotated.width, layerBitmap.width - destLeft)) {
                    val pixel = rotated.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xFF
                    if (alpha > 0) {
                        layerBitmap.setPixel(destLeft + x, destTop + y, pixel)
                    }
                }
            }
        }
        
        contentBitmap.recycle()
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || backupPixels == null) return
        
        val bounds = selection?.getCurrentBounds() ?: RectF(
            0f, 0f, layerBitmap.width.toFloat(), layerBitmap.height.toFloat()
        )
        
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        layerBitmap.setPixels(backupPixels!!, 0, width, left, top, width, height)
        
        rotatedBitmap?.recycle()
        rotatedBitmap = null
        isExecuted = false
    }
}

/**
 * 翻转命令
 * 
 * @param layerBitmap 图层位图
 * @param selection 选区（可选）
 * @param horizontal 是否水平翻转
 */
class FlipCommand(
    private val layerBitmap: Bitmap,
    private val selection: Selection?,
    private val horizontal: Boolean
) : TransformCommand(TransformTarget.CURRENT_LAYER, selection) {
    
    private var backupPixels: IntArray? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        val bounds = selection?.getCurrentBounds() ?: RectF(
            0f, 0f, layerBitmap.width.toFloat(), layerBitmap.height.toFloat()
        )
        
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        // 备份原始像素
        backupPixels = IntArray(width * height)
        layerBitmap.getPixels(backupPixels!!, 0, width, left, top, width, height)
        
        // 翻转
        if (horizontal) {
            // 水平翻转
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val srcX = left + x
                    val dstX = left + (width - 1 - x)
                    layerBitmap.setPixel(dstX, top + y, backupPixels!![y * width + x])
                }
            }
        } else {
            // 垂直翻转
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val srcY = top + y
                    val dstY = top + (height - 1 - y)
                    layerBitmap.setPixel(left + x, dstY, backupPixels!![y * width + x])
                }
            }
        }
        
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || backupPixels == null) return
        
        val bounds = selection?.getCurrentBounds() ?: RectF(
            0f, 0f, layerBitmap.width.toFloat(), layerBitmap.height.toFloat()
        )
        
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        layerBitmap.setPixels(backupPixels!!, 0, width, left, top, width, height)
        isExecuted = false
    }
}

/**
 * 透视变形命令
 * 
 * @param layerBitmap 图层位图
 * @param selection 选区
 * @param corners 四个角的新位置
 */
class PerspectiveTransformCommand(
    private val layerBitmap: Bitmap,
    private val selection: Selection,
    private val corners: TransformEngine.PerspectiveCorners
) : TransformCommand(TransformTarget.CURRENT_LAYER, selection) {
    
    private var backupPixels: IntArray? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        val bounds = selection.getCurrentBounds()
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        // 备份原始像素
        backupPixels = IntArray(width * height)
        layerBitmap.getPixels(backupPixels!!, 0, width, left, top, width, height)
        
        // 提取选区内容
        val contentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mask = createSelectionMask(selection, width, height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskAlpha = (mask.getPixel(x, y) shr 24) and 0xFF
                if (maskAlpha > 0) {
                    contentBitmap.setPixel(x, y, backupPixels!![y * width + x])
                }
            }
        }
        
        // 计算目标尺寸
        val targetWidth = (maxOf(
            sqrt((corners.topRight.x - corners.topLeft.x).toDouble().pow(2) + 
                 (corners.topRight.y - corners.topLeft.y).toDouble().pow(2)),
            sqrt((corners.bottomRight.x - corners.bottomLeft.x).toDouble().pow(2) + 
                 (corners.bottomRight.y - corners.bottomLeft.y).toDouble().pow(2))
        ) + 1).toInt().coerceIn(1, layerBitmap.width)
        
        val targetHeight = (maxOf(
            sqrt((corners.bottomLeft.x - corners.topLeft.x).toDouble().pow(2) + 
                 (corners.bottomLeft.y - corners.topLeft.y).toDouble().pow(2)),
            sqrt((corners.bottomRight.x - corners.topRight.x).toDouble().pow(2) + 
                 (corners.bottomRight.y - corners.topRight.y).toDouble().pow(2))
        ) + 1).toInt().coerceIn(1, layerBitmap.height)
        
        // 创建透视变换后的位图
        val transformedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        
        // 计算透视变换矩阵
        val srcPoints = floatArrayOf(
            0f, 0f,
            width.toFloat(), 0f,
            width.toFloat(), height.toFloat(),
            0f, height.toFloat()
        )
        
        val dstPoints = floatArrayOf(
            corners.topLeft.x - left, corners.topLeft.y - top,
            corners.topRight.x - left, corners.topRight.y - top,
            corners.bottomRight.x - left, corners.bottomRight.y - top,
            corners.bottomLeft.x - left, corners.bottomLeft.y - top
        )
        
        val matrix = Matrix()
        matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)
        
        // 应用变换
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val srcPoints2 = floatArrayOf(x.toFloat(), y.toFloat())
                val inverseMatrix = Matrix(matrix)
                inverseMatrix.invert(inverseMatrix)
                inverseMatrix.mapPoints(srcPoints2)
                
                val sx = srcPoints2[0].toInt()
                val sy = srcPoints2[1].toInt()
                
                if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                    val pixel = contentBitmap.getPixel(sx, sy)
                    val alpha = (pixel shr 24) and 0xFF
                    if (alpha > 0) {
                        transformedBitmap.setPixel(x, y, pixel)
                    }
                }
            }
        }
        
        // 清除原始区域
        for (y in 0 until height) {
            for (x in 0 until width) {
                layerBitmap.setPixel(left + x, top + y, Color.TRANSPARENT)
            }
        }
        
        // 绘制变换后的内容
        val destLeft = corners.topLeft.x.toInt().coerceIn(0, layerBitmap.width - 1)
        val destTop = corners.topLeft.y.toInt().coerceIn(0, layerBitmap.height - 1)
        
        for (y in 0 until minOf(transformedBitmap.height, layerBitmap.height - destTop)) {
            for (x in 0 until minOf(transformedBitmap.width, layerBitmap.width - destLeft)) {
                val pixel = transformedBitmap.getPixel(x, y)
                val alpha = (pixel shr 24) and 0xFF
                if (alpha > 0) {
                    layerBitmap.setPixel(destLeft + x, destTop + y, pixel)
                }
            }
        }
        
        contentBitmap.recycle()
        transformedBitmap.recycle()
        mask.recycle()
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || backupPixels == null) return
        
        val bounds = selection.getCurrentBounds()
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        layerBitmap.setPixels(backupPixels!!, 0, width, left, top, width, height)
        isExecuted = false
    }
}
