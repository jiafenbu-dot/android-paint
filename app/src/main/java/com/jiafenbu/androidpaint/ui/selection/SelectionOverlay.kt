package com.jiafenbu.androidpaint.ui.selection

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jiafenbu.androidpaint.model.Selection
import android.graphics.PathMeasure
import com.jiafenbu.androidpaint.model.SelectionShape
import com.jiafenbu.androidpaint.model.SelectionType
import com.jiafenbu.androidpaint.model.TransformHandle
import com.jiafenbu.androidpaint.model.TransformMode
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 选区覆盖层
 * 显示选区边框（蚂蚁线动画）、变换手柄，并处理选区交互
 * 
 * @param selection 选区
 * @param canvasWidth 画布宽度
 * @param canvasHeight 画布高度
 * @param scale 缩放比例
 * @param rotation 旋转角度
 * @param offsetX X 偏移
 * @param offsetY Y 偏移
 * @param onSelectionChange 选区变化回调
 * @param onTransformStart 开始变换回调
 * @param onTransformEnd 结束变换回调
 */
@Composable
fun SelectionOverlay(
    selection: Selection?,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    onSelectionChange: (Selection?) -> Unit,
    onTransformStart: (TransformHandle) -> Unit,
    onTransformEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 蚂蚁线动画偏移
    val infiniteTransition = rememberInfiniteTransition(label = "marchingAnts")
    val marchOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marchOffset"
    )
    
    // 变换状态
    var activeHandle by remember { mutableStateOf<TransformHandle?>(null) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var lastDrag by remember { mutableStateOf(Offset.Zero) }
    var isTransforming by remember { mutableStateOf(false) }
    
    // 手柄大小
    val handleSize = 12.dp
    
    if (selection != null) {
        val androidBounds = selection.getCurrentBounds()
        val bounds = Rect(androidBounds.left, androidBounds.top, androidBounds.right, androidBounds.bottom)

        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(selection) {
                    detectTapGestures(
                        onTap = { offset ->
                            // 点击变换手柄
                            val handle = findHandleAtPosition(
                                offset, bounds, scale, rotation, offsetX, offsetY,
                                canvasWidth, canvasHeight
                            )
                            if (handle != null) {
                                onTransformStart(handle)
                                activeHandle = handle
                                isTransforming = true
                            }
                        },
                        onLongPress = {
                            // 长按进入变换模式
                            onTransformStart(TransformHandle.CENTER)
                            activeHandle = TransformHandle.CENTER
                            isTransforming = true
                        }
                    )
                }
                .pointerInput(selection, activeHandle) {
                    if (activeHandle != null) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragStart = offset
                                lastDrag = offset
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = change.position - lastDrag
                                lastDrag = change.position
                                
                                // 根据手柄类型处理拖拽
                                // 变换逻辑由上层处理
                            },
                            onDragEnd = {
                                activeHandle = null
                                isTransforming = false
                                onTransformEnd()
                            }
                        )
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 绘制选区边框
                drawSelectionBorder(
                    selection = selection,
                    marchOffset = marchOffset,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    scale = scale,
                    rotation = rotation,
                    offsetX = offsetX,
                    offsetY = offsetY
                )
                
                // 绘制变换手柄
                if (isTransforming || selection.transformMode != TransformMode.NONE) {
                    drawTransformHandles(
                        bounds = bounds,
                        handle = activeHandle,
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        scale = scale,
                        rotation = rotation,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }
            }
        }
    }
}

/**
 * 绘制选区边框（带蚂蚁线动画）
 */
private fun DrawScope.drawSelectionBorder(
    selection: Selection,
    marchOffset: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float
) {
    val pathEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(10f, 10f),
        phase = marchOffset
    )
    
    val stroke = Stroke(
        width = 2f,
        pathEffect = pathEffect,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    
    // 应用画布变换
    rotate(rotation * 180f / PI.toFloat()) {
        // 绘制选区形状
        when (val shape = selection.shape) {
            is SelectionShape.Rectangle -> {
                val bounds = transformBoundsFromRectF(shape.bounds, scale, offsetX, offsetY)
                drawRect(
                    color = Color.White,
                    topLeft = bounds.topLeft,
                    size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                    style = stroke
                )
            }
            
            is SelectionShape.Ellipse -> {
                val bounds = transformBoundsFromRectF(shape.bounds, scale, offsetX, offsetY)
                drawOval(
                    color = Color.White,
                    topLeft = bounds.topLeft,
                    size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                    style = stroke
                )
            }
            
            is SelectionShape.Lasso -> {
                val transformedPath = transformPath(shape.path, scale, offsetX, offsetY)
                drawPath(
                    path = transformedPath,
                    color = Color.White,
                    style = stroke
                )
            }
            
            is SelectionShape.MagicWand -> {
                val bounds = transformBoundsFromRectF(shape.bounds, scale, offsetX, offsetY)
                drawRect(
                    color = Color.White,
                    topLeft = bounds.topLeft,
                    size = androidx.compose.ui.geometry.Size(bounds.width, bounds.height),
                    style = stroke
                )
            }
        }
    }
}

/**
 * 绘制变换手柄
 */
