package com.jiafenbu.androidpaint.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiafenbu.androidpaint.export.ExportManager
import com.jiafenbu.androidpaint.model.GridType
import com.jiafenbu.androidpaint.model.SelectionType
import com.jiafenbu.androidpaint.model.SymmetryAxis
import com.jiafenbu.androidpaint.model.ToolMode
import com.jiafenbu.androidpaint.palette.PaletteManager
import com.jiafenbu.androidpaint.ui.brush.BrushLibrary
import com.jiafenbu.androidpaint.ui.canvas.CanvasViewModel
import com.jiafenbu.androidpaint.ui.canvas.DrawingCanvas
import com.jiafenbu.androidpaint.ui.colorpicker.ColorPicker
import com.jiafenbu.androidpaint.ui.layers.LayerPanel
import com.jiafenbu.androidpaint.ui.palette.PalettePanel
import com.jiafenbu.androidpaint.ui.reference.ReferenceImagePanel
import com.jiafenbu.androidpaint.ui.canvas.GridOverlay
import com.jiafenbu.androidpaint.ui.canvas.SymmetryOverlay
import com.jiafenbu.androidpaint.ui.selection.SelectionOverlay
import com.jiafenbu.androidpaint.ui.text.TextToolPanel
import com.jiafenbu.androidpaint.ui.watermark.WatermarkOverlay
import com.jiafenbu.androidpaint.ui.watermark.WatermarkPanel
import com.jiafenbu.androidpaint.ui.toolbar.BrushSettingsPanel
import com.jiafenbu.androidpaint.ui.toolbar.FloatingToolBar
import com.jiafenbu.androidpaint.ui.toolbar.GridOptionsMenu
import com.jiafenbu.androidpaint.ui.toolbar.SelectionToolMenu
import com.jiafenbu.androidpaint.ui.toolbar.SymmetryOptionsMenu

/**
 * 主界面
 * 整合所有组件，包括画布、工具栏、面板等
 * 包含阶段6专业功能：选区、变形、色板、参考图、对称绘制、网格等
 * 
 * @param viewModel 画布 ViewModel
 * @param projectId 当前项目 ID（可选）
 * @param onBackToGallery 返回画廊的回调
 */
