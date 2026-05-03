package com.jiafenbu.androidpaint.selection

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Bitmap.createBitmap
import com.jiafenbu.androidpaint.model.Selection
import com.jiafenbu.androidpaint.model.SelectionShape
import com.jiafenbu.androidpaint.model.TransformMode
import com.jiafenbu.androidpaint.model.TransformTarget
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 变形计算引擎
 * 提供缩放、旋转、翻转、透视等变形功能
 */
object TransformEngine {
    
    /**
     * 变形结果
     * 
     * @param bitmap 变形后的位图
     * @param matrix 应用的变换矩阵
     * @param sourceBounds 源边界
     * @param targetBounds 目标边界
     */
    data class TransformResult(
        val bitmap: Bitmap?,
        val matrix: Matrix,
        val sourceBounds: RectF,
        val targetBounds: RectF
    )
    
    /**
     * 透视变换的四个角点
     * 
     * @param topLeft 左上角
     * @param topRight 右上角
     * @param bottomRight 右下角
     * @param bottomLeft 左下角
     */
    data class PerspectiveCorners(
        val topLeft: PointF,
        val topRight: PointF,
        val bottomRight: PointF,
        val bottomLeft: PointF
    ) {
        companion object {
            /**
             * 从矩形创建透视角点
             */
            fun fromRect(rect: RectF): PerspectiveCorners {
                return PerspectiveCorners(
                    topLeft = PointF(rect.left, rect.top),
                    topRight = PointF(rect.right, rect.top),
                    bottomRight = PointF(rect.right, rect.bottom),
                    bottomLeft = PointF(rect.left, rect.bottom)
                )
            }
        }
        
        /**
         * 转换为数组
         */
        fun toFloatArray(): FloatArray {
            return floatArrayOf(
                topLeft.x, topLeft.y,
                topRight.x, topRight.y,
                bottomRight.x, bottomRight.y,
                bottomLeft.x, bottomLeft.y
            )
        }
    }
    
