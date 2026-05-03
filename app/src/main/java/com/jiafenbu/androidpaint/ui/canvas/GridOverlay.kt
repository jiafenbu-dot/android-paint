package com.jiafenbu.androidpaint.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import com.jiafenbu.androidpaint.model.GridType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 网格辅助线覆盖层
 * 在画布上显示网格，帮助用户构图和精确定位
 * 
 * @param gridType 网格类型
 * @param canvasWidth 画布宽度
 * @param canvasHeight 画布高度
 * @param scale 缩放比例
 * @param offsetX X 偏移
 * @param offsetY Y 偏移
 * @param modifier 修饰符
 */
@Composable
fun GridOverlay(
    gridType: GridType,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    if (gridType == GridType.NONE) return
    
    val lineColor = Color(0xFF9E9E9E).copy(alpha = 0.4f)
    val accentColor = Color(0xFF2196F3).copy(alpha = 0.5f)
    
    Canvas(modifier = modifier) {
        val scaledWidth = canvasWidth * scale
        val scaledHeight = canvasHeight * scale
        val startX = offsetX
        val startY = offsetY
        
        when (gridType) {
            GridType.RULE_OF_THIRDS -> {
                // 三分法网格
                drawRuleOfThirdsGrid(
                    startX = startX,
                    startY = startY,
                    width = scaledWidth,
                    height = scaledHeight,
                    lineColor = lineColor,
                    accentColor = accentColor
                )
            }
            
            GridType.PERSPECTIVE -> {
                // 透视网格
                drawPerspectiveGrid(
                    startX = startX,
                    startY = startY,
                    width = scaledWidth,
                    height = scaledHeight,
                    lineColor = lineColor,
                    accentColor = accentColor
                )
            }
            
            GridType.NONE -> { /* 不绘制 */ }
        }
    }
}

/**
 * 绘制三分法网格
 */
private fun DrawScope.drawRuleOfThirdsGrid(
    startX: Float,
    startY: Float,
    width: Float,
    height: Float,
    lineColor: Color,
    accentColor: Color
) {
    // 垂直线（三等分）
    val thirdWidth = width / 3
    for (i in 1..2) {
        val x = startX + thirdWidth * i
        val isAccent = i == 1 || i == 2
        drawLine(
            color = if (isAccent) accentColor else lineColor,
            start = Offset(x, startY),
            end = Offset(x, startY + height),
            strokeWidth = if (isAccent) 1.5f else 1f
        )
    }
    
    // 水平线（三等分）
    val thirdHeight = height / 3
    for (i in 1..2) {
        val y = startY + thirdHeight * i
        val isAccent = i == 1 || i == 2
        drawLine(
            color = if (isAccent) accentColor else lineColor,
            start = Offset(startX, y),
            end = Offset(startX + width, y),
            strokeWidth = if (isAccent) 1.5f else 1f
        )
    }
    
    // 绘制交叉点（黄金点）
    for (i in 1..2) {
        for (j in 1..2) {
            val x = startX + thirdWidth * i
            val y = startY + thirdHeight * j
            drawCircle(
                color = accentColor,
                radius = 4f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * 绘制透视网格
 */
private fun DrawScope.drawPerspectiveGrid(
    startX: Float,
    startY: Float,
    width: Float,
    height: Float,
    lineColor: Color,
    accentColor: Color
) {
    // 透视网格 - 从视点放射的网格线
    val horizonY = startY + height * 0.4f // 地平线位置
    val vanishX = startX + width / 2 // 消失点 X
    val vanishY = horizonY // 消失点 Y
    
    // 绘制地平线
    drawLine(
        color = accentColor,
        start = Offset(startX, horizonY),
        end = Offset(startX + width, horizonY),
        strokeWidth = 2f
    )
    
    // 绘制从消失点放射的线条
    val numLines = 12
    for (i in 0..numLines) {
        val ratio = i.toFloat() / numLines
        val endX = startX + width * ratio
        
        drawLine(
            color = lineColor,
            start = Offset(vanishX, vanishY),
            end = Offset(endX, startY + height),
            strokeWidth = 1f
        )
    }
    
    // 绘制水平网格线（随距离变密）
    val numHorizontalLines = 8
    for (i in 1..numHorizontalLines) {
        val t = i.toFloat() / numHorizontalLines
        // 非线性分布，越远越密
        val y = horizonY + (startY + height - horizonY) * (t * t)
        
        val pathEffect = PathEffect.dashPathEffect(
            floats = floatArrayOf(10f, 5f),
            phase = 0f
        )
        
        drawLine(
            color = lineColor,
            start = Offset(startX, y),
            end = Offset(startX + width, y),
            strokeWidth = 1f,
            pathEffect = pathEffect
        )
    }
    
    // 绘制消失点
    drawCircle(
        color = accentColor,
        radius = 6f,
        center = Offset(vanishX, vanishY)
    )
    drawCircle(
        color = Color.White,
        radius = 3f,
        center = Offset(vanishX, vanishY)
    )
}

/**
 * 绘制对称网格
 */
@Composable
fun SymmetryGridOverlay(
    symmetryAxis: com.jiafenbu.androidpaint.model.SymmetryAxis,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    if (symmetryAxis == com.jiafenbu.androidpaint.model.SymmetryAxis.NONE) return
    
    val lineColor = Color(0xFFE91E63).copy(alpha = 0.3f)
    
    Canvas(modifier = modifier) {
        val scaledWidth = canvasWidth * scale
        val scaledHeight = canvasHeight * scale
        val centerX = offsetX + scaledWidth / 2
        val centerY = offsetY + scaledHeight / 2
        
        when (symmetryAxis) {
            com.jiafenbu.androidpaint.model.SymmetryAxis.HORIZONTAL -> {
                // 水平对称线
                drawLine(
                    color = lineColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = 2f
                )
            }
            
            com.jiafenbu.androidpaint.model.SymmetryAxis.VERTICAL -> {
                // 垂直对称线
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2f
                )
            }
            
            com.jiafenbu.androidpaint.model.SymmetryAxis.RADIAL -> {
                // 径向对称 - 多条线
                val numLines = 6
                for (i in 0 until numLines) {
                    val angle = (2 * PI * i / numLines).toFloat()
                    val endX = centerX + cos(angle) * size.width
                    val endY = centerY + sin(angle) * size.height
                    
                    drawLine(
                        color = lineColor,
                        start = Offset(centerX, centerY),
                        end = Offset(endX, endY),
                        strokeWidth = 2f
                    )
                }
                
                // 中心圆
                drawCircle(
                    color = lineColor,
                    radius = 20f,
                    center = Offset(centerX, centerY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
            
            com.jiafenbu.androidpaint.model.SymmetryAxis.NONE -> { /* 不绘制 */ }
        }
    }
}