@Composable
fun MainScreen(
    viewModel: CanvasViewModel = viewModel(),
    projectId: String? = null,
    onBackToGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    val exportManager = remember { ExportManager(context) }
    val paletteManager = remember { PaletteManager(context) }
    val fontManager = remember { com.jiafenbu.androidpaint.font.FontManager(context) }
    
    // 初始化色板管理器
    LaunchedEffect(Unit) {
        viewModel.setPaletteManager(paletteManager)
        viewModel.setFontManager(fontManager)
    }
    
    // 导出消息提示
    LaunchedEffect(viewModel.exportMessage) {
        viewModel.exportMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessage()
        }
    }
    
    // 保存消息提示
    LaunchedEffect(viewModel.saveMessage) {
        viewModel.saveMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveMessage()
        }
    }
    
    // 加载项目
    LaunchedEffect(projectId) {
        projectId?.let {
            viewModel.loadProject(it)
        }
    }
    
    // 对话框状态
    var showBackConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    // 菜单显示状态
    var showSelectionMenu by remember { mutableStateOf(false) }
    var showGridMenu by remember { mutableStateOf(false) }
    var showSymmetryMenu by remember { mutableStateOf(false) }
    
    // 显示加载状态
    if (viewModel.isSaving) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "正在保存...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    Box(modifier = Modifier.fillMaxWidth()) {
        // 顶部项目名称和返回按钮
        if (projectId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 48.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { showBackConfirmDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = viewModel.projectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
            }
        }
        
        // 画布
        DrawingCanvas(
            viewModel = viewModel,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 网格覆盖层
        if (viewModel.gridType != GridType.NONE) {
            GridOverlay(
                gridType = viewModel.gridType,
                canvasWidth = viewModel.canvasWidth.toFloat(),
                canvasHeight = viewModel.canvasHeight.toFloat(),
                scale = viewModel.canvasState.scale,
                offsetX = viewModel.canvasState.offsetX,
                offsetY = viewModel.canvasState.offsetY
            )
        }
        
        // 对称辅助线覆盖层
        if (viewModel.symmetryAxis != SymmetryAxis.NONE) {
            SymmetryOverlay(
                symmetryAxis = viewModel.symmetryAxis,
                canvasWidth = viewModel.canvasWidth.toFloat(),
                canvasHeight = viewModel.canvasHeight.toFloat(),
                scale = viewModel.canvasState.scale,
                rotation = viewModel.canvasState.rotation,
                offsetX = viewModel.canvasState.offsetX,
                offsetY = viewModel.canvasState.offsetY
            )
        }
        
        // 水印覆盖层
        if (viewModel.watermarkConfig != null && viewModel.watermarkConfig!!.isEnabled) {
            WatermarkOverlay(
                watermarkConfig = viewModel.watermarkConfig,
                canvasWidth = viewModel.canvasWidth,
                canvasHeight = viewModel.canvasHeight
            )
        }
        
        // 选区覆盖层
        if (viewModel.currentSelection != null) {
            SelectionOverlay(
                selection = viewModel.currentSelection,
                canvasWidth = viewModel.canvasWidth.toFloat(),
                canvasHeight = viewModel.canvasHeight.toFloat(),
                scale = viewModel.canvasState.scale,
                rotation = viewModel.canvasState.rotation,
                offsetX = viewModel.canvasState.offsetX,
                offsetY = viewModel.canvasState.offsetY,
                onSelectionChange = { selection ->
                    if (selection == null) {
                        viewModel.clearSelection()
                    }
                },
                onTransformStart = { handle ->
                    viewModel.enterTransformMode()
                },
                onTransformEnd = {
                    viewModel.exitTransformMode()
                }
            )
        }
        
        // 浮动工具栏
        FloatingToolBar(
            currentBrushType = viewModel.currentBrush.type,
            currentColor = viewModel.currentColor,
            canUndo = viewModel.canUndo,
            canRedo = viewModel.canRedo,
            toolMode = viewModel.toolMode,
            symmetryAxis = viewModel.symmetryAxis,
            gridType = viewModel.gridType,
            hasSelection = viewModel.currentSelection != null,
            onBrushLibraryClick = { viewModel.toggleBrushLibrary() },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onColorClick = { viewModel.toggleColorPicker() },
            onExportClick = { showExportDialog = true },
            onLayersClick = { viewModel.toggleLayerPanel() },
            // 阶段6新增
            onSelectionClick = {
                showSelectionMenu = !showSelectionMenu
                if (showSelectionMenu) {
                    showGridMenu = false
                    showSymmetryMenu = false
                }
            },
            onEyedropperClick = { 
                if (viewModel.toolMode == ToolMode.EYEDROPPER) {
                    viewModel.exitEyedropperMode()
                } else {
                    viewModel.enterEyedropperMode()
                }
            },
            onTransformClick = {
                if (viewModel.currentSelection != null) {
                    viewModel.enterTransformMode()
                }
            },
            onPaletteClick = { viewModel.togglePalettePanel() },
            onReferenceClick = { viewModel.toggleReferencePanel() },
            onGridClick = {
                showGridMenu = !showGridMenu
                if (showGridMenu) {
                    showSelectionMenu = false
                    showSymmetryMenu = false
                } else {
                    viewModel.toggleGrid()
                }
            },
            onSymmetryClick = {
                showSymmetryMenu = !showSymmetryMenu
                if (showSymmetryMenu) {
                    showSelectionMenu = false
                    showGridMenu = false
                } else {
                    viewModel.toggleSymmetry()
                }
            },
            // 阶段7新增
            onTextClick = {
                viewModel.enterTextMode()
            },
            onWatermarkClick = {
                viewModel.toggleWatermarkPanel()
            },
            modifier = Modifier.align(if (projectId != null) Alignment.TopCenter else Alignment.TopCenter)
                .padding(top = if (projectId != null) 90.dp else 48.dp)
        )
        
        // 选区工具菜单
        if (showSelectionMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (projectId != null) 150.dp else 110.dp)
            ) {
                SelectionToolMenu(
                    onRectangleSelect = {
                        viewModel.enterSelectionMode(SelectionType.RECTANGLE)
                        showSelectionMenu = false
                    },
                    onEllipseSelect = {
                        viewModel.enterSelectionMode(SelectionType.ELLIPSE)
                        showSelectionMenu = false
                    },
                    onLassoSelect = {
                        viewModel.enterSelectionMode(SelectionType.LASSO)
                        showSelectionMenu = false
                    },
                    onMagicWandSelect = {
                        viewModel.enterSelectionMode(SelectionType.MAGIC_WAND)
                        showSelectionMenu = false
                    }
                )
            }
        }
        
        // 网格选项菜单
        if (showGridMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (projectId != null) 150.dp else 110.dp)
            ) {
                GridOptionsMenu(
                    currentGridType = viewModel.gridType,
                    onGridTypeSelected = { type ->
                        viewModel.setGridType(type)
                        showGridMenu = false
                    }
                )
            }
        }
        
        // 对称选项菜单
        if (showSymmetryMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (projectId != null) 150.dp else 110.dp)
            ) {
                SymmetryOptionsMenu(
                    currentAxis = viewModel.symmetryAxis,
                    onAxisSelected = { axis ->
                        viewModel.setSymmetryAxis(axis)
                        showSymmetryMenu = false
                    }
                )
            }
        }
        
        // 缩放比例指示器
        ZoomIndicator(
            scale = viewModel.canvasState.scale,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
        
        // 笔刷设置面板
        if (viewModel.isBrushSettingsVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 140.dp)
            ) {
                BrushSettingsPanel(
                    brushDescriptor = viewModel.currentBrush,
                    onSizeChange = { viewModel.setBrushSize(it) },
                    onOpacityChange = { viewModel.setBrushOpacity(it) },
                    onSpacingChange = { viewModel.setBrushSpacing(it) },
                    onJitterChange = { viewModel.setBrushJitter(it) },
                    onDismiss = { viewModel.hideBrushSettings() }
                )
            }
        }
        
        // 颜色选择器
        if (viewModel.isColorPickerVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 140.dp)
            ) {
                ColorPicker(
                    currentColor = viewModel.currentColor,
                    colorHistory = viewModel.colorHistory,
                    onColorSelected = { color ->
                        viewModel.setColor(color)
                    },
                    onDismiss = { viewModel.hideColorPicker() }
                )
            }
        }
        
        // 图层面板
        if (viewModel.isLayerPanelVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                LayerPanel(
                    viewModel = viewModel,
                    onDismiss = { viewModel.hideLayerPanel() },
                    // 阶段7新增：文字图层编辑
                    onEditTextLayer = { index ->
                        viewModel.editTextLayer(index)
                    },
                    onRasterizeTextLayer = { index ->
                        viewModel.rasterizeTextLayer(index)
                    }
                )
            }
        }
        
        // 色板面板
        if (viewModel.isPalettePanelVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 200.dp)
            ) {
                PalettePanel(
                    palettes = viewModel.getAllPalettes(),
                    selectedPaletteId = viewModel.selectedPaletteId,
                    recentColors = viewModel.recentColors,
                    currentColor = viewModel.currentColor,
                    onColorSelected = { color ->
                        viewModel.setColor(color)
                    },
                    onPaletteSelected = { paletteId ->
                        viewModel.selectPalette(paletteId)
                    },
                    onAddColor = { color ->
                        viewModel.selectedPaletteId?.let { paletteId ->
                            viewModel.addColorToPalette(paletteId, color)
                        }
                    },
                    onCreatePalette = { name ->
                        viewModel.createPalette(name)
                    },
                    onDeletePalette = { paletteId ->
                        viewModel.deletePalette(paletteId)
                    },
                    onDismiss = { viewModel.hidePalettePanel() }
                )
            }
        }
        
        // 参考图面板
        if (viewModel.isReferencePanelVisible && viewModel.referenceImage != null) {
            ReferenceImagePanel(
                referenceImage = viewModel.referenceImage!!,
                onUpdate = { image ->
                    viewModel.updateReferenceImage(image)
                },
                onRemove = {
                    viewModel.clearReferenceImage()
                },
                onDismiss = {
                    viewModel.toggleReferencePanel()
                }
            )
        }
        
        // 阶段7：文字工具面板
        if (viewModel.isTextToolPanelVisible && viewModel.currentTextLayer != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 200.dp)
            ) {
                TextToolPanel(
                    textModel = viewModel.currentTextLayer!!,
                    onTextChange = { textModel ->
                        viewModel.updateCurrentText(textModel)
                    },
                    onDismiss = {
                        viewModel.cancelTextInput()
                    },
                    onConfirm = {
                        viewModel.confirmAddTextLayer()
                    }
                )
            }
        }
        
        // 阶段7：水印面板
        if (viewModel.isWatermarkPanelVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 200.dp)
            ) {
                WatermarkPanel(
                    watermarkConfig = viewModel.watermarkConfig,
                    onWatermarkChange = { config ->
                        viewModel.updateWatermarkConfig(config)
                    },
                    onDismiss = {
                        viewModel.toggleWatermarkPanel()
                    },
                    onImportImage = {
                        // TODO: 实现从相册导入图片
                    }
                )
            }
        }
        
        // 笔刷库面板
        BrushLibrary(
            isVisible = viewModel.isBrushLibraryVisible,
            selectedBrushType = viewModel.currentBrush.type,
            currentColor = viewModel.currentColor,
            onBrushSelected = { brushType ->
                viewModel.setBrushType(brushType)
                viewModel.hideBrushLibrary()
                viewModel.toggleBrushSettings()
            },
            onDismiss = { viewModel.hideBrushLibrary() }
        )
        
        // 选区操作面板（选区存在时显示）
        if (viewModel.currentSelection != null && viewModel.isSelectionToolbarVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                SelectionActionBar(
                    onFill = {
                        viewModel.fillSelection(viewModel.currentColor)
                    },
                    onClear = {
                        viewModel.clearSelectionContent()
                    },
                    onStroke = {
                        viewModel.strokeSelection(viewModel.currentColor, viewModel.strokeWidth)
                    },
                    onCopy = {
                        viewModel.copySelectionToNewLayer()
                    },
                    onDelete = {
                        viewModel.clearSelectionContent()
                        viewModel.clearSelection()
                    },
                    onInvert = {
                        viewModel.invertSelection()
                    },
                    onDeselect = {
                        viewModel.clearSelection()
                    }
                )
            }
        }
        
        // 导出对话框
        if (showExportDialog) {
            ExportDialog(
                onExportPng = {
                    viewModel.exportPng(exportManager)
                    showExportDialog = false
                },
                onExportJpeg = {
                    viewModel.exportJpeg(exportManager)
                    showExportDialog = false
                },
                onDismiss = { showExportDialog = false }
            )
        }
        
        // 返回确认对话框
        if (showBackConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showBackConfirmDialog = false },
                title = { Text("返回画廊") },
                text = { Text("是否保存当前项目？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBackConfirmDialog = false
                            onBackToGallery()
                        }
                    ) {
                        Text("保存并返回")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showBackConfirmDialog = false
                                viewModel.clearProjectState()
                                onBackToGallery()
                            }
                        ) {
                            Text("不保存")
                        }
                        TextButton(onClick = { showBackConfirmDialog = false }) {
                            Text("取消")
                        }
                    }
                }
            )
        }
    }
}

