package com.jiafenbu.androidpaint.ui.colorpicker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.R
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HSV 颜色选择器
 * 包含色轮、明度滑块、最近使用颜色和当前颜色预览
 */
@Composable
fun ColorPicker(
    currentColor: Int,
    colorHistory: List<Int>,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 将 ARGB Int 转换为 HSV
    val hsv = remember(currentColor) {
        val color = Color(currentColor)
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.argb(
                (color.alpha * 255).toInt(),
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            ),
            hsvArray
        )
        hsvArray
    }
    
    var hue by remember(hsv[0]) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(hsv[1]) { mutableFloatStateOf(hsv[1]) }
    var value by remember(hsv[2]) { mutableFloatStateOf(hsv[2]) }
    
    // 计算当前颜色
    val selectedColor = Color.hsv(hue, saturation, value)
    val selectedColorInt = android.graphics.Color.HSVToColor(
        (selectedColor.alpha * 255).toInt(),
        floatArrayOf(hue, saturation, value)
    )
    
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE1A1A1A),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.color_picker),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 色轮 + 方形选择器
            HueSaturationPicker(
                hue = hue,
                saturation = saturation,
                value = value,
                onHueSaturationChange = { h, s ->
                    hue = h
                    saturation = s
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 明度滑块
            ValueSlider(
                value = value,
                hue = hue,
                saturation = saturation,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 当前颜色预览
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.current_color),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .border(2.dp, Color.White, CircleShape)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = String.format("#%08X", selectedColorInt),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 最近使用颜色
            if (colorHistory.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.recent_colors),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(10),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    items(colorHistory.take(20)) { color ->
                        ColorSwatch(
                            color = Color(color),
                            isSelected = color == currentColor,
                            onClick = {
                                // 更新 HSV 值
                                val hsvArray = FloatArray(3)
                                android.graphics.Color.colorToHSV(color, hsvArray)
                                hue = hsvArray[0]
                                saturation = hsvArray[1]
                                value = hsvArray[2]
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 确认按钮
            Button(
                onClick = {
                    onColorSelected(selectedColorInt)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.confirm))
            }
        }
    }
}

/**
 * 色相-饱和度选择器
 * 圆形色相环 + 内部方形 SV 选择器
 */
@Composable
private fun HueSaturationPicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueSaturationChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickerSize by remember { mutableStateOf(Offset.Zero) }
    val centerRadius = remember(pickerSize) {
        min(pickerSize.x, pickerSize.y) / 2 * 0.75f
    }
    val ringRadius = remember(pickerSize) {
        min(pickerSize.x, pickerSize.y) / 2 * 0.85f
    }
    
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                pickerSize = Offset(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    pickerSize = Offset(size.width.toFloat(), size.height.toFloat())
                }
                .pointerInput(hue, saturation) {
                    detectDragGestures { change, _ ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        
                        val dx = change.position.x - centerX
                        val dy = change.position.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val maxRadius = min(size.width, size.height) / 2f * 0.85f
                        
                        if (distance <= maxRadius) {
                            // 在色环内，调整色相
                            var angle = atan2(dy, dx) * 180f / PI.toFloat()
                            if (angle < 0) angle += 360f
                            onHueSaturationChange(angle, saturation)
                        }
                    }
                }
                .pointerInput(hue, saturation) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = sqrt(dx * dx + dy * dy)
                        val maxRadius = min(size.width, size.height) / 2f * 0.85f
                        
                        if (distance <= maxRadius) {
                            var angle = atan2(dy, dx) * 180f / PI.toFloat()
                            if (angle < 0) angle += 360f
                            onHueSaturationChange(angle, saturation)
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            val maxRadius = min(width, height) / 2f
            
            // 绘制色相环
            val ringWidth = maxRadius * 0.15f
            
            // 绘制色相渐变环
            for (angle in 0..360 step 1) {
                val startAngle = angle.toFloat()
                val color = Color.hsv(angle.toFloat(), 1f, 1f)
                drawArc(
                    color = color,
                    startAngle = startAngle - 90,
                    sweepAngle = 2f,
                    useCenter = true,
                    topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
                    size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2),
                    style = Stroke(width = ringWidth)
                )
            }
            
            // 绘制中心白色背景
            drawCircle(
                color = Color.White,
                radius = centerRadius,
                center = Offset(centerX, centerY)
            )
            
            // 绘制 SV 选择区域
            // 水平渐变：白到当前色相
            val hueColor = Color.hsv(hue, 1f, 1f)
            val horizontalGradient = Brush.horizontalGradient(
                colors = listOf(Color.White, hueColor),
                startX = centerX - centerRadius,
                endX = centerX + centerRadius
            )
            
            // 绘制水平渐变
            drawRect(
                brush = horizontalGradient,
                topLeft = Offset(centerX - centerRadius, centerY - centerRadius),
                size = androidx.compose.ui.geometry.Size(centerRadius * 2, centerRadius * 2)
            )
            
            // 叠加垂直渐变：透明到黑
            val verticalGradient = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = centerY - centerRadius,
                endY = centerY + centerRadius
            )
            
            drawRect(
                brush = verticalGradient,
                topLeft = Offset(centerX - centerRadius, centerY - centerRadius),
                size = androidx.compose.ui.geometry.Size(centerRadius * 2, centerRadius * 2)
            )
            
            // 绘制选择器圆圈
            val selectorX = centerX + (saturation * 2 - 1) * centerRadius
            val selectorY = centerY + (1 - value) * centerRadius
            
            // 白色边框
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 3f)
            )
            
            // 黑色边框
            drawCircle(
                color = Color.Black,
                radius = 10f,
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 1f)
            )
        }
        
        // 独立的饱和度/明度点击区域
        Box(
            modifier = Modifier
                .padding(ringWidth.toInt().dp)
                .size(centerRadius * 2.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(hue, saturation, value) {
                        detectDragGestures { change, _ ->
                            val localCenter = centerRadius
                            val dx = change.position.x - localCenter
                            val dy = change.position.y - localCenter
                            
                            val newSat = ((dx / localCenter) + 1f).coerceIn(0f, 2f) / 2f
                            val newVal = 1f - ((dy / localCenter) + 1f).coerceIn(0f, 2f) / 2f
                            
                            onHueSaturationChange(hue, newSat.coerceIn(0f, 1f))
                        }
                    }
                    .pointerInput(hue) {
                        detectTapGestures { offset ->
                            val localCenter = centerRadius
                            val dx = offset.x - localCenter
                            val dy = offset.y - localCenter
                            
                            val newSat = ((dx / localCenter) + 1f).coerceIn(0f, 2f) / 2f
                            val newVal = 1f - ((dy / localCenter) + 1f).coerceIn(0f, 2f) / 2f
                            
                            onHueSaturationChange(hue, newSat.coerceIn(0f, 1f))
                        }
                    }
            )
        }
    }
}

/**
 * 明度滑块
 */
@Composable
private fun ValueSlider(
    value: Float,
    hue: Float,
    saturation: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "明度",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color.hsv(hue, saturation, 0.5f),
                            Color.hsv(hue, saturation, 1f),
                            Color.White
                        )
                    )
                )
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier.matchParentSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * 颜色色块
 */
@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
