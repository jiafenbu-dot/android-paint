package com.jiafenbu.androidpaint.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jiafenbu.androidpaint.model.SymmetryAxis
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 对称辅助线覆盖层
 * 在画布上显示对称轴，帮助用户绘制对称图形
 * 
 * @param symmetryAxis 对称轴类型
 * @param canvasWidth 画布宽度
 * @param canvasHeight 画布高度
 * @param scale 缩放比例
 * @param rotation 旋转角度
 * @param offsetX X 偏移
 * @param offsetY Y 偏移
 * @param modifier 修饰符
 */
@Composable
fun SymmetryOverlay(
    symmetryAxis: SymmetryAxis,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    if (symmetryAxis == SymmetryAxis.NONE) return
    
    val lineColor = Color(0xFF2196F3).copy(alpha = 0.6f)
    val dashEffect = PathEffect.dashPathEffect(
        intervals = floatArrayOf(10f, 10f),
        phase = 0f
    )
    
    Canvas(modifier = modifier) {
        val centerX = offsetX + canvasWidth * scale / 2
        val centerY = offsetY + canvasHeight * scale / 2
        
        when (symmetryAxis) {
            SymmetryAxis.HORIZONTAL -> {
                // 水平对称 - 中间一条水平线
                drawLine(
                    color = lineColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }
            
            SymmetryAxis.VERTICAL -> {
                // 垂直对称 - 中间一条垂直线
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2f,
                    pathEffect = dashEffect
                )
            }
            
            SymmetryAxis.RADIAL -> {
                // 径向对称 - 多条从中心射出的线
                val lineCount = 6
                for (i in 0 until lineCount) {
                    val angle = (2 * PI * i / lineCount).toFloat() + rotation
                    val endX = centerX + cos(angle) * size.width
                    val endY = centerY + sin(angle) * size.height
                    
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )
                }
                
                // 中心点
                drawCircle(
                    color = lineColor,
                    radius = 6f,
                    center = Offset(centerX, centerY)
                )
            }
            
            SymmetryAxis.NONE -> { /* 不绘制 */ }
        }
    }
}

/**
 * 获取对称镜像点
 * 计算给定点的对称位置
 * 
 * @param point 原点
 * @param symmetryAxis 对称轴类型
 * @param centerX 对称中心 X
 * @param centerY 对称中心 Y
 * @return 镜像点列表（如果有多个对称轴，可能返回多个点）
 */
fun getSymmetryPoints(
    point: Offset,
    symmetryAxis: SymmetryAxis,
    centerX: Float,
    centerY: Float
): List<Offset> {
    return when (symmetryAxis) {
        SymmetryAxis.HORIZONTAL -> {
            // 关于水平中线对称
            listOf(
                point,
                Offset(point.x, 2 * centerY - point.y)
            )
        }
        
        SymmetryAxis.VERTICAL -> {
            // 关于垂直中线对称
            listOf(
                point,
                Offset(2 * centerX - point.x, point.y)
            )
        }
        
        SymmetryAxis.RADIAL -> {
            // 6 重径向对称
            val points = mutableListOf<Offset>()
            val baseAngle = kotlin.math.atan2(
                (point.y - centerY).toDouble(),
                (point.x - centerX).toDouble()
            ).toFloat()
            val distance = kotlin.math.sqrt(
                (point.x - centerX) * (point.x - centerX) +
                (point.y - centerY) * (point.y - centerY)
            )
            
            for (i in 0 until 6) {
                val angle = baseAngle + (2 * PI * i / 6).toFloat()
                points.add(
                    Offset(
                        centerX + cos(angle) * distance,
                        centerY + sin(angle) * distance
                    )
                )
            }
            points
        }
        
        SymmetryAxis.NONE -> listOf(point)
    }
}

/**
 * 绘制对称辅助线（仅在绘画时显示）
 */
@Composable
fun SymmetryGuideLines(
    symmetryAxis: SymmetryAxis,
    points: List<Offset>,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    if (symmetryAxis == SymmetryAxis.NONE || points.isEmpty()) return
    
    val lineColor = Color(0xFF2196F3).copy(alpha = 0.4f)
    
    Canvas(modifier = modifier) {
        val centerX = offsetX + canvasWidth * scale / 2
        val centerY = offsetY + canvasHeight * scale / 2
        
        // 绘制从笔触到对称点的连线
        when (symmetryAxis) {
            SymmetryAxis.HORIZONTAL -> {
                points.forEach { point ->
                    val mirrored = Offset(point.x, 2 * centerY - point.y)
                    drawLine(
                        color = lineColor,
                        start = Offset(point.x * scale + offsetX, point.y * scale + offsetY),
                        end = Offset(mirrored.x * scale + offsetX, mirrored.y * scale + offsetY),
                        strokeWidth = 1f
                    )
                }
            }
            
            SymmetryAxis.VERTICAL -> {
                points.forEach { point ->
                    val mirrored = Offset(2 * centerX - point.x, point.y)
                    drawLine(
                        color = lineColor,
                        start = Offset(point.x * scale + offsetX, point.y * scale + offsetY),
                        end = Offset(mirrored.x * scale + offsetX, mirrored.y * scale + offsetY),
                        strokeWidth = 1f
                    )
                }
            }
            
            SymmetryAxis.RADIAL -> {
                points.forEach { point ->
                    val transformedX = point.x * scale + offsetX
                    val transformedY = point.y * scale + offsetY
                    
                    for (i in 1 until 6) {
                        val angle = (2 * PI * i / 6).toFloat()
                        val dx = (transformedX - centerX) * cos(angle) - (transformedY - centerY) * sin(angle)
                        val dy = (transformedX - centerX) * sin(angle) + (transformedY - centerY) * cos(angle)
                        
                        drawLine(
                            color = lineColor,
                            start = Offset(transformedX, transformedY),
                            end = Offset(centerX + dx, centerY + dy),
                            strokeWidth = 1f
                        )
                    }
                }
            }
            
            SymmetryAxis.NONE -> { /* 不绘制 */ }
        }
    }
}
