package com.jiafenbu.androidpaint.ui.text

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.font.FontItem
import com.jiafenbu.androidpaint.font.FontManager

/**
 * 字体选择对话框
 */
@Composable
fun FontPickerDialog(
    fontManager: FontManager,
    selectedFont: String,
    onFontSelected: (String, String?) -> Unit,
    onImportFont: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fonts by remember { mutableStateOf(fontManager.getAllFonts()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择字体")
                IconButton(onClick = onImportFont) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "导入字体",
                        tint = Color(0xFF2196F3)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                // 系统字体分组
                item {
                    Text(
                        text = "系统字体",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(fonts.filter { it.filePath == null }) { font ->
                    FontItem(
                        font = font,
                        isSelected = font.name == selectedFont,
                        onClick = {
                            onFontSelected(font.name, font.filePath)
                            onDismiss()
                        }
                    )
                }

                // 自定义字体分组
                val customFonts = fonts.filter { it.filePath != null }
                if (customFonts.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "自定义字体",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(customFonts) { font ->
                        FontItem(
                            font = font,
                            isSelected = font.name == selectedFont,
                            onClick = {
                                onFontSelected(font.name, font.filePath)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        modifier = modifier
    )
}

/**
 * 字体项
 */
@Composable
private fun FontItem(
    font: FontItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = font.displayName,
                    color = if (isSelected) Color(0xFF2196F3) else Color.White,
                    fontSize = 16.sp
                )
                if (font.filePath != null) {
                    Text(
                        text = "自定义字体",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // 预览文字
            Text(
                text = "Aa",
                color = if (isSelected) Color(0xFF2196F3) else Color.White,
                fontSize = 20.sp,
                fontFamily = FontFamily(font.fontface)
            )
        }
    }
}

/**
 * 文字预览组件
 */
@Composable
fun TextPreview(
    text: String,
    fontFamily: String,
    fontSize: Float,
    isBold: Boolean,
    isItalic: Boolean,
    textColor: Int,
    modifier: Modifier = Modifier
) {
    val fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
    val fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (text.isEmpty()) "预览文字" else text,
            color = Color(textColor),
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle
        )
    }
}
