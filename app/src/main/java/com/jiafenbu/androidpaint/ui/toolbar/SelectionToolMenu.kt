package com.jiafenbu.androidpaint.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.R
import com.jiafenbu.androidpaint.model.SelectionType

/**
 * 选区工具菜单
 * 显示选区工具选项
 * 
 * @param onRectangleSelect 点击矩形选区回调
 * @param onEllipseSelect 点击椭圆选区回调
 * @param onLassoSelect 点击自由选区回调
 * @param onMagicWandSelect 点击魔棒选区回调
 */
@Composable
fun SelectionToolMenu(
    onRectangleSelect: () -> Unit,
    onEllipseSelect: () -> Unit,
    onLassoSelect: () -> Unit,
    onMagicWandSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC000000),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = stringResource(R.string.selection),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            // 第一行：矩形和椭圆
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                // 矩形选区 - 最常用，保持启用
                SelectionToolItem(
                    icon = Icons.Default.CropSquare,
                    label = stringResource(R.string.selection_rectangle),
                    onClick = onRectangleSelect,
                    enabled = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // 椭圆选区 - 暂时禁用（功能待完善）
                SelectionToolItem(
                    icon = Icons.Default.RadioButtonUnchecked,
                    label = stringResource(R.string.selection_ellipse),
                    onClick = onEllipseSelect,
                    enabled = false
                )
            }
            
            // 第二行：套索和魔棒
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                // 套索选区 - 暂时禁用（功能待完善）
                SelectionToolItem(
                    icon = Icons.Default.Draw,
                    label = stringResource(R.string.selection_lasso),
                    onClick = onLassoSelect,
                    enabled = false
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // 魔棒选区 - 暂时禁用（功能待完善）
                SelectionToolItem(
                    icon = Icons.Default.AutoAwesome,
                    label = stringResource(R.string.selection_magic_wand),
                    onClick = onMagicWandSelect,
                    enabled = false
                )
            }
        }
    }
}

/**
 * 选区工具项
 */
@Composable
private fun SelectionToolItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .background(
                color = if (enabled) Color(0x33FFFFFF) else Color(0x11FFFFFF),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) Color.White else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 12.sp
        )
    }
}
