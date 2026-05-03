package com.jiafenbu.androidpaint.command

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.jiafenbu.androidpaint.model.Selection
import com.jiafenbu.androidpaint.model.SelectionShape
import com.jiafenbu.androidpaint.selection.SelectionEngine

/**
 * 选区填充命令
 * 使用前景色填充选区
 * 
 * @param layerIndex 图层索引
 * @param layerBitmap 图层位图
 * @param selection 选区
 * @param fillColor 填充颜色
 */
class SelectionFillCommand(
    private val layerIndex: Int,
    private val layerBitmap: Bitmap,
    private val selection: Selection,
    private val fillColor: Int
) : DrawCommand {
    
    // 备份被填充区域的原始像素
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
        
        // 创建填充掩码
        val mask = createSelectionMask(width, height)
        
        // 应用填充
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskPixel = mask.getPixel(x, y)
                val maskAlpha = (maskPixel shr 24) and 0xFF
                
                if (maskAlpha > 0) {
                    val index = y * width + x
                    val originalColor = backupPixels!![index]
                    
                    // 混合颜色
                    val newColor = blendColors(originalColor, fillColor)
                    layerBitmap.setPixel(left + x, top + y, newColor)
                }
            }
        }
        
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
    
    private fun createSelectionMask(width: Int, height: Int): Bitmap {
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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
                canvas.drawBitmap(shape.maskBitmap, 0f, 0f, paint)
            }
        }
        
        return mask
    }
    
    private fun blendColors(base: Int, overlay: Int): Int {
        val baseA = (base shr 24) and 0xFF
        val baseR = (base shr 16) and 0xFF
        val baseG = (base shr 8) and 0xFF
        val baseB = base and 0xFF
        
        val overA = (overlay shr 24) and 0xFF
        val overR = (overlay shr 16) and 0xFF
        val overG = (overlay shr 8) and 0xFF
        val overB = overlay and 0xFF
        
        // 简单混合（替换）
        if (overA >= 255) {
            return overlay
        }
        
        val outA = overA
        val outR = overR
        val outG = overG
        val outB = overB
        
        return (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
    }
}

/**
 * 选区清除命令
 * 清除选区内容（设为透明）
 * 
 * @param layerBitmap 图层位图
 * @param selection 选区
 */
class SelectionClearCommand(
    private val layerBitmap: Bitmap,
    private val selection: Selection
) : DrawCommand {
    
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
        
        // 清除（设为透明）
        val mask = createSelectionMask(selection, width, height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskPixel = mask.getPixel(x, y)
                val maskAlpha = (maskPixel shr 24) and 0xFF
                
                if (maskAlpha > 0) {
                    layerBitmap.setPixel(left + x, top + y, Color.TRANSPARENT)
                }
            }
        }
        
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
    
    private fun createSelectionMask(selection: Selection, width: Int, height: Int): Bitmap {
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
 * 选区描边命令
 * 沿选区边缘绘制线条
 * 
 * @param layerBitmap 图层位图
 * @param selection 选区
 * @param strokeColor 描边颜色
 * @param strokeWidth 描边宽度
 */
class SelectionStrokeCommand(
    private val layerBitmap: Bitmap,
    private val selection: Selection,
    private val strokeColor: Int,
    private val strokeWidth: Float
) : DrawCommand {
    
    private var backupPixels: IntArray? = null
    private var strokeBounds: RectF? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        val bounds = selection.getCurrentBounds()
        val padding = strokeWidth.toInt() + 2
        val left = (bounds.left - padding).toInt().coerceAtLeast(0)
        val top = (bounds.top - padding).toInt().coerceAtLeast(0)
        val width = (bounds.width() + padding * 2).toInt().coerceAtMost(layerBitmap.width - left)
        val height = (bounds.height() + padding * 2).toInt().coerceAtMost(layerBitmap.height - top)
        
        // 备份原始像素
        backupPixels = IntArray(width * height)
        layerBitmap.getPixels(backupPixels!!, 0, width, left, top, width, height)
        strokeBounds = RectF(left.toFloat(), top.toFloat(), (left + width).toFloat(), (top + height).toFloat())
        
        // 绘制描边
        val canvas = Canvas(layerBitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            this.strokeWidth = this@SelectionStrokeCommand.strokeWidth
        }
        
        // 创建裁剪区域
        val clipPath = createClipPath(selection, left.toFloat(), top.toFloat())
        
        // 绘制边缘
        val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            this.strokeWidth = this@SelectionStrokeCommand.strokeWidth
            // 内描边
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        
        // 简化实现：直接在边缘绘制
        when (val shape = selection.shape) {
            is SelectionShape.Rectangle -> {
                val rect = RectF(shape.bounds)
                canvas.drawRect(rect, paint)
            }
            is SelectionShape.Ellipse -> {
                val rect = RectF(shape.bounds)
                canvas.drawOval(rect, paint)
            }
            is SelectionShape.Lasso -> {
                canvas.drawPath(shape.path, paint)
            }
            is SelectionShape.MagicWand -> {
                // 绘制矩形边界
                val rect = RectF(shape.bounds)
                canvas.drawRect(rect, paint)
            }
        }
        
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || backupPixels == null || strokeBounds == null) return
        
        val bounds = strokeBounds!!
        val width = (bounds.width()).toInt()
        val height = (bounds.height()).toInt()
        
        layerBitmap.setPixels(backupPixels!!, 0, width, 
            bounds.left.toInt(), bounds.top.toInt(), width, height)
        isExecuted = false
    }
    
    private fun createClipPath(selection: Selection, offsetX: Float, offsetY: Float): Path {
        val path = Path()
        
        when (val shape = selection.shape) {
            is SelectionShape.Rectangle -> {
                path.addRect(shape.bounds, Path.Direction.CW)
            }
            is SelectionShape.Ellipse -> {
                path.addOval(shape.bounds, Path.Direction.CW)
            }
            is SelectionShape.Lasso -> {
                path.set(shape.path)
            }
            is SelectionShape.MagicWand -> {
                path.addRect(shape.bounds, Path.Direction.CW)
            }
        }
        
        path.offset(-offsetX, -offsetY)
        return path
    }
}

