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
 * 笔刷参数设置面板（紧凑横排布局）
 * 显示当前笔刷名称和可调参数滑块
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
        // 新增笔刷
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH, BrushType.PIXEL,
        BrushType.NEON, BrushType.PATTERN_BRUSH, BrushType.HAIR,
        // 第二批新增笔刷
        BrushType.CHARCOAL, BrushType.FOUNTAIN_PEN, BrushType.SPONGE,
        BrushType.RIBBON, BrushType.STAMP, BrushType.GLITTER
    )
    val showOpacity = brushType in listOf(
        BrushType.PENCIL, BrushType.INK_PEN, BrushType.WATERCOLOR,
        BrushType.MARKER, BrushType.SPRAY, BrushType.OIL_BRUSH,
        BrushType.CRAYON, BrushType.SMUDGE,
        // 新增笔刷
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH, BrushType.PIXEL,
        BrushType.NEON, BrushType.PATTERN_BRUSH, BrushType.HAIR,
        // 第二批新增笔刷
        BrushType.CHARCOAL, BrushType.FOUNTAIN_PEN, BrushType.SPONGE,
        BrushType.RIBBON, BrushType.STAMP, BrushType.GLITTER
    )
    val showSpacing = brushType in listOf(
        BrushType.PENCIL, BrushType.INK_PEN, BrushType.WATERCOLOR,
        BrushType.MARKER, BrushType.SPRAY, BrushType.OIL_BRUSH,
        BrushType.CRAYON, BrushType.BLUR, BrushType.ERASER,
        // 新增笔刷
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH, BrushType.PIXEL,
        BrushType.NEON, BrushType.PATTERN_BRUSH, BrushType.HAIR,
        // 第二批新增笔刷
        BrushType.CHARCOAL, BrushType.FOUNTAIN_PEN, BrushType.SPONGE,
        BrushType.RIBBON, BrushType.STAMP, BrushType.GLITTER
    )
    val showJitter = brushType in listOf(
        BrushType.PENCIL, BrushType.WATERCOLOR,
        BrushType.SPRAY, BrushType.OIL_BRUSH, BrushType.CRAYON,
        // 新增笔刷
        BrushType.CALLIGRAPHY, BrushType.AIRBRUSH,
        BrushType.PATTERN_BRUSH, BrushType.HAIR,
        // 第二批新增笔刷
        BrushType.CHARCOAL, BrushType.SPONGE, BrushType.RIBBON, BrushType.GLITTER
    )

    Surface(
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xEE1A1A1A),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = brushType.icon,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = brushType.displayName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

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

            Spacer(modifier = Modifier.height(12.dp))

            // 参数行：紧凑横排布局
            // 每行：参数名(36dp) + Slider(权重1) + 数值(36dp)

            if (showSize) {
                CompactSliderRow(
                    label = "大小",
                    value = localSize,
                    valueRange = 1f..100f,
                    onValueChange = { localSize = it },
                    onValueChangeFinished = { onSizeChange(localSize) },
                    valueText = "${localSize.toInt()}",
                    sliderColor = Color(0xFF6200EE)
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
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
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
 * 格式：标签(36dp) + Slider + 数值(36dp)
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
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp)
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
            fontSize = 11.sp,
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
    val previewSize = 40.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
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
