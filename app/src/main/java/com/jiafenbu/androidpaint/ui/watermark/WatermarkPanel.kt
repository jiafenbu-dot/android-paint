package com.jiafenbu.androidpaint.ui.watermark

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.model.WatermarkConfig
import com.jiafenbu.androidpaint.model.WatermarkTileMode
import com.jiafenbu.androidpaint.model.WatermarkType

/**
 * 水印设置面板
 */
@Composable
fun WatermarkPanel(
    watermarkConfig: WatermarkConfig?,
    onWatermarkChange: (WatermarkConfig) -> Unit,
    onDismiss: () -> Unit,
    onImportImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var config by remember { mutableStateOf(watermarkConfig ?: WatermarkConfig.create()) }

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
                    text = "水印设置",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 启用/禁用开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "启用水印",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { config = config.copy(isEnabled = it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 水印类型选择
            Text(
                text = "水印类型",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WatermarkTypeButton(
                    icon = Icons.Default.TextFields,
                    label = "文字",
                    isSelected = config.type == WatermarkType.TEXT,
                    onClick = { config = config.copy(type = WatermarkType.TEXT) },
                    modifier = Modifier.weight(1f)
                )
                WatermarkTypeButton(
                    icon = Icons.Default.Image,
                    label = "图片",
                    isSelected = config.type == WatermarkType.IMAGE,
                    onClick = { config = config.copy(type = WatermarkType.IMAGE) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 根据类型显示不同选项
            when (config.type) {
                WatermarkType.TEXT -> {
                    // 文字水印设置
                    Text(
                        text = "水印文字",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = config.text,
                        onValueChange = { config = config.copy(text = it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入水印文字") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 字号
                    Text(
                        text = "字号: ${config.fontSize.toInt()}",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = config.fontSize,
                        onValueChange = { config = config.copy(fontSize = it) },
                        valueRange = 8f..72f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 颜色
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
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(config.fontColor))
                                .border(1.dp, Color.White, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format("#%08X", config.fontColor),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                WatermarkType.IMAGE -> {
                    // 图片水印设置
                    Text(
                        text = "图片水印",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (config.imageBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E1E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onImportImage,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("从相册选择图片")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 缩放
                    Text(
                        text = "缩放: ${String.format("%.1f", config.scale)}x",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = config.scale,
                        onValueChange = { config = config.copy(scale = it) },
                        valueRange = 0.1f..3f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 透明度
            Text(
                text = "透明度: ${(config.opacity * 100).toInt()}%",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = config.opacity,
                onValueChange = { config = config.copy(opacity = it) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 平铺模式
            Text(
                text = "平铺模式",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TileModeOption(
                    icon = Icons.Default.Filter1,
                    label = "单个放置",
                    description = "在指定位置放置单个水印",
                    isSelected = config.tileMode == WatermarkTileMode.SINGLE,
                    onClick = { config = config.copy(tileMode = WatermarkTileMode.SINGLE) }
                )
                TileModeOption(
                    icon = Icons.Default.OtherHouses,
                    label = "对角线平铺",
                    description = "斜向重复铺满整张画布",
                    isSelected = config.tileMode == WatermarkTileMode.DIAGONAL,
                    onClick = { config = config.copy(tileMode = WatermarkTileMode.DIAGONAL) }
                )
                TileModeOption(
                    icon = Icons.Default.GridOn,
                    label = "网格平铺",
                    description = "水平垂直重复排列",
                    isSelected = config.tileMode == WatermarkTileMode.GRID,
                    onClick = { config = config.copy(tileMode = WatermarkTileMode.GRID) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 旋转角度
            Text(
                text = "旋转角度: ${config.rotation.toInt()}°",
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = config.rotation,
                onValueChange = { config = config.copy(rotation = it) },
                valueRange = -180f..180f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 应用按钮
            Button(
                onClick = {
                    onWatermarkChange(config)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("应用水印")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 水印类型按钮
 */
@Composable
private fun WatermarkTypeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.2f) else Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2196F3) else Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = if (isSelected) Color(0xFF2196F3) else Color.White,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 平铺模式选项
 */
@Composable
private fun TileModeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.2f) else Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF2196F3)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2196F3) else Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    color = if (isSelected) Color(0xFF2196F3) else Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
