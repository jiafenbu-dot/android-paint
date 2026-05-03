package com.jiafenbu.androidpaint.ui.layers

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Merge
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.model.BlendMode
import com.jiafenbu.androidpaint.model.LayerModel
import com.jiafenbu.androidpaint.model.LayerType
import com.jiafenbu.androidpaint.ui.canvas.CanvasViewModel
import kotlin.math.roundToInt
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 图层面板
 * 右侧浮动面板，用于图层管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerPanel(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    // 阶段7新增：文字图层编辑回调
    onEditTextLayer: (Int) -> Unit = {},
    onRasterizeTextLayer: (Int) -> Unit = {}
) {
    val layers = viewModel.layers
    val activeIndex = viewModel.activeLayerIndex
    val listState = rememberLazyListState()
    
    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf(false) }
    var layerToDelete by remember { mutableIntStateOf(-1) }
    
    // 重命名对话框
    var showRenameDialog by remember { mutableStateOf(false) }
    var layerToRename by remember { mutableIntStateOf(-1) }
    var newLayerName by remember { mutableStateOf("") }
    
    // 混合模式选择底部弹窗
    var showBlendModeSheet by remember { mutableStateOf(false) }
    var selectedLayerForBlendMode by remember { mutableIntStateOf(-1) }
    val blendModeSheetState = rememberModalBottomSheetState()
    
    // 拖拽状态
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(0.8f),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF2D2D2D).copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "图层",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                
                // 新建图层按钮
                IconButton(
                    onClick = { viewModel.addLayer() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建图层",
                        tint = Color.White
                    )
                }
            }
            
            // 图层列表
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = layers.reversed(), // 反转显示，最上层在顶部
                    key = { _, layer -> layer.id }
                ) { reversedIndex, layer ->
                    val actualIndex = layers.size - 1 - reversedIndex
                    val isActive = actualIndex == activeIndex
                    
                    LayerItem(
                        layer = layer,
                        isActive = isActive,
                        isDragging = actualIndex == draggedItemIndex,
                        dragOffset = if (actualIndex == draggedItemIndex) dragOffsetY else 0f,
                        onSelect = { viewModel.selectLayer(actualIndex) },
                        onToggleVisibility = { viewModel.setLayerVisibility(actualIndex, !layer.isVisible) },
                        onToggleLock = { viewModel.setLayerLocked(actualIndex, !layer.isLocked) },
                        onDelete = {
                            layerToDelete = actualIndex
                            showDeleteDialog = true
                        },
                        onRename = {
                            layerToRename = actualIndex
                            newLayerName = layer.name
                            showRenameDialog = true
                        },
                        onDuplicate = { viewModel.duplicateLayer(actualIndex) },
                        onMergeDown = { viewModel.mergeDown(actualIndex) },
                        onBlendModeClick = {
                            selectedLayerForBlendMode = actualIndex
                            showBlendModeSheet = true
                        },
                        onOpacityChange = { viewModel.setLayerOpacity(actualIndex, it) },
                        onDragStart = { draggedItemIndex = actualIndex },
                        onDrag = { offset ->
                            dragOffsetY = offset
                        },
                        onDragEnd = {
                            // 计算目标位置
                            val itemHeight = 90.dp.value
                            val targetIndex = (actualIndex + (dragOffsetY / itemHeight).roundToInt())
                                .coerceIn(0, layers.size - 1)
                            
                            if (draggedItemIndex != -1 && draggedItemIndex != targetIndex) {
                                viewModel.moveLayer(draggedItemIndex, targetIndex)
                            }
                            
                            draggedItemIndex = -1
                            dragOffsetY = 0f
                        },
                        // 阶段7新增：文字图层双击编辑
                        onDoubleClick = {
                            if (layer.layerType == LayerType.TEXT) {
                                onEditTextLayer(actualIndex)
                            }
                        }
                    )
                }
            }
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog && layerToDelete >= 0) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                layerToDelete = -1
            },
            title = { Text("删除图层") },
            text = { Text("确定要删除图层 \"${layers.getOrNull(layerToDelete)?.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLayer(layerToDelete)
                        showDeleteDialog = false
                        layerToDelete = -1
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        layerToDelete = -1
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 重命名对话框
    if (showRenameDialog && layerToRename >= 0) {
        AlertDialog(
            onDismissRequest = { 
                showRenameDialog = false
                layerToRename = -1
            },
            title = { Text("重命名图层") },
            text = {
                OutlinedTextField(
                    value = newLayerName,
                    onValueChange = { newLayerName = it },
                    label = { Text("图层名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newLayerName.isNotBlank()) {
                            viewModel.renameLayer(layerToRename, newLayerName)
                        }
                        showRenameDialog = false
                        layerToRename = -1
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showRenameDialog = false
                        layerToRename = -1
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 混合模式选择底部弹窗
    if (showBlendModeSheet && selectedLayerForBlendMode >= 0) {
        BlendModeBottomSheet(
            currentBlendMode = layers.getOrNull(selectedLayerForBlendMode)?.blendMode ?: BlendMode.NORMAL,
            onBlendModeSelected = { blendMode ->
                viewModel.setLayerBlendMode(selectedLayerForBlendMode, blendMode)
                showBlendModeSheet = false
                selectedLayerForBlendMode = -1
            },
            onDismiss = {
                showBlendModeSheet = false
                selectedLayerForBlendMode = -1
            },
            sheetState = blendModeSheetState
        )
    }
}

/**
 * 单个图层项
 */
@Composable
private fun LayerItem(
    layer: LayerModel,
    isActive: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMergeDown: () -> Unit,
    onBlendModeClick: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    // 阶段7新增
    onDoubleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showOpacitySlider by remember { mutableStateOf(false) }
    var localOpacity by remember(layer.opacity) { mutableFloatStateOf(layer.opacity) }
    
    // 双击计时器
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    // 动画
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF3D3D3D) else Color(0xFF2D2D2D),
        label = "bgColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF6200EE) else Color.Transparent,
        label = "borderColor"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "elevation"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .offset { IntOffset(0, dragOffset.roundToInt()) }
            .border(
                width = if (isActive) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { 
                // 双击检测
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 300) {
                    // 双击
                    onDoubleClick()
                } else {
                    // 单击
                    onSelect()
                }
                lastClickTime = currentTime
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragOffset + dragAmount.y)
                    },
                    onDragEnd = {
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragEnd()
                    }
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧活动指示器
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(60.dp)
                        .background(
                            Color(0xFF6200EE),
                            RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            // 缩略图
            LayerThumbnail(
                layer = layer,
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // 中间信息区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 图层名称
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = layer.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onRename() }
                    )
                    
                    // 可见性图标
                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "切换可见性",
                            tint = if (layer.isVisible) Color.White else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // 锁定图标
                    IconButton(
                        onClick = onToggleLock,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (layer.isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                            contentDescription = "切换锁定",
                            tint = if (layer.isLocked) Color(0xFFFFB74D) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // 混合模式和透明度行
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 混合模式按钮
                    Box {
                        Surface(
                            modifier = Modifier
                                .clickable { showMenu = true },
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF404040)
                        ) {
                            Text(
                                text = layer.blendMode.displayName,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // 点击混合模式按钮时关闭菜单并显示底部弹窗
                            DropdownMenuItem(
                                text = { Text("选择混合模式...") },
                                onClick = {
                                    onBlendModeClick()
                                    showMenu = false
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // 透明度
                    Surface(
                        modifier = Modifier
                            .clickable { showOpacitySlider = !showOpacitySlider },
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF404040)
                    ) {
                        Text(
                            text = "${(layer.opacity * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // 透明度滑块（点击后显示）
                if (showOpacitySlider) {
                    Slider(
                        value = localOpacity,
                        onValueChange = { 
                            localOpacity = it
                            onOpacityChange(it)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }
            
            // 右下角菜单按钮
            Box {
                Surface(
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { showMenu = true },
                    shape = CircleShape,
                    color = Color(0xFF404040)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⋮",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = {
                            onRename()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("复制图层") },
                        onClick = {
                            onDuplicate()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("向下合并") },
                        onClick = {
                            onMergeDown()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Merge, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除图层") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete, 
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 图层缩略图
 */
@Composable
private fun LayerThumbnail(
    layer: LayerModel,
    modifier: Modifier = Modifier
) {
    val thumbnail = layer.thumbnailBitmap
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF404040), RoundedCornerShape(4.dp)),
        color = Color(0xFF1A1A1A)
    ) {
        if (thumbnail != null && !thumbnail.isRecycled) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = "图层缩略图",
                modifier = Modifier.size(60.dp)
            )
        } else {
            // 占位符
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            ) {
                Text(
                    text = layer.name.take(2),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 混合模式选择底部弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlendModeBottomSheet(
    currentBlendMode: BlendMode,
    onBlendModeSelected: (BlendMode) -> Unit,
    onDismiss: () -> Unit,
    sheetState: androidx.compose.material3.SheetState
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2D2D2D)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择混合模式",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 常用模式（放在前面）
            val commonModes = listOf(
                BlendMode.NORMAL,
                BlendMode.MULTIPLY,
                BlendMode.SCREEN,
                BlendMode.OVERLAY,
                BlendMode.SOFT_LIGHT,
                BlendMode.HARD_LIGHT,
                BlendMode.COLOR_DODGE,
                BlendMode.COLOR_BURN
            )
            
            // 所有模式
            val allModes = BlendMode.entries
            
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 常用模式组
                item {
                    Text(
                        text = "常用模式",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(commonModes.size) { index ->
                    val mode = commonModes[index]
                    BlendModeItem(
                        mode = mode,
                        isSelected = mode == currentBlendMode,
                        onClick = { onBlendModeSelected(mode) }
                    )
                }
                
                // 所有模式组
                item {
                    Text(
                        text = "全部模式",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(allModes.size) { index ->
                    val mode = allModes[index]
                    BlendModeItem(
                        mode = mode,
                        isSelected = mode == currentBlendMode,
                        onClick = { onBlendModeSelected(mode) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 混合模式选项
 */
@Composable
private fun BlendModeItem(
    mode: BlendMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFF6200EE) else Color(0xFF3D3D3D)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.displayName,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = mode.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF6200EE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
