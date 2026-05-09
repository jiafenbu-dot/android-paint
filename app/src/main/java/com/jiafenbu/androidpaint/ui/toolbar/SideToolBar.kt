package com.jiafenbu.androidpaint.ui.toolbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.R
import com.jiafenbu.androidpaint.brush.BrushType
import com.jiafenbu.androidpaint.model.ToolMode

/**
 * 左侧竖排工具栏（精简版）
 * 深灰色背景，竖排排列核心工具图标按钮
 * 从上到下：选区、平移、橡皮擦、颜色、笔刷、图层
 * 底部：笔刷大小(S)和透明度圆形指示器
 */
@Composable
fun SideToolBar(
    currentBrushType: BrushType,
    currentColor: Int,
    currentBrushSize: Float,
    currentBrushOpacity: Float,
    toolMode: ToolMode = ToolMode.DRAW,
    hasSelection: Boolean = false,
    onSelectionClick: () -> Unit,
    onPanClick: () -> Unit,
    onEraserClick: () -> Unit,
    onColorClick: () -> Unit,
    onBrushClick: () -> Unit,
    onBrushLibraryClick: () -> Unit,
    onLayersClick: () -> Unit,
    onBrushSizeClick: () -> Unit = onBrushClick,
    onOpacityClick: () -> Unit = onBrushClick,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(56.dp)
            .background(Color(0xFF2A2A2A)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. 选区/移动工具
        SideToolButton(
            icon = Icons.Outlined.SelectAll,
            isActive = toolMode == ToolMode.SELECTION,
            onClick = onSelectionClick,
            contentDescription = stringResource(R.string.selection)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 2. 手形平移工具
        SideToolButton(
            icon = null,
            emojiLabel = "✋",
            isActive = toolMode == ToolMode.PAN,
            onClick = onPanClick,
            contentDescription = "平移"
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 3. 橡皮擦
        SideToolButton(
            icon = null,
            emojiLabel = "🧹",
            isActive = toolMode == ToolMode.DRAW && currentBrushType == BrushType.ERASER,
            onClick = onEraserClick,
            contentDescription = stringResource(R.string.eraser)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 4. 颜色选择器
        ColorIndicatorButton(
            color = Color(currentColor),
            onClick = onColorClick
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 5. 笔刷工具（长按打开笔刷库）
        SideToolButtonWithLabel(
            label = currentBrushType.icon,
            isActive = toolMode == ToolMode.DRAW && currentBrushType != BrushType.ERASER && currentBrushType != BrushType.FILL,
            onClick = onBrushClick,
            onLongClick = onBrushLibraryClick,
            contentDescription = stringResource(R.string.pencil)
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 6. 图层面板
        SideToolButton(
            icon = Icons.Default.Layers,
            isActive = false,
            onClick = onLayersClick,
            contentDescription = stringResource(R.string.layers)
        )

        // 弹性空间，把底部指示器推到最下方
        Spacer(modifier = Modifier.weight(1f))

        // 底部：笔刷大小圆形指示器
        CircleIndicator(
            label = "S",
            value = currentBrushSize.toInt().toString(),
            onClick = onBrushSizeClick
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 底部：透明度圆形指示器
        CircleIndicator(
            label = "",
            value = (currentBrushOpacity * 100).toInt().toString(),
            onClick = onOpacityClick
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 侧边栏工具按钮（图标）
 */
@Composable
private fun SideToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isActive: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    enabled: Boolean = true,
    emojiLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) Color(0xFF4A90D9) else Color.Transparent
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    !enabled -> Color.Gray
                    isActive -> Color.White
                    else -> Color.White.copy(alpha = 0.85f)
                },
                modifier = Modifier.size(22.dp)
            )
        } else if (emojiLabel != null) {
            Text(
                text = emojiLabel,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 侧边栏工具按钮（emoji标签，用于画笔图标）
 * 支持长按打开笔刷库
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SideToolButtonWithLabel(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) Color(0xFF4A90D9) else Color.Transparent
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 颜色指示器按钮
 */
@Composable
private fun ColorIndicatorButton(
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 半透明内圈表示可点击
    }
}

/**
 * 底部圆形指示器（笔刷大小/透明度）
 */
@Composable
private fun CircleIndicator(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFF3A3A3A))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = value,
                color = Color.White,
                fontSize = if (label.isNotEmpty()) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
