package com.jiafenbu.androidpaint.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.R
import com.jiafenbu.androidpaint.brush.BrushType

/**
 * 顶部操作栏
 * 深灰色背景，左侧撤销/重做，右侧笔刷库/图层/导出
 * 横跨左侧工具栏右侧的剩余宽度
 */
@Composable
fun TopActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    projectName: String,
    currentBrushType: BrushType,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBrushLibraryClick: () -> Unit,
    onLayersClick: () -> Unit,
    onExportClick: () -> Unit,
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
        // 左侧：撤销、重做
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
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
        }

        // 中间：项目名称
        Text(
            text = projectName,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        // 右侧：笔刷库、图层、导出
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 笔刷库按钮
            IconButton(
                onClick = onBrushLibraryClick,
                modifier = Modifier.size(40.dp)
            ) {
                Text(
                    text = currentBrushType.icon,
                    fontSize = 18.sp
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

            // 导出按钮
            IconButton(
                onClick = onExportClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(R.string.export),
                    tint = Color.White
                )
            }
        }
    }
}
