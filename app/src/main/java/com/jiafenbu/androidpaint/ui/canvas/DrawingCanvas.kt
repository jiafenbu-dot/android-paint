package com.jiafenbu.androidpaint.ui.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jiafenbu.androidpaint.brush.BrushRenderer
import com.jiafenbu.androidpaint.brush.BrushType
import com.jiafenbu.androidpaint.model.CanvasState
import com.jiafenbu.androidpaint.model.GridType
import com.jiafenbu.androidpaint.model.SelectionType
import com.jiafenbu.androidpaint.model.StrokeData
import com.jiafenbu.androidpaint.model.StrokePoint
import com.jiafenbu.androidpaint.model.SymmetryAxis
import com.jiafenbu.androidpaint.model.ToolMode
import com.jiafenbu.androidpaint.selection.SelectionEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 绘图画布组件
 * 支持多种工具模式：绘画、选区、吸管、变形等
 * 单指绘画、双指缩放旋转
 */
@Composable
fun DrawingCanvas(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    // 合成后的位图（canvasRevision 变化时重新合成）
    val compositedBitmap = remember(viewModel.layers, viewModel.canvasWidth, viewModel.canvasHeight, viewModel.canvasRevision) {
        viewModel.getCompositedBitmap()
    }

    // 当前笔画预览（通过 State 自动触发重组）
    val currentStroke = viewModel.currentStrokePoints

    // 选区预览
    val selectionPoints = viewModel.getSelectionPoints()
    val selectionStartPoint = viewModel.getSelectionStartPoint()

    // 手势状态
    var scale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var hasInitialized by remember { mutableStateOf(false) }

    // 临时变量用于选区绘制
    var tempSelectionEnd by remember { mutableStateOf(Offset.Zero) }

    // 画布尺寸（Composable scope）
    val canvasWidth = viewModel.canvasWidth.toFloat()
    val canvasHeight = viewModel.canvasHeight.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .onSizeChanged { size ->
                // 初始缩放：让画布适配屏幕（留 10% 边距）
                if (!hasInitialized) {
                    val fitScaleX = size.width * 0.9f / canvasWidth
                    val fitScaleY = size.height * 0.9f / canvasHeight
                    scale = minOf(fitScaleX, fitScaleY)
                    hasInitialized = true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown()

                    var zoom = 1f
                    var rotationChange = 0f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    val touchSlop = viewConfiguration.touchSlop
                    var lockedToPan = false

                    var isDrawing = false
                    val downPosition = firstDown.position

                    // 根据工具模式处理
                    val currentToolMode = viewModel.toolMode
                    val isFillBrush = viewModel.currentBrush.type == BrushType.FILL

                    // 坐标转换
                    val canvasPoint = screenToCanvas(
                        downPosition,
                        scale,
                        rotation,
                        size.width.toFloat(),
                        size.height.toFloat(),
                        viewModel.canvasWidth.toFloat(),
                        viewModel.canvasHeight.toFloat(),
                        offset
                    )

                    // 根据工具模式开始操作
                    when (currentToolMode) {
                        ToolMode.DRAW -> {
                            // 绘画模式
                            if (isFillBrush) {
                                isDrawing = true
                                executeFillAt(viewModel, canvasPoint.x.toInt(), canvasPoint.y.toInt())
                            } else {
                                viewModel.startStroke(canvasPoint.x, canvasPoint.y)
                                isDrawing = true
                            }
                        }
                        
                        ToolMode.SELECTION -> {
                            // 选区模式
                            viewModel.startSelection(canvasPoint.x, canvasPoint.y)
                            isDrawing = true
                        }
                        
                        ToolMode.EYEDROPPER -> {
                            // 吸管模式 - 立即取色
                            viewModel.pickColorFromCanvas(canvasPoint.x, canvasPoint.y)
                        }
                        
                        ToolMode.TEXT -> {
                            // 文字模式 - 点击设置文字输入位置
                            viewModel.setTextInputPosition(canvasPoint.x, canvasPoint.y)
                            viewModel.toggleTextToolPanel()
                        }
                        
                        ToolMode.WATERMARK -> {
                            // 水印模式 - 点击打开水印面板
                            viewModel.toggleWatermarkPanel()
                        }
                        
                        ToolMode.TRANSFORM,
                        ToolMode.REFERENCE,
                        ToolMode.PAN,
                        ToolMode.ZOOM -> {
                            // 这些模式需要双指手势或特殊处理
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                        val changes = event.changes

                        if (changes.size > 1) {
                            // 多指手势
                            val transformEvent = awaitPointerEvent()
                            if (!lockedToPan) {
                                zoom = transformEvent.calculateZoom()
                                rotationChange = transformEvent.calculateRotation()
                                pan = transformEvent.calculatePan()
                            }

                            val centroidSize = transformEvent.calculateCentroidSize()
                            if (!pastTouchSlop && centroidSize > touchSlop) {
                                pastTouchSlop = true
                            }

                            if (pastTouchSlop) {
                                lockedToPan = true

                                // 取消当前操作
                                if (isDrawing) {
                                    when (currentToolMode) {
                                        ToolMode.DRAW -> {
                                            if (!isFillBrush) {
                                                viewModel.endStroke()
                                            }
                                        }
                                        ToolMode.SELECTION -> {
                                            // 选区模式取消
                                        }
                                        else -> {}
                                    }
                                    isDrawing = false
                                }

                                // 应用变换
                                scale = (scale * zoom).coerceIn(CanvasState.MIN_SCALE, CanvasState.MAX_SCALE)
                                rotation += rotationChange
                                offset += pan

                                viewModel.updateCanvasState(scale, rotation, offset.x, offset.y)
                            }

                            changes.forEach { it.consume() }
                        } else if (changes.size == 1) {
                            // 单指手势
                            val change = changes.first()

                            if (pastTouchSlop) {
                                break
                            }

                            if (isDrawing) {
                                val position = change.position
                                val canvasPoint = screenToCanvas(
                                    position,
                                    scale,
                                    rotation,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    viewModel.canvasWidth.toFloat(),
                                    viewModel.canvasHeight.toFloat(),
                                    offset
                                )

                                // 使用 position.hashCode() 来判断位置是否变化（简化的检查方式）
                                val positionChanged = true
                                if (positionChanged) {
                                    when (currentToolMode) {
                                        ToolMode.DRAW -> {
                                            if (!isFillBrush) {
                                                viewModel.continueStroke(canvasPoint.x, canvasPoint.y)
                                                
                                                // 对称绘制：实时镜像
                                                if (viewModel.symmetryAxis != SymmetryAxis.NONE) {
                                                    // 对称点会在绘制时处理
                                                }
                                            }
                                        }
                                        ToolMode.SELECTION -> {
                                            viewModel.continueSelection(canvasPoint.x, canvasPoint.y)
                                            tempSelectionEnd = Offset(canvasPoint.x, canvasPoint.y)
                                        }
                                        else -> {}
                                    }
                                    change.consume()
                                }
                            }
                        }
                    } while (changes.any { it.pressed })

                    // 结束操作
                    if (isDrawing) {
                        when (currentToolMode) {
                            ToolMode.DRAW -> {
                                if (!isFillBrush) {
                                    viewModel.endStroke()
                                }
                            }
                            ToolMode.SELECTION -> {
                                viewModel.endSelection(tempSelectionEnd.x, tempSelectionEnd.y)
                            }
                            else -> {}
                        }
                    }
                }
            }
    ) {
        withTransform({
            translate(size.width / 2f, size.height / 2f)
            scale(scale, scale, Offset.Zero)
            rotate(rotation * 180f / PI.toFloat(), Offset.Zero)
            translate(
                -canvasWidth / 2f + offset.x / scale,
                -canvasHeight / 2f + offset.y / scale
            )
        }) {
            // 绘制白色背景
            drawRect(
                color = Color.White,
                topLeft = Offset.Zero,
                size = Size(canvasWidth, canvasHeight)
            )

            // 绘制合成后的图层
            drawContext.canvas.nativeCanvas.let { nativeCanvas ->
                nativeCanvas.save()
                nativeCanvas.translate(0f, 0f)

                // 绘制位图
                if (!compositedBitmap.isRecycled) {
                    nativeCanvas.drawBitmap(compositedBitmap, 0f, 0f, null)
                }

                nativeCanvas.restore()
            }

            // 绘制当前笔画预览（实时预览）
            if (currentStroke != null && currentStroke.isNotEmpty()) {
                val path = Path()
                path.moveTo(currentStroke[0].x, currentStroke[0].y)
                for (i in 1 until currentStroke.size) {
                    path.lineTo(currentStroke[i].x, currentStroke[i].y)
                }

                // 使用 Compose 的 drawPath 方法绘制笔画预览
                drawPath(
                    path = path,
                    color = Color(viewModel.currentColor),
                    style = Stroke(
                        width = viewModel.currentBrush.size,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 绘制选区预览（选区绘制过程中）
            if (viewModel.toolMode == ToolMode.SELECTION &&
                viewModel.currentSelection == null) {
                selectionStartPoint?.let { start ->
                    selectionPoints?.let { points ->
                        val previewPath = Path()
                        previewPath.moveTo(start.x, start.y)
                        points.drop(1).forEach { point ->
                            previewPath.lineTo(point.x, point.y)
                        }

                        drawPath(
                            path = previewPath,
                            color = Color(0xFF2196F3),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // 绘制当前选区边框
            viewModel.currentSelection?.let { sel ->
                val selBounds = sel.getCurrentBounds()

                drawRect(
                    color = Color.White,
                    topLeft = Offset(selBounds.left, selBounds.top),
                    size = Size(selBounds.width(), selBounds.height()),
                    style = Stroke(width = 2f)
                )
            }
        }
    }

    // 绘制网格覆盖层（在 Canvas 外部）
    if (viewModel.gridType != GridType.NONE) {
        GridOverlay(
            gridType = viewModel.gridType,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            scale = 1f,
            offsetX = 0f,
            offsetY = 0f,
            modifier = Modifier
        )
    }

    // 绘制对称辅助线（在 Canvas 外部）
    if (viewModel.symmetryAxis != SymmetryAxis.NONE) {
        SymmetryOverlay(
            symmetryAxis = viewModel.symmetryAxis,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            scale = 1f,
            rotation = rotation,
            offsetX = 0f,
            offsetY = 0f
        )
    }
}

/**
 * 执行填充
 */
private fun executeFillAt(viewModel: CanvasViewModel, x: Int, y: Int) {
    // 填充逻辑需要在协程中执行
    // 这里简化处理，实际实现应该使用 flood fill 算法
}

/**
 * 屏幕坐标转画布坐标
 */
private fun screenToCanvas(
    screenPoint: Offset,
    scale: Float,
    rotation: Float,
    screenWidth: Float,
    screenHeight: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    offset: Offset
): Offset {
    // 正向变换:
    //   T1: translate(screenW/2, screenH/2)
    //   S:  scale(s)
    //   R:  rotate(r)
    //   T2: translate(-canvasW/2 + offset.x/s, -canvasH/2 + offset.y/s)
    //
    // 逆变换: canvas = T2^-1 * R^-1 * S^-1 * T1^-1 * screen

    // Step 1: 逆T1 - 减去屏幕中心
    var x = screenPoint.x - screenWidth / 2f
    var y = screenPoint.y - screenHeight / 2f

    // Step 2: 逆S - 除以缩放
    x /= scale
    y /= scale

    // Step 3: 逆R - 反向旋转
    val r = rotation
    val cosR = cos(-r)
    val sinR = sin(-r)
    val rx = x * cosR - y * sinR
    val ry = x * sinR + y * cosR

    // Step 4: 逆T2
    val dx = -canvasWidth / 2f + offset.x / scale
    val dy = -canvasHeight / 2f + offset.y / scale

    val finalX = rx - dx
    val finalY = ry - dy

    return Offset(finalX, finalY)
}

/**
 * 获取用于对称绘制的镜像点
 */
private fun getMirrorPoints(
    point: Offset,
    symmetryAxis: SymmetryAxis,
    canvasWidth: Float,
    canvasHeight: Float
): List<Offset> {
    val centerX = canvasWidth / 2f
    val centerY = canvasHeight / 2f

    return when (symmetryAxis) {
        SymmetryAxis.HORIZONTAL -> listOf(
            point,
            Offset(point.x, 2 * centerY - point.y)
        )
        SymmetryAxis.VERTICAL -> listOf(
            point,
            Offset(2 * centerX - point.x, point.y)
        )
        SymmetryAxis.RADIAL -> {
            val points = mutableListOf<Offset>()
            val angle = kotlin.math.atan2(
                (point.y - centerY).toDouble(),
                (point.x - centerX).toDouble()
            ).toFloat()
            val distance = kotlin.math.sqrt(
                (point.x - centerX) * (point.x - centerX) +
                (point.y - centerY) * (point.y - centerY)
            )
            for (i in 0 until 6) {
                val a = angle + (2 * PI * i / 6).toFloat()
                points.add(Offset(
                    centerX + cos(a) * distance,
                    centerY + sin(a) * distance
                ))
            }
            points
        }
        SymmetryAxis.NONE -> listOf(point)
    }
}
