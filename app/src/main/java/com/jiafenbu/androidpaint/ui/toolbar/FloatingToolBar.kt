package com.jiafenbu.androidpaint.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Waterdrop
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.R
import com.jiafenbu.androidpaint.brush.BrushType
import com.jiafenbu.androidpaint.model.GridType
import com.jiafenbu.androidpaint.model.SelectionType
import com.jiafenbu.androidpaint.model.SymmetryAxis
import com.jiafenbu.androidpaint.model.ToolMode

/**
 * 浮动工具栏
 * Procreate 风格，位于屏幕顶部
 * 包含：颜色、笔刷库、图层、撤销重做、导出、选区工具、吸管、变形、色板、参考图、网格、对称
 */
@Composable
fun FloatingToolBar(
    currentBrushType: BrushType,
    currentColor: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    toolMode: ToolMode = ToolMode.DRAW,
    symmetryAxis: SymmetryAxis = SymmetryAxis.NONE,
    gridType: GridType = GridType.NONE,
    hasSelection: Boolean = false,
    onBrushLibraryClick: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onColorClick: () -> Unit,
    onExportClick: () -> Unit,
    onLayersClick: () -> Unit,
    // 阶段6新增
    onSelectionClick: () -> Unit,
    onEyedropperClick: () -> Unit,
    onTransformClick: () -> Unit,
    onPaletteClick: () -> Unit,
    onReferenceClick: () -> Unit,
    onGridClick: () -> Unit,
    onSymmetryClick: () -> Unit,
    // 阶段7新增
    onTextClick: () -> Unit,
    onWatermarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC000000),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 颜色选择按钮
            ColorButton(
                color = Color(currentColor),
                onClick = onColorClick
            )

            VerticalDivider()

            // 笔刷库按钮
            BrushLibraryButton(
                brushType = currentBrushType,
                onClick = onBrushLibraryClick
            )

            VerticalDivider()

            // 选区工具按钮
            ToolButton(
                icon = Icons.Outlined.SelectAll,
                isActive = toolMode == ToolMode.SELECTION,
                onClick = onSelectionClick,
                contentDescription = stringResource(R.string.selection)
            )

            // 吸管工具按钮
            ToolButton(
                icon = Icons.Outlined.Colorize,
                isActive = toolMode == ToolMode.EYEDROPPER,
                onClick = onEyedropperClick,
                contentDescription = stringResource(R.string.eyedropper)
            )

            // 变形按钮
            ToolButton(
                icon = Icons.Outlined.Transform,
                isActive = toolMode == ToolMode.TRANSFORM || hasSelection,
                onClick = onTransformClick,
                contentDescription = stringResource(R.string.transform),
                enabled = hasSelection
            )

            VerticalDivider()

            // 色板按钮
            ToolButton(
                icon = Icons.Default.Palette,
                isActive = false,
                onClick = onPaletteClick,
                contentDescription = stringResource(R.string.palette)
            )

            // 参考图按钮
            ToolButton(
                icon = Icons.Default.Image,
                isActive = false,
                onClick = onReferenceClick,
                contentDescription = stringResource(R.string.reference)
            )

            VerticalDivider()

            // 网格按钮
            ToolButton(
                icon = Icons.Default.GridOn,
                isActive = gridType != GridType.NONE,
                onClick = onGridClick,
                contentDescription = stringResource(R.string.grid)
            )

            // 对称按钮
            ToolButton(
                icon = Icons.Default.SwapHoriz,
                isActive = symmetryAxis != SymmetryAxis.NONE,
                onClick = onSymmetryClick,
                contentDescription = stringResource(R.string.symmetry)
            )

            VerticalDivider()

            // 文字工具按钮
            ToolButton(
                icon = Icons.Default.TextFields,
                isActive = toolMode == ToolMode.TEXT,
                onClick = onTextClick,
                contentDescription = stringResource(R.string.text_tool)
            )

            // 水印按钮
            ToolButton(
                icon = Icons.Default.Waterdrop,
                isActive = toolMode == ToolMode.WATERMARK,
                onClick = onWatermarkClick,
                contentDescription = stringResource(R.string.watermark)
            )

            VerticalDivider()

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

            VerticalDivider()

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

            VerticalDivider()

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

/**
 * 颜色选择按钮
 */
@Composable
private fun ColorButton(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
            .clickable(onClick = onClick)
    )
}

/**
 * 笔刷库按钮
 */
@Composable
private fun BrushLibraryButton(
    brushType: BrushType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = brushType.icon,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 工具按钮
 */
@Composable
private fun ToolButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> Color.Gray
                isActive -> Color(0xFF2196F3)
                else -> Color.White
            }
        )
    }
}

/**
 * 垂直分隔线
 */
@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .size(1.dp, 24.dp)
            .background(Color.White.copy(alpha = 0.2f))
    )
}


