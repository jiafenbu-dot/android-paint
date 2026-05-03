package com.jiafenbu.androidpaint.model

import android.graphics.Path
import android.graphics.RectF
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset

/**
 * 选区类型枚举
 */
enum class SelectionType {
    RECTANGLE,     // 矩形选区
    ELLIPSE,       // 椭圆选区
    LASSO,         // 自由套索
    MAGIC_WAND     // 魔棒选区
}

/**
 * 选区形状数据
 * 存储不同类型选区的路径和边界信息
 */
sealed class SelectionShape {
    abstract val bounds: RectF
    
    /** 矩形选区 */
    data class Rectangle(
        override val bounds: RectF
    ) : SelectionShape()
    
    /** 椭圆选区 */
    data class Ellipse(
        override val bounds: RectF
    ) : SelectionShape()
    
    /** 自由套索选区（由路径定义） */
    data class Lasso(
        val path: Path,
        override val bounds: RectF
    ) : SelectionShape()
    
    /** 魔棒选区（由位图掩码定义） */
    data class MagicWand(
        val maskBitmap: Bitmap,
        override val bounds: RectF
    ) : SelectionShape()
}

/**
 * 变换手柄位置
 */
enum class TransformHandle {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    LEFT_CENTER, RIGHT_CENTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
    CENTER  // 整体移动
}

/**
 * 变形模式
 */
enum class TransformMode {
    NONE,           // 无变形
    MOVE,           // 移动
    SCALE,          // 缩放
    ROTATE,         // 旋转
    FLIP_HORIZONTAL, // 水平翻转
    FLIP_VERTICAL,   // 垂直翻转
    PERSPECTIVE     // 透视变形
}

/**
 * 选区数据模型
 * 存储选区的形状、状态和变换信息
 * 
 * @param id 选区唯一标识
 * @param type 选区类型
 * @param shape 选区形状数据
 * @param isFeathered 是否已羽化
 * @param featherRadius 羽化半径
 * @param transformMode 当前变形模式
 * @param transformMatrix 变换矩阵
 * @param originalBounds 原始边界（用于撤销）
 */
data class Selection(
    val id: Long = System.currentTimeMillis(),
    val type: SelectionType = SelectionType.RECTANGLE,
    val shape: SelectionShape = SelectionShape.Rectangle(RectF(0f, 0f, 100f, 100f)),
    val isFeathered: Boolean = false,
    val featherRadius: Float = 0f,
    val transformMode: TransformMode = TransformMode.NONE,
    val transformMatrix: Matrix = Matrix(),
    val originalBounds: RectF = RectF(0f, 0f, 100f, 100f)
) {
    companion object {
        private var nextId = 0L
        
        /**
         * 创建新的选区
         */
        fun create(
            type: SelectionType,
            shape: SelectionShape,
            featherRadius: Float = 0f
        ): Selection {
            return Selection(
                id = nextId++,
                type = type,
                shape = shape,
                isFeathered = featherRadius > 0,
                featherRadius = featherRadius,
                originalBounds = RectF(shape.bounds)
            )
        }
        
        /**
         * 重置 ID 计数器
         */
        fun resetIdCounter() {
            nextId = 0L
        }
    }
    
    /**
     * 判断点是否在选区内
     */
    fun containsPoint(x: Float, y: Float): Boolean {
        return when (val s = shape) {
            is SelectionShape.Rectangle -> s.bounds.contains(x, y)
            is SelectionShape.Ellipse -> isPointInEllipse(x, y, s.bounds)
            is SelectionShape.Lasso -> isPointInLasso(x, y, s.path)
            is SelectionShape.MagicWand -> isPointInMask(x.toInt(), y.toInt(), s.maskBitmap)
        }
    }
    
    /**
     * 判断点是否在椭圆内
     */
    private fun isPointInEllipse(x: Float, y: Float, bounds: RectF): Boolean {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val rx = bounds.width() / 2f
        val ry = bounds.height() / 2f
        
        if (rx <= 0 || ry <= 0) return false
        
        val dx = (x - cx) / rx
        val dy = (y - cy) / ry
        return (dx * dx + dy * dy) <= 1f
    }
    
    /**
     * 判断点是否在套索路径内
     */
    private fun isPointInLasso(x: Float, y: Float, path: Path): Boolean {
        val rect = RectF()
        path.computeBounds(rect, true)
        if (!rect.contains(x, y)) return false
        
        // 使用 Path.contains 进行更准确的判断
        // Path.contains() 需要传入 Path.Direction 参数
        val region = android.graphics.Region()
        val clipRegion = android.graphics.Region()
        clipRegion.set(
            rect.left.toInt(), rect.top.toInt(),
            rect.right.toInt(), rect.bottom.toInt()
        )
        region.setPath(path, clipRegion)
        return region.contains(x.toInt(), y.toInt())
    }
    
    /**
     * 判断点是否在魔棒掩码内
     */
    private fun isPointInMask(px: Int, py: Int, mask: Bitmap): Boolean {
        if (px < 0 || py < 0 || px >= mask.width || py >= mask.height) return false
        val pixels = IntArray(1)
        mask.getPixels(pixels, 0, 1, px, py, 1, 1)
        return (pixels[0] shr 24) and 0xFF > 0
    }
    
    /**
     * 应用变换矩阵到选区
     */
    fun applyTransform(matrix: Matrix): Selection {
        return copy(transformMatrix = matrix)
    }
    
    /**
     * 设置变形模式
     */
    fun setTransformMode(mode: TransformMode): Selection {
        return copy(transformMode = mode)
    }
    
    /**
     * 设置羽化
     */
    fun setFeather(radius: Float): Selection {
        return copy(
            isFeathered = radius > 0,
            featherRadius = radius
        )
    }
    
    /**
     * 获取当前边界
     */
    fun getCurrentBounds(): RectF {
        val matrix = transformMatrix
        val points = floatArrayOf(
            originalBounds.left, originalBounds.top,
            originalBounds.right, originalBounds.top,
            originalBounds.right, originalBounds.bottom,
            originalBounds.left, originalBounds.bottom
        )
        matrix.mapPoints(points)
        
        return RectF().apply {
            left = minOf(points[0], points[2], points[4], points[6])
            top = minOf(points[1], points[3], points[5], points[7])
            right = maxOf(points[0], points[2], points[4], points[6])
            bottom = maxOf(points[1], points[3], points[5], points[7])
        }
    }
}

/**
 * 变换操作的目标范围
 */
enum class TransformTarget {
    CURRENT_LAYER,    // 当前图层
    ALL_VISIBLE_LAYERS, // 所有可见图层
    SELECTION_AREA    // 选区内容
}

/**
 * 工具模式
 * 用于区分不同的交互模式
 * 阶段7新增：TEXT（文字工具）、WATERMARK（水印工具）
 */
enum class ToolMode {
    DRAW,         // 绘画模式（默认）
    SELECTION,    // 选区模式
    EYEDROPPER,   // 吸管模式
    TRANSFORM,    // 变形模式
    REFERENCE,    // 参考图模式
    PAN,          // 平移模式
    ZOOM,         // 缩放模式
    TEXT,         // 文字工具
    WATERMARK     // 水印工具
}

/**
 * 对称轴类型
 */
enum class SymmetryAxis {
    NONE,           // 无对称
    HORIZONTAL,     // 水平对称
    VERTICAL,       // 垂直对称
    RADIAL          // 径向对称
}

/**
 * 网格类型
 */
enum class GridType {
    NONE,           // 无网格
    RULE_OF_THIRDS, // 三分法网格
    PERSPECTIVE     // 透视网格
}
