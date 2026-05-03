package com.jiafenbu.androidpaint.model

/**
 * 笔画中的一个点
 * @param x X坐标
 * @param y Y坐标
 * @param pressure 压力值（预留，目前恒为1f）
 * @param timestamp 时间戳（预留）
 */
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = 0L
)

/**
 * 完整的笔画数据
 * 包含所有点和笔刷参数，用于撤销/重做
 * @param points 笔画经过的所有点
 * @param brushDescriptor 笔刷参数描述
 * @param color 颜色值（ARGB格式）
 */
data class StrokeData(
    val points: List<StrokePoint>,
    val brushDescriptor: com.jiafenbu.androidpaint.brush.BrushDescriptor,
    val color: Int
)
