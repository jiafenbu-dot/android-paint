package com.jiafenbu.androidpaint.model

/**
 * 画布状态
 * 记录画布的缩放、旋转和偏移
 * @param scale 缩放比例
 * @param rotation 旋转角度（弧度）
 * @param offsetX X轴偏移
 * @param offsetY Y轴偏移
 */
data class CanvasState(
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    companion object {
        /** 最小缩放比例 */
        const val MIN_SCALE = 0.1f
        
        /** 最大缩放比例 */
        const val MAX_SCALE = 10f
    }
}
