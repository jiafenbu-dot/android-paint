package com.jiafenbu.androidpaint.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.R
import com.jiafenbu.androidpaint.model.Palette
import com.jiafenbu.androidpaint.model.PaletteColor
import com.jiafenbu.androidpaint.palette.PaletteManager

/**
 * 浮动色板面板
 * 显示色板颜色选择器
 * 
 * @param palettes 色板列表
 * @param selectedPaletteId 选中的色板 ID
 * @param recentColors 最近使用颜色
 * @param currentColor 当前颜色
 * @param onColorSelected 颜色选择回调
 * @param onPaletteSelected 色板选择回调
 * @param onAddColor 添加颜色到当前色板
 * @param onCreatePalette 创建新色板
 * @param onDeletePalette 删除色板
 * @param onDismiss 关闭面板
 */
@Composable
fun PalettePanel(
    palettes: List<Palette>,
    selectedPaletteId: Long?,
    recentColors: List<Int>,
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onPaletteSelected: (Long) -> Unit,
    onAddColor: (Int) -> Unit,
    onCreatePalette: (String) -> Unit,
    onDeletePalette: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPaletteName by remember { mutableStateOf("") }
    
    val selectedPalette = palettes.find { it.id == selectedPaletteId }
    
    Surface(
        modifier = modifier.width(320.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2D2D2D).copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.palette),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新建色板",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tab 切换
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("预设", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("自定义", fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("最近", fontSize = 12.sp) }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 内容区域
            when (selectedTabIndex) {
                0 -> {
                    // 预设色板
                    PaletteGrid(
                        palettes = palettes.filter { it.isDefault },
                        selectedPaletteId = selectedPaletteId,
                        currentColor = currentColor,
                        onPaletteSelected = onPaletteSelected,
                        onColorSelected = onColorSelected,
                        showMenu = false,
                        onDelete = {}
                    )
                }
                1 -> {
                    // 自定义色板
                    if (palettes.filter { !it.isDefault }.isEmpty()) {
                        Text(
                            text = "暂无自定义色板",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        PaletteGrid(
                            palettes = palettes.filter { !it.isDefault },
                            selectedPaletteId = selectedPaletteId,
                            currentColor = currentColor,
                            onPaletteSelected = onPaletteSelected,
                            onColorSelected = onColorSelected,
                            showMenu = true,
                            onDelete = onDeletePalette
                        )
                    }
                }
                2 -> {
                    // 最近使用
                    RecentColorsGrid(
                        colors = recentColors,
                        currentColor = currentColor,
                        onColorSelected = onColorSelected
                    )
                }
            }
            
            // 当前选中色板的颜色
            if (selectedPalette != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = selectedPalette.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 颜色网格
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    items(selectedPalette.colors) { paletteColor ->
                        ColorSwatch(
                            color = paletteColor.color,
                            isSelected = paletteColor.color == currentColor,
                            onClick = { onColorSelected(paletteColor.color) }
                        )
                    }
                    
                    // 添加按钮
                    item {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onAddColor(currentColor) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加颜色",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 创建色板对话框
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建色板") },
            text = {
                OutlinedTextField(
                    value = newPaletteName,
                    onValueChange = { newPaletteName = it },
                    label = { Text("色板名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPaletteName.isNotBlank()) {
                            onCreatePalette(newPaletteName)
                            newPaletteName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("创建")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 色板网格
 */
@Composable
private fun PaletteGrid(
    palettes: List<Palette>,
    selectedPaletteId: Long?,
    currentColor: Int,
    onPaletteSelected: (Long) -> Unit,
    onColorSelected: (Int) -> Unit,
    showMenu: Boolean,
    onDelete: (Long) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        palettes.forEach { palette ->
            PaletteItem(
                palette = palette,
                isSelected = palette.id == selectedPaletteId,
                currentColor = currentColor,
                onSelect = { onPaletteSelected(palette.id) },
                onColorSelected = onColorSelected,
                showMenu = showMenu,
                onDelete = { onDelete(palette.id) }
            )
        }
    }
}

/**
 * 色板条目
 */
@Composable
private fun PaletteItem(
    palette: Palette,
    isSelected: Boolean,
    currentColor: Int,
    onSelect: () -> Unit,
    onColorSelected: (Int) -> Unit,
    showMenu: Boolean,
    onDelete: () -> Unit
) {
    var showPopupMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFF2196F3).copy(alpha = 0.3f) 
            else 
                Color(0xFF3D3D3D)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = palette.name,
                    color = Color.White,
                    fontSize = 12.sp
                )
                
                Row {
                    if (showMenu) {
                        Box {
                            IconButton(
                                onClick = { showPopupMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showPopupMenu,
                                onDismissRequest = { showPopupMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("删除") },
                                    onClick = {
                                        onDelete()
                                        showPopupMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 颜色预览条
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                palette.colors.take(8).forEach { paletteColor ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(paletteColor.color))
                            .clickable { onColorSelected(paletteColor.color) }
                    )
                }
            }
        }
    }
}

/**
 * 最近使用颜色网格
 */
@Composable
private fun RecentColorsGrid(
    colors: List<Int>,
    currentColor: Int,
    onColorSelected: (Int) -> Unit
) {
    if (colors.isEmpty()) {
        Text(
            text = "暂无最近使用颜色",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(colors) { color ->
                ColorSwatch(
                    color = color,
                    isSelected = color == currentColor,
                    onClick = { onColorSelected(color) }
                )
            }
        }
    }
}

/**
 * 颜色色块
 */
@Composable
private fun ColorSwatch(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(color))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Color.White, CircleShape)
                } else {
                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                }
            )
            .clickable { onClick() }
    )
}

/**
 * 小型色板面板（画布上浮动）
 */
@Composable
fun FloatingColorPalette(
    colors: List<Int>,
    currentColor: Int,
    onColorSelected: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2D2D2D).copy(alpha = 0.9f),
        shadowElevation = 4.dp,
        onClick = onClick
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(colors.take(10)) { color ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (color == currentColor) {
                                Modifier.border(1.5.dp, Color.White, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}
