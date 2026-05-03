package com.jiafenbu.androidpaint.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import com.jiafenbu.androidpaint.brush.BrushDescriptor
import com.jiafenbu.androidpaint.brush.BrushRenderer
import com.jiafenbu.androidpaint.brush.BrushType
import com.jiafenbu.androidpaint.model.BlendMode
import com.jiafenbu.androidpaint.model.StrokeData

/**
 * 渲染引擎抽象接口
 * 当前用 Canvas 2D 实现，将来可替换为 OpenGL/Vulkan 实现
 */
interface DrawEngine {
    /**
     * 初始化引擎
     * @param width 画布宽度
     * @param height 画布高度
     */
    fun initialize(width: Int, height: Int)

    /**
     * 将一个笔画渲染到位图上
     * @param bitmap 目标位图
     * @param stroke 笔画数据
     */
    fun renderStroke(bitmap: Bitmap, stroke: StrokeData)

    /**
     * 从笔画列表重新渲染整个位图（清空后重绘所有笔画）
     * @param bitmap 目标位图
     * @param strokes 笔画列表
     */
    fun rerenderLayer(bitmap: Bitmap, strokes: List<StrokeData>)

    /**
     * 增量渲染一个新笔画（不清空，直接叠加）
     * 自动判断笔刷类型，对需要操作已有像素的笔刷（模糊笔、涂抹笔）
     * 调用带位图参数的渲染方法
     * @param bitmap 目标位图
     * @param stroke 笔画数据
     */
    fun addStroke(bitmap: Bitmap, stroke: StrokeData)

    /**
     * 带锁定透明像素的笔画渲染
     * 当 layerBitmap 已有像素时，只在已有像素区域绘制
     * @param layerBitmap 图层位图（包含已有内容）
     * @param stroke 笔画数据
     */
    fun addStrokeWithLock(bitmap: Bitmap, layerBitmap: Bitmap, stroke: StrokeData)

    /**
     * 合成所有图层，返回最终位图
     * @param layers 图层渲染数据列表
     * @return 合成后的位图
     */
    fun compositeLayers(layers: List<LayerRenderData>): Bitmap

    /**
     * 创建一个新的空白位图
     * @param width 宽度
     * @param height 高度
     * @return 空白位图（透明背景）
     */
    fun createBlankBitmap(width: Int, height: Int): Bitmap

    /**
     * 清空位图内容（设为透明）
     * @param bitmap 目标位图
     */
    fun clearBitmap(bitmap: Bitmap)

    /**
     * 清理资源
     */
    fun cleanup()
}

/**
 * 图层渲染数据
 * 用于图层合成
 *
 * @param bitmap 图层的位图
 * @param opacity 透明度 (0f - 1f)
 * @param isVisible 是否可见
 * @param offsetX X轴偏移
 * @param offsetY Y轴偏移
 * @param blendMode 混合模式，默认为正常混合
 * @param isLocked 是否锁定透明像素
 */
data class LayerRenderData(
    val bitmap: Bitmap,
    val opacity: Float = 1f,
    val isVisible: Boolean = true,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val isLocked: Boolean = false
) {
    /**
     * 检查是否应该渲染
     */
    fun shouldRender(): Boolean = isVisible && opacity > 0f && !bitmap.isRecycled
}