    /**
     * 缩放选区内容
     * 
     * @param sourceBitmap 源位图
     * @param selection 选区
     * @param scaleX X 方向缩放
     * @param scaleY Y 方向缩放
     * @param pivotX 缩放中心 X
     * @param pivotY 缩放中心 Y
     * @return 缩放结果
     */
    fun scaleSelection(
        sourceBitmap: Bitmap,
        selection: Selection,
        scaleX: Float,
        scaleY: Float,
        pivotX: Float = 0f,
        pivotY: Float = 0f
    ): TransformResult {
        val bounds = selection.getCurrentBounds()
        val width = bounds.width()
        val height = bounds.height()
        
        // 计算新尺寸
        val newWidth = (width * abs(scaleX)).toInt()
        val newHeight = (height * abs(scaleY)).toInt()
        
        if (newWidth <= 0 || newHeight <= 0) {
            return TransformResult(null, Matrix(), bounds, bounds)
        }
        
        // 提取选区内容
        val contentBitmap = extractSelectionContent(sourceBitmap, selection)
        
        // 创建缩放后的位图
        val scaledBitmap = createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        
        val matrix = Matrix()
        matrix.postScale(scaleX, scaleY)
        
        canvas.drawBitmap(contentBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
        
        // 计算新的变换矩阵
        val newMatrix = Matrix()
        newMatrix.postTranslate(-bounds.left, -bounds.top)
        newMatrix.postScale(scaleX, scaleY)
        newMatrix.postTranslate(bounds.left, bounds.top)
        
        return TransformResult(
            bitmap = scaledBitmap,
            matrix = newMatrix,
            sourceBounds = bounds,
            targetBounds = RectF(bounds.left, bounds.top, bounds.left + newWidth, bounds.top + newHeight)
        )
    }
    
    /**
     * 旋转选区内容
     * 
     * @param sourceBitmap 源位图
     * @param selection 选区
     * @param angle 旋转角度（弧度）
     * @param pivotX 旋转中心 X
     * @param pivotY 旋转中心 Y
     * @return 旋转结果
     */
    fun rotateSelection(
        sourceBitmap: Bitmap,
        selection: Selection,
        angle: Float,
        pivotX: Float = 0f,
        pivotY: Float = 0f
    ): TransformResult {
        val bounds = selection.getCurrentBounds()
        val cx = if (pivotX == 0f) bounds.centerX() else pivotX
        val cy = if (pivotY == 0f) bounds.centerY() else pivotY
        
        // 提取选区内容
        val contentBitmap = extractSelectionContent(sourceBitmap, selection)
        val width = contentBitmap.width
        val height = contentBitmap.height
        
        // 计算旋转后的新尺寸
        val cosA = abs(cos(angle.toDouble())).toFloat()
        val sinA = abs(sin(angle.toDouble())).toFloat()
        val newWidth = (width * cosA + height * sinA).toInt()
        val newHeight = (width * sinA + height * cosA).toInt()
        
        // 创建旋转后的位图
        val rotatedBitmap = createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(rotatedBitmap)
        
        canvas.translate(newWidth / 2f, newHeight / 2f)
        canvas.rotate((angle * 180f / PI).toFloat())
        canvas.translate(-width / 2f, -height / 2f)
        canvas.drawBitmap(contentBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        
        // 计算新的变换矩阵
        val newMatrix = Matrix()
        newMatrix.postTranslate(-cx, -cy)
        newMatrix.postRotate((angle * 180f / PI).toFloat())
        newMatrix.postTranslate(cx, cy)
        
        return TransformResult(
            bitmap = rotatedBitmap,
            matrix = newMatrix,
            sourceBounds = bounds,
            targetBounds = RectF(
                cx - newWidth / 2f,
                cy - newHeight / 2f,
                cx + newWidth / 2f,
                cy + newHeight / 2f
            )
        )
    }
    
    /**
     * 水平翻转选区内容
     * 
     * @param sourceBitmap 源位图
     * @param selection 选区
     * @return 翻转结果
     */
    fun flipHorizontal(
        sourceBitmap: Bitmap,
        selection: Selection
    ): TransformResult {
        val bounds = selection.getCurrentBounds()
        
        // 提取选区内容
        val contentBitmap = extractSelectionContent(sourceBitmap, selection)
        
        // 水平翻转
        val flippedBitmap = createBitmap(
            contentBitmap.width,
            contentBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(flippedBitmap)
        
        canvas.translate(contentBitmap.width.toFloat(), 0f)
        canvas.scale(-1f, 1f)
        canvas.drawBitmap(contentBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        
        // 计算新的变换矩阵
        val newMatrix = Matrix()
        newMatrix.postScale(-1f, 1f)
        
        return TransformResult(
            bitmap = flippedBitmap,
            matrix = newMatrix,
            sourceBounds = bounds,
            targetBounds = bounds
        )
    }
    
    /**
     * 垂直翻转选区内容
     * 
     * @param sourceBitmap 源位图
     * @param selection 选区
     * @return 翻转结果
     */
    fun flipVertical(
        sourceBitmap: Bitmap,
        selection: Selection
    ): TransformResult {
        val bounds = selection.getCurrentBounds()
        
        // 提取选区内容
        val contentBitmap = extractSelectionContent(sourceBitmap, selection)
        
        // 垂直翻转
        val flippedBitmap = createBitmap(
            contentBitmap.width,
            contentBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(flippedBitmap)
        
        canvas.translate(0f, contentBitmap.height.toFloat())
        canvas.scale(1f, -1f)
        canvas.drawBitmap(contentBitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        
        // 计算新的变换矩阵
        val newMatrix = Matrix()
        newMatrix.postScale(1f, -1f)
        
        return TransformResult(
            bitmap = flippedBitmap,
            matrix = newMatrix,
            sourceBounds = bounds,
            targetBounds = bounds
        )
    }
    
    /**
     * 透视变形
     * 使用四角映射实现近大远小效果
     * 
     * @param sourceBitmap 源位图
     * @param selection 选区
     * @param corners 目标角点位置
     * @return 透视变形结果
     */
    fun perspectiveTransform(
        sourceBitmap: Bitmap,
        selection: Selection,
        corners: PerspectiveCorners
    ): TransformResult {
        val bounds = selection.getCurrentBounds()
        
        // 提取选区内容
        val contentBitmap = extractSelectionContent(sourceBitmap, selection)
        
        // 计算目标尺寸
        val width = max(
            distance(corners.topLeft, corners.topRight),
            distance(corners.bottomLeft, corners.bottomRight)
        ).toInt()
        val height = max(
            distance(corners.topLeft, corners.bottomLeft),
            distance(corners.topRight, corners.bottomRight)
        ).toInt()
        
        if (width <= 0 || height <= 0) {
            return TransformResult(null, Matrix(), bounds, bounds)
        }
        
        // 创建透视变形后的位图
        val transformedBitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(transformedBitmap)
        
        // 源矩形
        val src = floatArrayOf(
            0f, 0f,
            contentBitmap.width.toFloat(), 0f,
            contentBitmap.width.toFloat(), contentBitmap.height.toFloat(),
            0f, contentBitmap.height.toFloat()
        )
        
        // 目标矩形
        val dst = corners.toFloatArray()
        
        // 计算透视变换矩阵
        val matrix = Matrix()
        matrix.setPolyToPoly(src, 0, dst, 0, 4)
        
        // 使用 Path 和 Paint 进行绘制
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(width.toFloat(), 0f)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
        
        // 保存画布状态
        canvas.save()
        
        // 应用变换
        canvas.concat(matrix)
        
        // 反向映射：将目标位图坐标映射回源位图坐标
        val inverseMatrix = Matrix(matrix)
        inverseMatrix.invert(inverseMatrix)
        
        // 逐像素采样（简化实现）
        for (y in 0 until height) {
            for (x in 0 until width) {
                val srcPoints = floatArrayOf(x.toFloat(), y.toFloat())
                inverseMatrix.mapPoints(srcPoints)
                
                val sx = srcPoints[0].toInt()
                val sy = srcPoints[1].toInt()
                
                if (sx >= 0 && sx < contentBitmap.width && 
                    sy >= 0 && sy < contentBitmap.height) {
                    val pixel = contentBitmap.getPixel(sx, sy)
                    transformedBitmap.setPixel(x, y, pixel)
                }
            }
        }
        
        canvas.restore()
        
        return TransformResult(
            bitmap = transformedBitmap,
            matrix = matrix,
            sourceBounds = bounds,
            targetBounds = RectF(
                minOf(corners.topLeft.x, corners.bottomLeft.x),
                minOf(corners.topLeft.y, corners.topRight.y),
                maxOf(corners.topRight.x, corners.bottomRight.x),
                maxOf(corners.bottomLeft.y, corners.bottomRight.y)
            )
        )
    }
    
    /**
     * 计算两点之间的距离
     */
    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * 提取选区内容
     * 从源位图中提取选区内的像素
     * 
     * @param sourceBitmap 源位图
     * @param selection 选区
     * @return 提取的位图
     */
    fun extractSelectionContent(
        sourceBitmap: Bitmap,
        selection: Selection
    ): Bitmap {
        val bounds = selection.getCurrentBounds()
        
        val left = bounds.left.toInt().coerceIn(0, sourceBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, sourceBitmap.height - 1)
        val width = (bounds.width().toInt()).coerceIn(1, sourceBitmap.width - left)
        val height = (bounds.height().toInt()).coerceIn(1, sourceBitmap.height - top)
        
        // 创建掩码
        val mask = createSelectionMask(selection, width, height)
        
        // 创建结果位图
        val result = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        // 提取像素
        for (y in 0 until height) {
            for (x in 0 until width) {
                val sx = left + x
                val sy = top + y
                
                if (sx >= 0 && sx < sourceBitmap.width && 
                    sy >= 0 && sy < sourceBitmap.height) {
                    val maskPixel = mask.getPixel(x, y)
                    val maskAlpha = (maskPixel shr 24) and 0xFF
                    
                    if (maskAlpha > 0) {
                        val sourcePixel = sourceBitmap.getPixel(sx, sy)
                        // 应用掩码 alpha
                        val newAlpha = (sourcePixel shr 24) and 0xFF
                        val newPixel = (newAlpha and 0xFF shl 24) or 
                                       (sourcePixel and 0x00FFFFFF)
                        result.setPixel(x, y, newPixel)
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * 创建选区掩码
     */
    private fun createSelectionMask(
        selection: Selection,
        width: Int,
        height: Int
    ): Bitmap {
        val mask = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        
        when (val shape = selection.shape) {
            is SelectionShape.Rectangle -> {
                canvas.drawRect(shape.bounds, paint)
            }
            is SelectionShape.Ellipse -> {
                canvas.drawOval(shape.bounds, paint)
            }
            is SelectionShape.Lasso -> {
                canvas.drawPath(shape.path, paint)
            }
            is SelectionShape.MagicWand -> {
                // 缩放掩码以适应新尺寸
                val srcBounds = shape.bounds
                val scaleX = width / srcBounds.width()
                val scaleY = height / srcBounds.height()
                
                val matrix = Matrix()
                matrix.postScale(scaleX, scaleY)
                
                canvas.save()
                canvas.concat(matrix)
                canvas.drawBitmap(shape.maskBitmap, 0f, 0f, paint)
                canvas.restore()
            }
        }
        
        return mask
    }
    
    /**
     * 应用变形结果到目标位图
     * 
     * @param targetBitmap 目标位图
     * @param result 变形结果
     * @param offsetX X 偏移
     * @param offsetY Y 偏移
     */
    fun applyTransformResult(
        targetBitmap: Bitmap,
        result: TransformResult,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        result.bitmap?.let { bitmap ->
            val canvas = Canvas(targetBitmap)
            
            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.drawBitmap(bitmap, result.matrix, Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        }
    }
    
    /**
     * 计算旋转角度（从两点）
     */
    fun calculateRotationAngle(
        centerX: Float, centerY: Float,
        currentX: Float, currentY: Float,
        startX: Float, startY: Float
    ): Float {
        val currentAngle = atan2(currentY - centerY, currentX - centerX)
        val startAngle = atan2(startY - centerY, startX - centerX)
        return currentAngle - startAngle
    }
    
    /**
     * 计算缩放比例
     */
    fun calculateScale(
        centerX: Float, centerY: Float,
        currentX: Float, currentY: Float,
        startX: Float, startY: Float
    ): Float {
        val currentDistance = sqrt(
            (currentX - centerX) * (currentX - centerX) +
            (currentY - centerY) * (currentY - centerY)
        )
        val startDistance = sqrt(
            (startX - centerX) * (startX - centerX) +
            (startY - centerY) * (startY - centerY)
        )
        
        return if (startDistance > 0) currentDistance / startDistance else 1f
    }
}
