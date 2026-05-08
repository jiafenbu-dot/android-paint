package com.jiafenbu.androidpaint.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.brush.BrushDescriptor
import com.jiafenbu.androidpaint.brush.BrushType

/**
 * 笔刷参数设置面板（适配左侧工具栏右侧弹出布局）
 * 显示当前笔刷名称和可调参数滑块
 * 面板紧贴左侧工具栏右侧弹出
 */
@Composable
fun BrushSettingsPanel(
    brushDescriptor: BrushDescriptor,
    onSizeChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onSpacingChange: (Float) -> Unit,
    onJitterChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var localSize by remember(brushDescriptor.size) { mutableFloatStateOf(brushDescriptor.size) }
    var localOpacity by remember(brushDescriptor.opacity) { mutableFloatStateOf(brushDescriptor.opacity) }
    var localSpacing by remember(brushDescriptor.spacing) { mutableFloatStateOf(brushDescriptor.spacing) }
    var localJitter by remember(brushDescriptor.jitter) { mutableFloatStateOf(brushDescriptor.jitter) }

    val brushType = brushDescriptor.type

    // 根据笔刷类型决定哪些参数可见
    val showSize = brushType in listOf(
        BrushType.PENCIL, BrushType.INK_PEN, BrushType.WATERCOLOR,
        BrushType.MARKER, BrushType.SPRAY, BrushType.OIL_BRUSH,
        BrushType.CRAYON, BrushType.BLUR, BrushType.SMUDGE, BrushType.ERASER,
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH, BrushType.PIXEL,
        BrushType.NEON, BrushType.PATTERN_BRUSH, BrushType.HAIR,
        BrushType.CHARCOAL, BrushType.FOUNTAIN_PEN, BrushType.SPONGE,
        BrushType.RIBBON, BrushType.STAMP, BrushType.GLITTER
    )
    val showOpacity = brushType in listOf(
        BrushType.PENCIL, BrushType.INK_PEN, BrushType.WATERCOLOR,
        BrushType.MARKER, BrushType.SPRAY, BrushType.OIL_BRUSH,
        BrushType.CRAYON, BrushType.SMUDGE,
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH, BrushType.PIXEL,
        BrushType.NEON, BrushType.PATTERN_BRUSH, BrushType.HAIR,
        BrushType.CHARCOAL, BrushType.FOUNTAIN_PEN, BrushType.SPONGE,
        BrushType.RIBBON, BrushType.STAMP, BrushType.GLITTER
    )
    val showSpacing = brushType in listOf(
        BrushType.PENCIL, BrushType.INK_PEN, BrushType.WATERCOLOR,
        BrushType.MARKER, BrushType.SPRAY, BrushType.OIL_BRUSH,
        BrushType.CRAYON, BrushType.BLUR, BrushType.ERASER,
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH, BrushType.PIXEL,
        BrushType.NEON, BrushType.PATTERN_BRUSH, BrushType.HAIR,
        BrushType.CHARCOAL, BrushType.FOUNTAIN_PEN, BrushType.SPONGE,
        BrushType.RIBBON, BrushType.STAMP, BrushType.GLITTER
    )
    val showJitter = brushType in listOf(
        BrushType.PENCIL, BrushType.WATERCOLOR,
        BrushType.SPRAY, BrushType.OIL_BRUSH, BrushType.CRAYON,
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH,
        BrushType.PATTERN_BRUSH, BrushType.HAIR,
        BrushType.CHARCOAL, BrushType.SPONGE, BrushType.RIBBON, BrushType.GLITTER
    )

    Surface(
        modifier = modifier.width(240.dp),
        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
        color = Color(0xEE1A1A1A),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题栏：笔刷图标 + 名称 + 关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 笔刷图标
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = brushType.icon,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = brushType.displayName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 参数滑块行

            if (showSize) {
                CompactSliderRow(
                    label = "大小",
                    value = localSize,
                    valueRange = 1f..100f,
                    onValueChange = { localSize = it },
                    onValueChangeFinished = { onSizeChange(localSize) },
                    valueText = "${localSize.toInt()}",
                    sliderColor = Color(0xFF4A90D9)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (showOpacity) {
                CompactSliderRow(
                    label = "透明度",
                    value = localOpacity,
                    valueRange = 0.01f..1f,
                    onValueChange = { localOpacity = it },
                    onValueChangeFinished = { onOpacityChange(localOpacity) },
                    valueText = "${(localOpacity * 100).toInt()}%",
                    sliderColor = Color(0xFF03DAC5)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (showSpacing) {
                CompactSliderRow(
                    label = "间距",
                    value = localSpacing,
                    valueRange = 0.01f..1f,
                    onValueChange = { localSpacing = it },
                    onValueChangeFinished = { onSpacingChange(localSpacing) },
                    valueText = String.format("%.0f%%", localSpacing * 100),
                    sliderColor = Color(0xFFFF9800)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (showJitter) {
                CompactSliderRow(
                    label = "抖动",
                    value = localJitter,
                    valueRange = 0f..1f,
                    onValueChange = { localJitter = it },
                    onValueChangeFinished = { onJitterChange(localJitter) },
                    valueText = "${(localJitter * 100).toInt()}%",
                    sliderColor = Color(0xFFE91E63)
                )
            }

            // 填充笔提示文字
            if (brushType == BrushType.FILL) {
                Text(
                    text = "点击画布填充相同颜色区域",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 预览圆
            BrushPreview(
                brushDescriptor = brushDescriptor.copy(
                    size = if (showSize) localSize else brushDescriptor.size,
                    opacity = if (showOpacity) localOpacity else brushDescriptor.opacity
                )
            )
        }
    }
}

/**
 * 紧凑滑块行
 * 格式：标签(32dp) + Slider + 数值(36dp)
 */
@Composable
private fun CompactSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueText: String,
    sliderColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 参数名
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            modifier = Modifier.width(32.dp)
        )

        // 滑块
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = sliderColor,
                activeTrackColor = sliderColor,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )

        // 数值
        Text(
            text = valueText,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp),
            maxLines = 1
        )
    }
}

/**
 * 笔刷预览圆
 */
@Composable
fun BrushPreview(
    brushDescriptor: BrushDescriptor,
    color: Int = 0xFF000000.toInt(),
    modifier: Modifier = Modifier
) {
    val previewSize = 36.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // 预览圆：展示笔刷大小和透明度
        Box(
            modifier = Modifier
                .size(previewSize)
                .clip(CircleShape)
                .background(
                    Color(color).copy(
                        alpha = brushDescriptor.opacity.coerceIn(0.1f, 1f)
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
    }
}
