package com.jiafenbu.androidpaint.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jiafenbu.androidpaint.R

/**
 * 顶部操作栏
 * 深灰色背景，左侧关闭按钮，中间偏右撤销/重做，右侧图层和设置
 * 设置按钮点击后弹出菜单，包含导出、笔刷库、色板、参考图等更多选项
 */
@Composable
fun TopActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onCloseClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onLayersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧：关闭按钮
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }

        // 右侧：撤销、重做、图层、设置
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 撤销按钮
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.undo),
                    tint = if (canUndo) Color.White else Color.Gray
                )
            }

            // 重做按钮
            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.redo),
                    tint = if (canRedo) Color.White else Color.Gray
                )
            }

            // 图层按钮
            IconButton(
                onClick = onLayersClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = stringResource(R.string.layers),
                    tint = Color.White
                )
            }

            // 设置/齿轮按钮
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = Color.White
                )
            }
        }
    }
}
