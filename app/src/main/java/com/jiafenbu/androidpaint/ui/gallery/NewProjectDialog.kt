package com.jiafenbu.androidpaint.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * 新建项目对话框
 * 允许用户输入项目名称、选择画布尺寸
 */
@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, width: Int, height: Int) -> Unit
) {
    var projectName by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(CanvasPreset.PHONE_WALLPAPER) }
    var customWidth by remember { mutableStateOf("1080") }
    var customHeight by remember { mutableStateOf("1920") }
    var showCustomInput by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "新建项目",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 项目名称输入
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("项目名称") },
                    placeholder = { Text("输入项目名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 画布尺寸选择
                Text(
                    text = "画布尺寸",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 预设尺寸
                CanvasPreset.values().forEach { preset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedPreset == preset && !showCustomInput,
                                onClick = {
                                    selectedPreset = preset
                                    showCustomInput = false
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPreset == preset && !showCustomInput,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${preset.width} × ${preset.height}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 自定义选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = showCustomInput,
                            onClick = { showCustomInput = true },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = showCustomInput,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "自定义尺寸",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // 自定义尺寸输入
                if (showCustomInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customWidth,
                            onValueChange = { customWidth = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("宽") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text("×")
                        OutlinedTextField(
                            value = customHeight,
                            onValueChange = { customHeight = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("高") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val name = projectName.ifBlank { "未命名作品" }
                    val width: Int
                    val height: Int
                    
                    if (showCustomInput) {
                        width = customWidth.toIntOrNull()?.coerceIn(100, 8192) ?: 1080
                        height = customHeight.toIntOrNull()?.coerceIn(100, 8192) ?: 1920
                    } else {
                        width = selectedPreset.width
                        height = selectedPreset.height
                    }
                    
                    onConfirm(name, width, height)
                }
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 画布预设尺寸
 */
enum class CanvasPreset(
    val displayName: String,
    val width: Int,
    val height: Int
) {
    PHONE_WALLPAPER("手机壁纸", 1080, 1920),
    SQUARE("方形", 2048, 2048),
    UHD_4K("4K", 3840, 2160),
    A4("A4", 2480, 3508)
}

/**
 * 快速新建项目卡片
 * 用于在画廊空状态时快速创建
 */
@Composable
fun QuickCreateCard(
    preset: CanvasPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${preset.width} × ${preset.height}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
