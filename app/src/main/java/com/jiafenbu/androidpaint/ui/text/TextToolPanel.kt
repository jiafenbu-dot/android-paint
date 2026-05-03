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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.model.TextAlignment
import com.jiafenbu.androidpaint.model.TextLayerModel

/**
 * 文字工具面板
 * 用于创建和编辑文字
 */
@Composable
fun TextToolPanel(
    textModel: TextLayerModel,
    onTextChange: (TextLayerModel) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentText by remember { mutableStateOf(textModel.text) }
    var currentFontSize by remember { mutableFloatStateOf(textModel.fontSize) }
    var currentFontFamily by remember { mutableStateOf(textModel.fontFamily) }
    var isBold by remember { mutableStateOf(textModel.isBold) }
    var isItalic by remember { mutableStateOf(textModel.isItalic) }
    var letterSpacing by remember { mutableFloatStateOf(textModel.letterSpacing) }
    var lineSpacing by remember { mutableFloatStateOf(textModel.lineSpacing) }
    var textColor by remember { mutableIntStateOf(textModel.textColor) }
    var alignment by remember { mutableStateOf(textModel.alignment) }
    var strokeEnabled by remember { mutableStateOf(textModel.strokeEnabled) }
    var strokeWidth by remember { mutableFloatStateOf(textModel.strokeWidth) }
    var strokeColor by remember { mutableIntStateOf(textModel.strokeColor) }
    var shadowEnabled by remember { mutableStateOf(textModel.shadowEnabled) }
    var shadowOffsetX by remember { mutableFloatStateOf(textModel.shadowOffsetX) }
    var shadowOffsetY by remember { mutableFloatStateOf(textModel.shadowOffsetY) }
    var shadowBlurRadius by remember { mutableFloatStateOf(textModel.shadowBlurRadius) }
    var shadowColor by remember { mutableIntStateOf(textModel.shadowColor) }

    // 显示颜色选择器
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf("fill") }

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
                Text(
                    text = "文字工具",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = Color.Gray)
                    }
                    TextButton(onClick = {
                        val updatedModel = textModel.copy(
                            text = currentText,
                            fontFamily = currentFontFamily,
                            fontSize = currentFontSize,
                            isBold = isBold,
                            isItalic = isItalic,
                            letterSpacing = letterSpacing,
                            lineSpacing = lineSpacing,
                            textColor = textColor,
                            alignment = alignment,
                            strokeEnabled = strokeEnabled,
                            strokeWidth = strokeWidth,
                            strokeColor = strokeColor,
                            shadowEnabled = shadowEnabled,
                            shadowOffsetX = shadowOffsetX,
                            shadowOffsetY = shadowOffsetY,
                            shadowBlurRadius = shadowBlurRadius,
                            shadowColor = shadowColor
                        )
                        onTextChange(updatedModel)
                        onConfirm()
                    }) {
                        Text("确定", color = Color(0xFF2196F3))
                    }
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
                value = currentText,
                onValueChange = { currentText = it },
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
                        if (currentText.isEmpty()) {
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
                selectedFont = currentFontFamily,
                onFontSelected = { currentFontFamily = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 字号滑块
            Text(
                text = "字号: ${currentFontSize.toInt()}sp",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = currentFontSize,
                onValueChange = { currentFontSize = it },
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
                    selected = isBold,
                    onClick = { isBold = !isBold },
                    label = { Text("粗体", fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = isItalic,
                    onClick = { isItalic = !isItalic },
                    label = { Text("斜体", fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 字间距/行间距
            Text(
                text = "字间距: ${String.format("%.1f", letterSpacing)}",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = letterSpacing,
                onValueChange = { letterSpacing = it },
                valueRange = 0f..10f,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "行间距: ${String.format("%.1f", lineSpacing)}x",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = lineSpacing,
                onValueChange = { lineSpacing = it },
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E1E))
                    .clickable {
                        colorPickerTarget = "fill"
                        showColorPicker = true
                    }
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(textColor))
                        .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("#%08X", textColor),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

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
                    isSelected = alignment == TextAlignment.LEFT,
                    onClick = { alignment = TextAlignment.LEFT },
                    modifier = Modifier.weight(1f)
                )
                AlignmentButton(
                    icon = Icons.Default.FormatAlignCenter,
                    isSelected = alignment == TextAlignment.CENTER,
                    onClick = { alignment = TextAlignment.CENTER },
                    modifier = Modifier.weight(1f)
                )
                AlignmentButton(
                    icon = Icons.Default.FormatAlignRight,
                    isSelected = alignment == TextAlignment.RIGHT,
                    onClick = { alignment = TextAlignment.RIGHT },
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
                    checked = strokeEnabled,
                    onCheckedChange = { strokeEnabled = it }
                )
            }

            if (strokeEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "描边宽度: ${strokeWidth.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = strokeWidth,
                    onValueChange = { strokeWidth = it },
                    valueRange = 1f..20f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable {
                            colorPickerTarget = "stroke"
                            showColorPicker = true
                        }
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(strokeColor))
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "描边颜色: ${String.format("#%08X", strokeColor)}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
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
                    checked = shadowEnabled,
                    onCheckedChange = { shadowEnabled = it }
                )
            }

            if (shadowEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "X偏移: ${shadowOffsetX.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = shadowOffsetX,
                    onValueChange = { shadowOffsetX = it },
                    valueRange = -20f..20f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Y偏移: ${shadowOffsetY.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = shadowOffsetY,
                    onValueChange = { shadowOffsetY = it },
                    valueRange = -20f..20f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "模糊半径: ${shadowBlurRadius.toInt()}",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Slider(
                    value = shadowBlurRadius,
                    onValueChange = { shadowBlurRadius = it },
                    valueRange = 0f..30f,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable {
                            colorPickerTarget = "shadow"
                            showColorPicker = true
                        }
                        .padding(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(shadowColor))
                            .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "阴影颜色",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 颜色选择对话框
    if (showColorPicker) {
        SimpleColorPickerDialog(
            initialColor = when (colorPickerTarget) {
                "fill" -> textColor
                "stroke" -> strokeColor
                else -> shadowColor
            },
            onColorSelected = { color ->
                when (colorPickerTarget) {
                    "fill" -> textColor = color
                    "stroke" -> strokeColor = color
                    else -> shadowColor = color
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

/**
 * 对齐按钮
 */
@Composable
private fun AlignmentButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF2196F3) else Color(0xFF1E1E1E))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else Color.Gray
        )
    }
}

/**
 * 字体选择按钮
 */
@Composable
private fun FontPickerButton(
    selectedFont: String,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFont,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2D2D2D))
        ) {
            listOf("默认", "宋体", "黑体", "楷体", "等线", "serif", "monospace").forEach { font ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = font,
                            color = if (font == selectedFont) Color(0xFF2196F3) else Color.White
                        )
                    },
                    onClick = {
                        onFontSelected(font)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * 简单的颜色选择对话框
 */
@Composable
private fun SimpleColorPickerDialog(
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }

    // 从初始颜色计算 HSV
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor, hsv)
    hue = hsv[0]
    saturation = hsv[1]
    value = hsv[2]

    val currentColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色") },
        text = {
            Column {
                // 颜色预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(currentColor))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 色相滑块
                Text("色相", fontSize = 12.sp)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(currentColor),
                        activeTrackColor = Color(currentColor)
                    )
                )

                // 饱和度滑块
                Text("饱和度", fontSize = 12.sp)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f
                )

                // 明度滑块
                Text("明度", fontSize = 12.sp)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
