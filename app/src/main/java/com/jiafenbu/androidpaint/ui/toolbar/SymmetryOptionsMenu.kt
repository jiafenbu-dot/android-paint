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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.jiafenbu.androidpaint.model.SymmetryAxis

/**
 * 对称选项菜单
 * 显示对称轴选项
 * 
 * @param currentAxis 当前选中的对称轴
 * @param onAxisSelected 选择对称轴回调
 * @param modifier 修饰符
 */
@Composable
fun SymmetryOptionsMenu(
    currentAxis: SymmetryAxis,
    onAxisSelected: (SymmetryAxis) -> Unit,
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
                text = stringResource(R.string.symmetry),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )
            
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                SymmetryAxisItem(
                    icon = Icons.Default.Clear,
                    label = stringResource(R.string.symmetry_none),
                    isSelected = currentAxis == SymmetryAxis.NONE,
                    onClick = { onAxisSelected(SymmetryAxis.NONE) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                SymmetryAxisItem(
                    icon = Icons.Default.SwapHoriz,
                    label = stringResource(R.string.symmetry_vertical),
                    isSelected = currentAxis == SymmetryAxis.VERTICAL,
                    onClick = { onAxisSelected(SymmetryAxis.VERTICAL) }
                )
            }
            
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                SymmetryAxisItem(
                    icon = Icons.Default.FlipToBack,
                    label = stringResource(R.string.symmetry_horizontal),
                    isSelected = currentAxis == SymmetryAxis.HORIZONTAL,
                    onClick = { onAxisSelected(SymmetryAxis.HORIZONTAL) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                SymmetryAxisItem(
                    icon = Icons.Default.SwapHoriz,
                    label = stringResource(R.string.symmetry_radial),
                    isSelected = currentAxis == SymmetryAxis.RADIAL,
                    onClick = { onAxisSelected(SymmetryAxis.RADIAL) }
                )
            }
        }
    }
}

/**
 * 对称轴项
 */
@Composable
private fun SymmetryAxisItem(
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
