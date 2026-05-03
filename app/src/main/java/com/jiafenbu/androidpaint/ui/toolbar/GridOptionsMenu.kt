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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Perspective
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
import com.jiafenbu.androidpaint.model.GridType

/**
 * 网格选项菜单
 * 显示网格类型选项
 * 
 * @param currentGridType 当前选中的网格类型
 * @param onGridTypeSelected 选择网格类型回调
 * @param modifier 修饰符
 */
@Composable
fun GridOptionsMenu(
    currentGridType: GridType,
    onGridTypeSelected: (GridType) -> Unit,
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
                text = stringResource(R.string.grid),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                GridTypeItem(
                    icon = Icons.Default.Apps,
                    label = stringResource(R.string.grid_none),
                    isSelected = currentGridType == GridType.NONE,
                    onClick = { onGridTypeSelected(GridType.NONE) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                GridTypeItem(
                    icon = Icons.Default.GridOn,
                    label = stringResource(R.string.grid_rule_of_thirds),
                    isSelected = currentGridType == GridType.RULE_OF_THIRDS,
                    onClick = { onGridTypeSelected(GridType.RULE_OF_THIRDS) }
                )
            }
            
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                GridTypeItem(
                    icon = Icons.Default.Perspective,
                    label = stringResource(R.string.grid_perspective),
                    isSelected = currentGridType == GridType.PERSPECTIVE,
                    onClick = { onGridTypeSelected(GridType.PERSPECTIVE) }
                )
            }
        }
    }
}

/**
 * 网格类型项
 */
@Composable
private fun GridTypeItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(
                color = if (isSelected) Color(0xFF6200EE).copy(alpha = 0.5f) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF6200EE) else Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF6200EE) else Color.White,
            fontSize = 12.sp
        )
    }
}
