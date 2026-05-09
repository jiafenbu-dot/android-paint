package com.jiafenbu.androidpaint.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiafenbu.androidpaint.brush.BrushType
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
import com.jiafenbu.androidpaint.ui.toolbar.SideToolBar
import com.jiafenbu.androidpaint.ui.toolbar.TopActionBar
import com.jiafenbu.androidpaint.ui.toolbar.GridOptionsMenu
import com.jiafenbu.androidpaint.ui.toolbar.SelectionToolMenu
import com.jiafenbu.androidpaint.ui.toolbar.SymmetryOptionsMenu

/**
 * 主界面
 * 整合所有组件，包括画布、工具栏、面板等
 * 布局：顶部TopActionBar(全宽) + 下方Row(SideToolBar + DrawingCanvas + 叠加层)
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
        viewModel.initFontManager(fontManager)
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
    var showSettingsMenu by remember { mutableStateOf(false) }

    // 显示加载状态
    if (viewModel.isSaving) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "正在保存...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 主布局：Column = 顶部TopActionBar(全宽) + 下方Row(SideToolBar + 画布区域)
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().background(Color.Black)) {

        // 顶部操作栏
        TopActionBar(
            canUndo = viewModel.canUndo,
            canRedo = viewModel.canRedo,
            onCloseClick = {
                if (projectId != null) {
                    showBackConfirmDialog = true
                }
            },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onLayersClick = {
                if (viewModel.toolMode == ToolMode.SELECTION) {
                    viewModel.clearSelection()
                }
                viewModel.toggleLayerPanel()
            },
            onSettingsClick = {
                showSettingsMenu = !showSettingsMenu
                if (showSettingsMenu) {
                    showSelectionMenu = false
                    showGridMenu = false
                    showSymmetryMenu = false
                }
            }
        )

        // ========== 下方区域：左侧工具栏 + 画布 ==========
        Row(modifier = Modifier.weight(1f).fillMaxSize()) {

            // ========== 左侧竖排工具栏（精简版） ==========
            SideToolBar(
                currentBrushType = viewModel.currentBrush.type,
                currentColor = viewModel.currentColor,
                currentBrushSize = viewModel.currentBrush.size,
                currentBrushOpacity = viewModel.currentBrush.opacity,
                toolMode = viewModel.toolMode,
                hasSelection = viewModel.currentSelection != null,
                onSelectionClick = {
                    showSelectionMenu = !showSelectionMenu
                    if (showSelectionMenu) {
                        showGridMenu = false
                        showSymmetryMenu = false
                        showSettingsMenu = false
                    }
                },
                onPanClick = {
                    if (viewModel.toolMode == ToolMode.PAN) {
                        viewModel.changeToolMode(ToolMode.DRAW)
                    } else {
                        if (viewModel.toolMode == ToolMode.SELECTION) {
                            viewModel.clearSelection()
                        }
                        viewModel.changeToolMode(ToolMode.PAN)
                    }
                },
                onEraserClick = {
                    if (viewModel.toolMode == ToolMode.SELECTION) {
                        viewModel.clearSelection()
                    }
                    viewModel.setBrushType(BrushType.ERASER)
                    viewModel.changeToolMode(ToolMode.DRAW)
                },
                onColorClick = {
                    if (viewModel.toolMode == ToolMode.SELECTION) {
                        viewModel.clearSelection()
                    }
                    viewModel.toggleColorPicker()
                },
                onBrushClick = {
                    // 退出选区模式
                    if (viewModel.toolMode == ToolMode.SELECTION) {
                        viewModel.clearSelection()
                    }
                    // 切换到绘画模式，设为当前非橡皮擦/非填充的笔刷
                    if (viewModel.currentBrush.type == BrushType.ERASER || viewModel.currentBrush.type == BrushType.FILL) {
                        viewModel.setBrushType(BrushType.PENCIL)
                    }
                    viewModel.changeToolMode(ToolMode.DRAW)
                    viewModel.toggleBrushSettings()
                },
                onBrushLibraryClick = {
                    if (viewModel.toolMode == ToolMode.SELECTION) {
                        viewModel.clearSelection()
                    }
                    viewModel.toggleBrushLibrary()
                },
                onLayersClick = {
                    if (viewModel.toolMode == ToolMode.SELECTION) {
                        viewModel.clearSelection()
                    }
                    viewModel.toggleLayerPanel()
                },
                onBrushSizeClick = {
                    viewModel.toggleBrushSettings()
                },
                onOpacityClick = {
                    viewModel.toggleBrushSettings()
                }
            )

            // 画布区域（使用 Box 叠加覆盖层和面板）
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {

                // 画布
                DrawingCanvas(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
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

                // ===== 左侧工具栏右侧弹出的面板 =====

                // 笔刷设置面板
                if (viewModel.isBrushSettingsVisible) {
                    BrushSettingsPanel(
                        brushDescriptor = viewModel.currentBrush,
                        onSizeChange = { viewModel.setBrushSize(it) },
                        onOpacityChange = { viewModel.setBrushOpacity(it) },
                        onSpacingChange = { viewModel.setBrushSpacing(it) },
                        onJitterChange = { viewModel.setBrushJitter(it) },
                        onDismiss = { viewModel.hideBrushSettings() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
                    )
                }

                // 颜色选择器
                if (viewModel.isColorPickerVisible) {
                    ColorPicker(
                        currentColor = viewModel.currentColor,
                        colorHistory = viewModel.colorHistory,
                        onColorSelected = { color ->
                            viewModel.setColor(color)
                        },
                        onDismiss = { viewModel.hideColorPicker() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
                    )
                }

                // 选区工具菜单
                if (showSelectionMenu) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
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
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
                    ) {
                        GridOptionsMenu(
                            currentGridType = viewModel.gridType,
                            onGridTypeSelected = { type ->
                                viewModel.changeGridType(type)
                                showGridMenu = false
                            }
                        )
                    }
                }

                // 对称选项菜单
                if (showSymmetryMenu) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
                    ) {
                        SymmetryOptionsMenu(
                            currentAxis = viewModel.symmetryAxis,
                            onAxisSelected = { axis ->
                                viewModel.changeSymmetryAxis(axis)
                                showSymmetryMenu = false
                            }
                        )
                    }
                }

                // ===== 设置菜单弹出面板 =====
                if (showSettingsMenu) {
                    SettingsMenuPanel(
                        onBrushLibrary = {
                            viewModel.toggleBrushLibrary()
                            showSettingsMenu = false
                        },
                        onPalette = {
                            viewModel.togglePalettePanel()
                            showSettingsMenu = false
                        },
                        onReference = {
                            viewModel.toggleReferencePanel()
                            showSettingsMenu = false
                        },
                        onGrid = {
                            showGridMenu = !showGridMenu
                            showSettingsMenu = false
                        },
                        onSymmetry = {
                            showSymmetryMenu = !showSymmetryMenu
                            showSettingsMenu = false
                        },
                        onText = {
                            viewModel.enterTextMode()
                            showSettingsMenu = false
                        },
                        onWatermark = {
                            viewModel.toggleWatermarkPanel()
                            showSettingsMenu = false
                        },
                        onEyedropper = {
                            if (viewModel.toolMode == ToolMode.EYEDROPPER) {
                                viewModel.exitEyedropperMode()
                            } else {
                                viewModel.enterEyedropperMode()
                            }
                            showSettingsMenu = false
                        },
                        onFill = {
                            viewModel.setBrushType(BrushType.FILL)
                            viewModel.changeToolMode(ToolMode.DRAW)
                            showSettingsMenu = false
                        },
                        onExport = {
                            showExportDialog = true
                            showSettingsMenu = false
                        },
                        onDismiss = {
                            showSettingsMenu = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 8.dp)
                    )
                }

                // ===== 右侧面板 =====

                // 图层面板
                if (viewModel.isLayerPanelVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    ) {
                        LayerPanel(
                            viewModel = viewModel,
                            onDismiss = { viewModel.hideLayerPanel() },
                            onEditTextLayer = { index ->
                                viewModel.editTextLayer(index)
                            },
                            onRasterizeTextLayer = { index ->
                                viewModel.rasterizeTextLayer(index)
                            }
                        )
                    }
                }

                // ===== 中央/其他面板 =====

                // 色板面板
                if (viewModel.isPalettePanelVisible) {
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
                        onDismiss = { viewModel.hidePalettePanel() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
                    )
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

                // 文字工具面板
                if (viewModel.isTextToolPanelVisible && viewModel.currentTextLayer != null) {
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
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
                    )
                }

                // 水印面板
                if (viewModel.isWatermarkPanelVisible) {
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
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 4.dp)
                    )
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
                            .padding(bottom = 16.dp)
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

                // 缩放比例指示器
                ZoomIndicator(
                    scale = viewModel.canvasState.scale,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
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

/**
 * 设置菜单弹出面板
 * 包含从侧边栏精简掉的功能入口
 */