/**
 * 选区操作工具条
 */
@Composable
private fun SelectionActionBar(
    onFill: () -> Unit,
    onClear: () -> Unit,
    onStroke: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onInvert: () -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = Color(0xCC000000),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.IconButton(onClick = onFill) {
                Text("🪣", fontSize = androidx.compose.ui.unit.sp(20))
            }
            androidx.compose.material3.IconButton(onClick = onClear) {
                Text("🗑️", fontSize = androidx.compose.ui.unit.sp(20))
            }
            androidx.compose.material3.IconButton(onClick = onStroke) {
                Text("✏️", fontSize = androidx.compose.ui.unit.sp(20))
            }
            androidx.compose.material3.IconButton(onClick = onCopy) {
                Text("📋", fontSize = androidx.compose.ui.unit.sp(20))
            }
            androidx.compose.material3.IconButton(onClick = onInvert) {
                Text("🔄", fontSize = androidx.compose.ui.unit.sp(20))
            }
            androidx.compose.material3.IconButton(onClick = onDeselect) {
                Text("❌", fontSize = androidx.compose.ui.unit.sp(20))
            }
        }
    }
}

/**
 * 缩放比例指示器
 */
@Composable
private fun ZoomIndicator(
    scale: Float,
    modifier: Modifier = Modifier
) {
    Text(
        text = String.format("%.1fx", scale),
        color = Color.White.copy(alpha = 0.7f),
        modifier = modifier
            .padding(8.dp)
    )
}

/**
 * 导出对话框
 */
@Composable
private fun ExportDialog(
    onExportPng: () -> Unit,
    onExportJpeg: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "导出图片")
        },
        text = {
            Text(text = "选择导出格式")
        },
        confirmButton = {
            TextButton(onClick = onExportPng) {
                Text("PNG")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onExportJpeg) {
                    Text("JPEG")
                }
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}