/**
 * 选区移动命令
 * 移动选区内容
 * 
 * @param layerBitmap 图层位图
 * @param selection 选区
 * @param deltaX X 方向偏移
 * @param deltaY Y 方向偏移
 */
class SelectionMoveCommand(
    private val layerBitmap: Bitmap,
    private val selection: Selection,
    private val deltaX: Float,
    private val deltaY: Float
) : DrawCommand {
    
    private var contentBitmap: Bitmap? = null
    private var originalPixels: IntArray? = null
    private var contentBounds: RectF? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        val bounds = selection.getCurrentBounds()
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        // 备份原始像素
        originalPixels = IntArray(width * height)
        layerBitmap.getPixels(originalPixels!!, 0, width, left, top, width, height)
        
        // 提取选区内容
        contentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val mask = SelectionEngine.applyFeatherToMask(
            createMask(selection, width, height),
            selection.featherRadius
        )
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskPixel = mask.getPixel(x, y)
                val maskAlpha = (maskPixel shr 24) and 0xFF
                
                if (maskAlpha > 0) {
                    val originalPixel = originalPixels!![y * width + x]
                    contentBitmap!!.setPixel(x, y, originalPixel)
                }
            }
        }
        
        // 清除原始区域
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskPixel = mask.getPixel(x, y)
                val maskAlpha = (maskPixel shr 24) and 0xFF
                
                if (maskAlpha > 0) {
                    layerBitmap.setPixel(left + x, top + y, Color.TRANSPARENT)
                }
            }
        }
        
        // 绘制到新位置
        val newLeft = (left + deltaX).toInt().coerceIn(0, layerBitmap.width - 1)
        val newTop = (top + deltaY).toInt().coerceIn(0, layerBitmap.height - 1)
        
        contentBitmap?.let { content ->
            for (y in 0 until minOf(content.height, layerBitmap.height - newTop)) {
                for (x in 0 until minOf(content.width, layerBitmap.width - newLeft)) {
                    val pixel = content.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xFF
                    
                    if (alpha > 0) {
                        layerBitmap.setPixel(newLeft + x, newTop + y, pixel)
                    }
                }
            }
        }
        
        contentBounds = RectF(newLeft.toFloat(), newTop.toFloat(), 
            (newLeft + width).toFloat(), (newTop + height).toFloat())
        
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || originalPixels == null) return
        
        val bounds = selection.getCurrentBounds()
        val left = bounds.left.toInt().coerceIn(0, layerBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, layerBitmap.height - 1)
        val width = bounds.width().toInt().coerceIn(1, layerBitmap.width - left)
        val height = bounds.height().toInt().coerceIn(1, layerBitmap.height - top)
        
        layerBitmap.setPixels(originalPixels!!, 0, width, left, top, width, height)
        
        // 清除移动后的区域
        if (contentBounds != null) {
            val newLeft = contentBounds!!.left.toInt()
            val newTop = contentBounds!!.top.toInt()
            for (y in 0 until minOf(height, layerBitmap.height - newTop)) {
                for (x in 0 until minOf(width, layerBitmap.width - newLeft)) {
                    layerBitmap.setPixel(newLeft + x, newTop + y, Color.TRANSPARENT)
                }
            }
        }
        
        isExecuted = false
    }
    
    private fun createMask(selection: Selection, width: Int, height: Int): Bitmap {
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
 * 选区复制命令
 * 复制选区内容到新图层
 * 
 * @param sourceBitmap 源图层位图
 * @param selection 选区
 * @return 新图层的位图内容
 */
class SelectionCopyCommand(
    private val sourceBitmap: Bitmap,
    private val selection: Selection
) : DrawCommand {
    
    private var copiedBitmap: Bitmap? = null
    
    override fun execute() {
        val bounds = selection.getCurrentBounds()
        val width = bounds.width().toInt().coerceIn(1, sourceBitmap.width)
        val height = bounds.height().toInt().coerceIn(1, sourceBitmap.height)
        
        copiedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        val mask = SelectionEngine.applyFeatherToMask(
            createMask(selection, width, height),
            selection.featherRadius
        )
        
        val left = bounds.left.toInt().coerceIn(0, sourceBitmap.width - 1)
        val top = bounds.top.toInt().coerceIn(0, sourceBitmap.height - 1)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val maskPixel = mask.getPixel(x, y)
                val maskAlpha = (maskPixel shr 24) and 0xFF
                
                if (maskAlpha > 0) {
                    val sx = left + x
                    val sy = top + y
                    
                    if (sx < sourceBitmap.width && sy < sourceBitmap.height) {
                        copiedBitmap!!.setPixel(x, y, sourceBitmap.getPixel(sx, sy))
                    }
                }
            }
        }
    }
    
    override fun undo() {
        copiedBitmap?.recycle()
        copiedBitmap = null
    }
    
    /**
     * 获取复制的位图
     */
    fun getCopiedBitmap(): Bitmap? = copiedBitmap
    
    /**
     * 获取内容边界
     */
    fun getContentBounds(): RectF = selection.getCurrentBounds()
    
    private fun createMask(selection: Selection, width: Int, height: Int): Bitmap {
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
 * 选区羽化命令
 * 对选区应用羽化效果
 * 
 * @param selection 选区
 * @param radius 羽化半径
 */
class SelectionFeatherCommand(
    private var selection: Selection,
    private var radius: Float
) : DrawCommand {
    
    private var originalRadius: Float = 0f
    
    override fun execute() {
        originalRadius = selection.featherRadius
        selection = selection.setFeather(radius)
    }
    
    override fun undo() {
        selection = selection.setFeather(originalRadius)
    }
    
    /**
     * 获取羽化后的选区
     */
    fun getFeatheredSelection(): Selection = selection
}

/**
 * 选区反选命令
 * 反转选区选择
 * 
 * @param originalSelection 原始选区
 * @param canvasWidth 画布宽度
 * @param canvasHeight 画布高度
 */
class SelectionInvertCommand(
    private val originalSelection: Selection,
    private val canvasWidth: Int,
    private val canvasHeight: Int
) : DrawCommand {
    
    private var invertedSelection: Selection? = null
    
    override fun execute() {
        invertedSelection = SelectionEngine.invertSelection(
            originalSelection, canvasWidth, canvasHeight
        )
    }
    
    override fun undo() {
        invertedSelection = null
    }
    
    /**
     * 获取反选后的选区
     */
    fun getInvertedSelection(): Selection? = invertedSelection
}