private fun DrawScope.drawTransformHandles(
    bounds: Rect,
    handle: TransformHandle?,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float
) {
    val handleColor = Color(0xFF2196F3)
    val handleSize = 10f
    
    val transformedBounds = transformBoundsFromRectF(
        android.graphics.RectF(bounds.left, bounds.top, bounds.right, bounds.bottom),
        scale, offsetX, offsetY
    )
    
    // 手柄位置
    val topLeft = Offset(transformedBounds.left, transformedBounds.top)
    val topRight = Offset(transformedBounds.right, transformedBounds.top)
    val bottomLeft = Offset(transformedBounds.left, transformedBounds.bottom)
    val bottomRight = Offset(transformedBounds.right, transformedBounds.bottom)
    val topCenter = Offset(transformedBounds.center.x, transformedBounds.top)
    val bottomCenter = Offset(transformedBounds.center.x, transformedBounds.bottom)
    val leftCenter = Offset(transformedBounds.left, transformedBounds.center.y)
    val rightCenter = Offset(transformedBounds.right, transformedBounds.center.y)
    
    // 绘制四角手柄（较大）
    listOf(topLeft, topRight, bottomLeft, bottomRight).forEach { pos ->
        drawCircle(
            color = handleColor,
            radius = handleSize,
            center = pos
        )
        drawCircle(
            color = Color.White,
            radius = handleSize - 2f,
            center = pos
        )
    }
    
    // 绘制边中手柄（较小）
    listOf(topCenter, bottomCenter, leftCenter, rightCenter).forEach { pos ->
        drawCircle(
            color = handleColor,
            radius = handleSize * 0.7f,
            center = pos
        )
        drawCircle(
            color = Color.White,
            radius = handleSize * 0.5f,
            center = pos
        )
    }
    
    // 绘制中心手柄（旋转时使用）
    val center = Offset(
        (transformedBounds.left + transformedBounds.right) / 2,
        (transformedBounds.top + transformedBounds.bottom) / 2
    )
    drawCircle(
        color = handleColor,
        radius = handleSize * 0.6f,
        center = center
    )
}

/**
 * 变换边界矩形（从 RectF 转换为 Rect）
 */
private fun transformBoundsFromRectF(
    bounds: android.graphics.RectF,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): Rect {
    return Rect(
        left = bounds.left * scale + offsetX,
        top = bounds.top * scale + offsetY,
        right = bounds.right * scale + offsetX,
        bottom = bounds.bottom * scale + offsetY
    )
}

/**
 * 变换路径
 */
private fun transformPath(
    path: android.graphics.Path,
    scale: Float,
    offsetX: Float,
    offsetY: Float
): androidx.compose.ui.graphics.Path {
    // 简化实现：创建 Compose Path 并应用变换
    val composePath = androidx.compose.ui.graphics.Path()
    
    // 将 Android Path 转换为 Compose Path
    val pm = PathMeasure(path, false)
    val length = pm.length
    val points = FloatArray(2)
    val tangent = FloatArray(2)
    
    if (length > 0) {
        var distance = 0f
        while (distance <= length) {
            pm.getPosTan(distance, points, tangent)
            val x = points[0] * scale + offsetX
            val y = points[1] * scale + offsetY
            if (distance == 0f) {
                composePath.moveTo(x, y)
            } else {
                composePath.lineTo(x, y)
            }
            distance += 2f
        }
        pm.getPosTan(length, points, tangent)
        composePath.lineTo(points[0] * scale + offsetX, points[1] * scale + offsetY)
    }
    
    return composePath
}

/**
 * 在给定位置查找变换手柄
 */
private fun findHandleAtPosition(
    position: Offset,
    bounds: Rect,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    canvasWidth: Float,
    canvasHeight: Float
): TransformHandle? {
    val transformedBounds = transformBoundsFromRectF(
        android.graphics.RectF(bounds.left, bounds.top, bounds.right, bounds.bottom),
        scale, offsetX, offsetY
    )
    val handleRadius = 15f
    
    val topLeft = Offset(transformedBounds.left, transformedBounds.top)
    val topRight = Offset(transformedBounds.right, transformedBounds.top)
    val bottomLeft = Offset(transformedBounds.left, transformedBounds.bottom)
    val bottomRight = Offset(transformedBounds.right, transformedBounds.bottom)
    val topCenter = Offset(transformedBounds.center.x, transformedBounds.top)
    val bottomCenter = Offset(transformedBounds.center.x, transformedBounds.bottom)
    val leftCenter = Offset(transformedBounds.left, transformedBounds.center.y)
    val rightCenter = Offset(transformedBounds.right, transformedBounds.center.y)
    
    // 检查四角手柄
    if (distance(position, topLeft) < handleRadius) return TransformHandle.TOP_LEFT
    if (distance(position, topRight) < handleRadius) return TransformHandle.TOP_RIGHT
    if (distance(position, bottomLeft) < handleRadius) return TransformHandle.BOTTOM_LEFT
    if (distance(position, bottomRight) < handleRadius) return TransformHandle.BOTTOM_RIGHT
    
    // 检查边中手柄
    if (distance(position, topCenter) < handleRadius) return TransformHandle.TOP_CENTER
    if (distance(position, bottomCenter) < handleRadius) return TransformHandle.BOTTOM_CENTER
    if (distance(position, leftCenter) < handleRadius) return TransformHandle.LEFT_CENTER
    if (distance(position, rightCenter) < handleRadius) return TransformHandle.RIGHT_CENTER
    
    // 检查中心
    val center = Offset(
        (transformedBounds.left + transformedBounds.right) / 2,
        (transformedBounds.top + transformedBounds.bottom) / 2
    )
    if (distance(position, center) < handleRadius) return TransformHandle.CENTER
    
    return null
}

/**
 * 计算两点间距离
 */
private fun distance(p1: Offset, p2: Offset): Float {
    return hypot(p1.x - p2.x, p1.y - p2.y)
}

/**
 * 选区工具栏
 * 位于选区旁边或底部的操作按钮
 */
@Composable
fun SelectionToolbar(
    onFill: () -> Unit,
    onClear: () -> Unit,
    onStroke: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onInvert: () -> Unit,
    onFeather: () -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 实现待补充 - 可以使用 Row 布局多个 IconButton
}