@Composable
private fun SettingsMenuPanel(
    onBrushLibrary: () -> Unit,
    onPalette: () -> Unit,
    onReference: () -> Unit,
    onGrid: () -> Unit,
    onSymmetry: () -> Unit,
    onText: () -> Unit,
    onWatermark: () -> Unit,
    onEyedropper: () -> Unit,
    onFill: () -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF333333),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp)
        ) {
            SettingsMenuItem(icon = Icons.Default.Brush, label = "笔刷库", onClick = onBrushLibrary)
            SettingsMenuItem(icon = Icons.Default.Palette, label = "色板", onClick = onPalette)
            SettingsMenuItem(icon = Icons.Default.Image, label = "参考图", onClick = onReference)
            SettingsMenuItem(icon = Icons.Default.GridOn, label = "网格", onClick = onGrid)
            SettingsMenuItem(icon = Icons.Default.TextFields, label = "文字", onClick = onText)
            SettingsMenuItem(icon = Icons.Outlined.Colorize, label = "吸管", onClick = onEyedropper)
            SettingsMenuItem(icon = Icons.Default.FormatColorFill, label = "填充", onClick = onFill)
            SettingsMenuItem(icon = Icons.Default.Download, label = "导出", onClick = onExport)
            SettingsMenuItem(icon = Icons.Default.Layers, label = "对称", onClick = onSymmetry)
            SettingsMenuItem(icon = null, emojiLabel = "💧", label = "水印", onClick = onWatermark)
        }
    }
}

/**
 * 设置菜单项
 */
@Composable
private fun SettingsMenuItem(
    icon: ImageVector?,
    emojiLabel: String? = null,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else if (emojiLabel != null) {
            Text(
                text = emojiLabel,
                fontSize = 16.sp
            )
        }
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp
        )
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xCC000000),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onFill) {
                Text("🪣", fontSize = 20.sp)
            }
            IconButton(onClick = onClear) {
                Text("🗑️", fontSize = 20.sp)
            }
            IconButton(onClick = onStroke) {
                Text("✏️", fontSize = 20.sp)
            }
            IconButton(onClick = onCopy) {
                Text("📋", fontSize = 20.sp)
            }
            IconButton(onClick = onInvert) {
                Text("🔄", fontSize = 20.sp)
            }
            IconButton(onClick = onDeselect) {
                Text("❌", fontSize = 20.sp)
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
            .padding(4.dp)
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
