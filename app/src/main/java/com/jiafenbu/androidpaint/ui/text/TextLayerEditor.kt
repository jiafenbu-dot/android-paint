package com.jiafenbu.androidpaint.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.model.LayerModel
import com.jiafenbu.androidpaint.model.LayerType
import com.jiafenbu.androidpaint.model.TextAlignment
import com.jiafenbu.androidpaint.model.TextLayerModel

/**
 * 文字图层编辑器
 * 用于编辑已存在的文字图层
 */
@Composable
fun TextLayerEditor(
    layer: LayerModel,
    currentTextModel: TextLayerModel,
    onTextChange: (TextLayerModel) -> Unit,
    onRasterize: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var textModel by remember { mutableStateOf(currentTextModel) }

    Surface(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(0.8f),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2D2D2D).copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = layer.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 文字输入框
            Text(
                text = "文字内容",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            BasicTextField(
                value = textModel.text,
                onValueChange = { textModel = textModel.copy(text = it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(8.dp),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                cursorBrush = SolidColor(Color(0xFF2196F3)),
                decorationBox = { innerTextField ->
                    Box {
                        if (textModel.text.isEmpty()) {
                            Text(
                                text = "请输入文字...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 字体选择
            Text(
                text = "字体",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            FontPickerButton(
                selectedFont = textModel.fontFamily,
                onFontSelected = { textModel = textModel.copy(fontFamily = it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 字号滑块
            Text(
                text = "字号: ${textModel.fontSize.toInt()}sp",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = textModel.fontSize,
                onValueChange = { textModel = textModel.copy(fontSize = it) },
                valueRange = 8f..200f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 粗体/斜体
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = textModel.isBold,
                    onClick = { textModel = textModel.copy(isBold = !textModel.isBold) },
                    label = {
                        Text(
                            "粗体",
                            fontWeight = if (textModel.isBold) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = textModel.isItalic,
                    onClick = { textModel = textModel.copy(isItalic = !textModel.isItalic) },
                    label = {
                        Text(
                            "斜体",
                            fontStyle = if (textModel.isItalic) FontStyle.Italic else FontStyle.Normal
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 字间距/行间距
            Text(
                text = "字间距: ${String.format("%.1f", textModel.letterSpacing)}",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = textModel.letterSpacing,
                onValueChange = { textModel = textModel.copy(letterSpacing = it) },
                valueRange = 0f..10f,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "行间距: ${String.format("%.1f", textModel.lineSpacing)}x",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = textModel.lineSpacing,
                onValueChange = { textModel = textModel.copy(lineSpacing = it) },
                valueRange = 0.5f..3f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 文字颜色
            Text(
                text = "文字颜色",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            ColorPreview(
                color = textModel.textColor,
                label = String.format("#%08X", textModel.textColor),
                onClick = { /* TODO: 打开颜色选择器 */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 对齐方式
            Text(
                text = "对齐方式",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlignmentButton(
                    icon = Icons.Default.FormatAlignLeft,
                    isSelected = textModel.alignment == TextAlignment.LEFT,
                    onClick = { textModel = textModel.copy(alignment = TextAlignment.LEFT) },
                    modifier = Modifier.weight(1f)
                )
                AlignmentButton(
                    icon = Icons.Default.FormatAlignCenter,
                    isSelected = textModel.alignment == TextAlignment.CENTER,
                    onClick = { textModel = textModel.copy(alignment = TextAlignment.CENTER) },
                    modifier = Modifier.weight(1f)
                )
                AlignmentButton(
                    icon = Icons.Default.FormatAlignRight,
                    isSelected = textModel.alignment == TextAlignment.RIGHT,
                    onClick = { textModel = textModel.copy(alignment = TextAlignment.RIGHT) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 描边设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "文字描边",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Switch(
                    checked = textModel.strokeEnabled,
                    onCheckedChange = { textModel = textModel.copy(strokeEnabled = it) }
                )
            }

            if (textModel.strokeEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "描边宽度: ${textModel.strokeWidth.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = textModel.strokeWidth,
                    onValueChange = { textModel = textModel.copy(strokeWidth = it) },
                    valueRange = 1f..20f,
                    modifier = Modifier.fillMaxWidth()
                )

                ColorPreview(
                    color = textModel.strokeColor,
                    label = "描边颜色",
                    onClick = { /* TODO: 打开颜色选择器 */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 阴影设置
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "文字阴影",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Switch(
                    checked = textModel.shadowEnabled,
                    onCheckedChange = { textModel = textModel.copy(shadowEnabled = it) }
                )
            }

            if (textModel.shadowEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "X偏移: ${textModel.shadowOffsetX.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = textModel.shadowOffsetX,
                    onValueChange = { textModel = textModel.copy(shadowOffsetX = it) },
                    valueRange = -20f..20f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Y偏移: ${textModel.shadowOffsetY.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = textModel.shadowOffsetY,
                    onValueChange = { textModel = textModel.copy(shadowOffsetY = it) },
                    valueRange = -20f..20f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "模糊半径: ${textModel.shadowBlurRadius.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = textModel.shadowBlurRadius,
                    onValueChange = { textModel = textModel.copy(shadowBlurRadius = it) },
                    valueRange = 0f..30f,
                    modifier = Modifier.fillMaxWidth()
                )

                ColorPreview(
                    color = textModel.shadowColor,
                    label = "阴影颜色",
                    onClick = { /* TODO: 打开颜色选择器 */ }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onTextChange(textModel)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("应用")
                }

                OutlinedButton(
                    onClick = onRasterize,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Transform,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("光栅化")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 提示信息
            Text(
                text = "提示：光栅化后文字将变为普通图层，无法再编辑文字属性",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 颜色预览组件
 */
@Composable
private fun ColorPreview(
    color: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(color))
                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}
