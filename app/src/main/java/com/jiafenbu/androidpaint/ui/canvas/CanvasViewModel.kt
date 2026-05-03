package com.jiafenbu.androidpaint.ui.canvas

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiafenbu.androidpaint.brush.BrushDescriptor
import com.jiafenbu.androidpaint.brush.BrushType
import com.jiafenbu.androidpaint.command.CommandManager
import com.jiafenbu.androidpaint.command.DrawCommand
import com.jiafenbu.androidpaint.engine.Canvas2DEngine
import com.jiafenbu.androidpaint.engine.DrawEngine
import com.jiafenbu.androidpaint.engine.LayerCompositor
import com.jiafenbu.androidpaint.engine.LayerRenderData
import com.jiafenbu.androidpaint.model.BlendMode
import com.jiafenbu.androidpaint.model.CanvasState
import com.jiafenbu.androidpaint.model.GridType
import com.jiafenbu.androidpaint.model.LayerModel
import com.jiafenbu.androidpaint.model.LayerProperty
import com.jiafenbu.androidpaint.model.Palette
import com.jiafenbu.androidpaint.model.ReferenceImage
import com.jiafenbu.androidpaint.model.Selection
import com.jiafenbu.androidpaint.model.StrokeData
import com.jiafenbu.androidpaint.model.StrokePoint
import com.jiafenbu.androidpaint.model.SymmetryAxis
import com.jiafenbu.androidpaint.model.TextAlignment
import com.jiafenbu.androidpaint.model.TextLayerModel
import com.jiafenbu.androidpaint.model.ToolMode
import com.jiafenbu.androidpaint.model.WatermarkConfig
import com.jiafenbu.androidpaint.model.WatermarkTileMode
import com.jiafenbu.androidpaint.model.WatermarkType
import com.jiafenbu.androidpaint.palette.PaletteManager
import com.jiafenbu.androidpaint.font.FontManager
import com.jiafenbu.androidpaint.project.ProjectManager
import com.jiafenbu.androidpaint.selection.SelectionEngine
import com.jiafenbu.androidpaint.selection.TransformEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 画布 ViewModel
 * 管理所有画布相关的状态和操作，包括图层管理、选区、变形、色板等功能
 */
class CanvasViewModel : ViewModel() {

    // ==================== 画布尺寸 ====================
    var canvasWidth by mutableStateOf(1080)
        private set

    var canvasHeight by mutableStateOf(1920)
        private set

    // ==================== 图层管理 ====================
    private val _layers = mutableListOf<LayerModel>()
    val layers: List<LayerModel> get() = _layers

    var activeLayerIndex by mutableIntStateOf(0)
        private set

    /** 获取当前活动图层 */
    val activeLayer: LayerModel?
        get() = _layers.getOrNull(activeLayerIndex)

    /** 图层位图缓存 */
    private val cachedBitmaps = mutableMapOf<Long, Bitmap>()

    // ==================== 画布状态 ====================
    var canvasState by mutableStateOf(CanvasState())
        private set

    // ==================== 笔刷状态 ====================
    var currentBrush by mutableStateOf(BrushDescriptor.getDefault(BrushType.PENCIL))
        private set

    var currentColor by mutableStateOf(0xFF000000.toInt())
        private set

    /** 最近使用的颜色历史 */
    var colorHistory by mutableStateOf<List<Int>>(emptyList())
        private set

    // ==================== 撤销/重做 ====================
    val commandManager = CommandManager(50)

    val canUndo: Boolean get() = commandManager.canUndo()
    val canRedo: Boolean get() = commandManager.canRedo()

    // ==================== 渲染引擎 ====================
    private val drawEngine: DrawEngine = Canvas2DEngine()
    private val layerCompositor = LayerCompositor()

    // ==================== 当前正在绘制的笔画 ====================
    private var currentStroke: MutableList<StrokePoint>? = null

    // ==================== 面板状态 ====================
    var isBrushSettingsVisible by mutableStateOf(false)
        private set

    var isColorPickerVisible by mutableStateOf(false)
        private set

    var isLayerPanelVisible by mutableStateOf(false)
        private set

    /** 笔刷库面板是否可见 */
    var isBrushLibraryVisible by mutableStateOf(false)
        private set

    // ==================== 导出状态 ====================
    var isExporting by mutableStateOf(false)
        private set

    var exportMessage by mutableStateOf<String?>(null)
        private set

    // ==================== 项目管理状态 ====================
    
    /** 项目管理器 */
    private var projectManager: ProjectManager? = null
    
    /** 同步管理器（可选，用于云同步） */
    private var syncManager: com.jiafenbu.androidpaint.sync.SyncManager? = null
    
    /** 当前项目 ID */
    var currentProjectId by mutableStateOf<String?>(null)
        private set
    
    /** 当前项目名称 */
    var projectName by mutableStateOf("未命名作品")
        private set
    
    /** 是否为新项目（尚未保存过） */
    var isNewProject by mutableStateOf(true)
        private set
    
    /** 是否正在保存 */
    var isSaving by mutableStateOf(false)
        private set
    
    /** 保存消息 */
    var saveMessage by mutableStateOf<String?>(null)
        private set
    
    /** 同步状态 */
    var syncStatus by mutableStateOf<com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus>(
        com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.IDLE
    )
        private set
    
    /** 是否正在同步 */
    var isSyncing by mutableStateOf(false)
        private set

    // ==================== 阶段6：专业功能状态 ====================
    
    /** 工具模式 */
    var toolMode by mutableStateOf(ToolMode.DRAW)
        private set
    
    /** 当前选区 */
    var currentSelection by mutableStateOf<Selection?>(null)
        private set
    
    /** 魔棒选区容差 */
    var magicWandTolerance by mutableStateOf(32)
        private set
    
    /** 羽化半径 */
    var featherRadius by mutableStateOf(0f)
        private set
    
    /** 描边宽度 */
    var strokeWidth by mutableStateOf(2f)
        private set
    
    /** 变形目标范围 */
    var transformTarget by mutableStateOf(com.jiafenbu.androidpaint.model.TransformTarget.CURRENT_LAYER)
        private set
    
    /** 对称轴类型 */
    var symmetryAxis by mutableStateOf(SymmetryAxis.NONE)
        private set
    
    /** 网格类型 */
    var gridType by mutableStateOf(GridType.NONE)
        private set
    
    /** 参考图 */
    var referenceImage by mutableStateOf<ReferenceImage?>(null)
        private set
    
    /** 参考图面板是否可见 */
    var isReferencePanelVisible by mutableStateOf(false)
        private set
    
    /** 色板面板是否可见 */
    var isPalettePanelVisible by mutableStateOf(false)
        private set
    
    /** 选区工具栏是否可见 */
    var isSelectionToolbarVisible by mutableStateOf(false)
        private set
    
    /** 色板管理器 */
    private var paletteManager: PaletteManager? = null
    
    /** 当前选中的色板 ID */
    var selectedPaletteId by mutableStateOf<Long?>(null)
        private set
    
    /** 最近使用颜色 */
    var recentColors by mutableStateOf<List<Int>>(emptyList())
        private set
    
    // ==================== 阶段7：文字工具状态 ====================
    
    /** 文字工具面板是否可见 */
    var isTextToolPanelVisible by mutableStateOf(false)
        private set
    
    /** 当前正在编辑的文字图层 */
    var currentTextLayer by mutableStateOf<TextLayerModel?>(null)
        private set
    
    /** 文字输入位置（点击画布时的位置） */
    var textInputPosition by mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
        private set
    
    /** 正在编辑的文字图层索引 */
    var editingTextLayerIndex by mutableIntStateOf(-1)
        private set
    
    /** 字体管理器 */
    var fontManager by mutableStateOf<FontManager?>(null)
        private set
    
    /** 文字工具面板是否显示（用于输入新文字） */
    var isTextInputMode by mutableStateOf(false)
        private set
    
    // ==================== 阶段7：水印状态 ====================
    
    /** 水印配置 */
    var watermarkConfig by mutableStateOf<WatermarkConfig?>(null)
        private set
    
    /** 水印面板是否可见 */
    var isWatermarkPanelVisible by mutableStateOf(false)
        private set
    
    /** 是否在水印编辑模式 */
    var isWatermarkEditMode by mutableStateOf(false)
        private set
    
    // ==================== 选区绘制状态 ====================
    /** 当前正在绘制的选区类型 */
    var selectionType by mutableStateOf(com.jiafenbu.androidpaint.model.SelectionType.RECTANGLE)
        private set
    
    /** 选区绘制中的点列表 */
    private var selectionPoints: MutableList<androidx.compose.ui.geometry.Offset>? = null
    
    /** 选区起始点 */
    private var selectionStartPoint: androidx.compose.ui.geometry.Offset? = null
    
    init {
        // 初始化：创建一个默认图层
        initializeCanvas()
    }
    
    // ==================== 色板管理器设置 ====================
    
    /**
     * 设置色板管理器
     */
    fun setPaletteManager(manager: PaletteManager) {
        paletteManager = manager
        recentColors = manager.getRecentColors()
    }
    
    /**
     * 设置字体管理器
     */
    fun setFontManager(manager: FontManager) {
        fontManager = manager
    }
    
    // ==================== 项目管理方法 ====================
    
    /**
     * 设置项目管理器
     */
    fun setProjectManager(manager: ProjectManager) {
        projectManager = manager
    }
    
    /**
     * 设置同步管理器
     */
    fun setSyncManager(manager: com.jiafenbu.androidpaint.sync.SyncManager) {
        syncManager = manager
    }
    
    /**
     * 创建新项目
     */
    fun createNewProject(name: String, width: Int, height: Int) {
        viewModelScope.launch {
            val manager = projectManager ?: return@launch
            
            val projectId = manager.createProject(name, width, height)
            if (projectId != null) {
                currentProjectId = projectId
                projectName = name
                isNewProject = false
                initializeCanvas(width, height)
            } else {
                saveMessage = "创建项目失败"
            }
        }
    }
    
    /**
     * 加载项目
     */
    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val manager = projectManager ?: return@launch
            
            isLoading = true
            val result = manager.loadProject(projectId)
            isLoading = false
            
            if (result != null) {
                _layers.clear()
                cachedBitmaps.values.forEach {
                    if (!it.isRecycled) it.recycle()
                }
                cachedBitmaps.clear()
                commandManager.clear()
                
                canvasWidth = result.width
                canvasHeight = result.height
                drawEngine.initialize(result.width, result.height)
                
                _layers.addAll(result.layers)
                cachedBitmaps.putAll(result.layerBitmaps)
                activeLayerIndex = 0
                
                currentProjectId = projectId
                val projectInfo = manager.getProjectList().find { it.id == projectId }
                projectName = projectInfo?.name ?: "未命名作品"
                isNewProject = false
                
                saveMessage = "项目加载成功"
            } else {
                saveMessage = "加载项目失败"
            }
        }
    }
    
    /**
     * 保存当前项目
     */
    fun saveCurrentProject() {
        viewModelScope.launch {
            val manager = projectManager
            val projectId = currentProjectId
            
            if (manager == null || projectId == null) return@launch
            
            isSaving = true
            
            val mergedBitmap = getCompositedBitmap()
            
            val success = manager.saveProject(
                projectId = projectId,
                layers = _layers.toList(),
                layerBitmaps = cachedBitmaps.toMap(),
                width = canvasWidth,
                height = canvasHeight
            )
            
            isSaving = false
            isNewProject = false
            
            if (success) {
                val projectInfo = manager.getProjectList().find { it.id == projectId }
                projectInfo?.let {
                    projectName = it.name
                }
                triggerAutoSync(projectId)
            }
        }
    }
    
    /**
     * 快速保存
     */
    fun quickSave() {
        if (currentProjectId == null || isNewProject) {
            saveCurrentProject()
            return
        }
        
        viewModelScope.launch {
            val manager = projectManager ?: return@launch
            val projectId = currentProjectId ?: return@launch
            
            manager.saveProject(
                projectId = projectId,
                layers = _layers.toList(),
                layerBitmaps = cachedBitmaps.toMap(),
                width = canvasWidth,
                height = canvasHeight
            )
        }
    }
    
    /**
     * 更新项目名称
     */
    fun updateProjectName(name: String) {
        projectName = name
    }
    
    /**
     * 清除项目状态
     */
    fun clearProjectState() {
        quickSave()
        currentProjectId = null
        projectName = "未命名作品"
        isNewProject = true
    }
    
    /**
     * 清除保存消息
     */
    fun clearSaveMessage() {
        saveMessage = null
    }
    
    // ==================== 同步功能 ====================
    
    private fun triggerAutoSync(projectId: String) {
        val sync = syncManager ?: return
        
        viewModelScope.launch {
            isSyncing = true
            syncStatus = com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.UPLOADING
            
            val result = sync.autoSync(projectId)
            
            isSyncing = false
            
            result.fold(
                onSuccess = {
                    syncStatus = com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.IDLE
                },
                onFailure = { e ->
                    when (e) {
                        is com.jiafenbu.androidpaint.sync.ConflictException -> {
                            syncStatus = com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.CONFLICT
                            saveMessage = "检测到版本冲突，请手动解决"
                        }
                        else -> {
                            syncStatus = com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.ERROR
                        }
                    }
                }
            )
        }
    }
    
    fun manualSync() {
        val projectId = currentProjectId ?: return
        triggerAutoSync(projectId)
    }
    
    fun getSyncStatusIcon(): String {
        return syncManager?.getSyncStatusIcon(syncStatus) ?: "☁️"
    }
    
    fun resolveConflict(resolution: String) {
        val projectId = currentProjectId ?: return
        val sync = syncManager ?: return
        
        viewModelScope.launch {
            isSyncing = true
            syncStatus = com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.UPLOADING
            
            val result = sync.resolveConflict(projectId, resolution)
            
            isSyncing = false
            
            result.fold(
                onSuccess = {
                    syncStatus = com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.IDLE
                    saveMessage = "冲突已解决"
                },
                onFailure = { e ->
                    syncStatus = com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.ERROR
                    saveMessage = "解决冲突失败: ${e.message}"
                }
            )
        }
    }
    
    private var isLoading by mutableStateOf(false)
        private set

    // ==================== 画布初始化 ====================

    fun initializeCanvas(width: Int = canvasWidth, height: Int = canvasHeight) {
        canvasWidth = width
        canvasHeight = height

        drawEngine.initialize(width, height)

        _layers.clear()
        cachedBitmaps.values.forEach {
            if (!it.isRecycled) it.recycle()
        }
        cachedBitmaps.clear()
        commandManager.clear()

        val defaultLayer = LayerModel.create("图层 1")
        _layers.add(defaultLayer)
        activeLayerIndex = 0

        val layerBitmap = drawEngine.createBlankBitmap(width, height)
        cachedBitmaps[defaultLayer.id] = layerBitmap
    }

    // ==================== 笔画操作 ====================

    fun startStroke(x: Float, y: Float) {
        currentStroke = mutableListOf(StrokePoint(x, y))
    }

    fun continueStroke(x: Float, y: Float) {
        currentStroke?.add(StrokePoint(x, y))
    }

    fun endStroke() {
        val points = currentStroke ?: return
        if (points.size < 2) {
            currentStroke = null
            return
        }

        val strokeData = StrokeData(
            points = points.toList(),
            brushDescriptor = currentBrush,
            color = currentColor
        )

        val command = StrokeCommand(strokeData, activeLayerIndex)
        commandManager.execute(command)

        BrushDescriptor.saveParams(currentBrush)

        currentStroke = null
    }

    fun getCurrentStrokePoints(): List<StrokePoint>? = currentStroke?.toList()

    fun undo() {
        commandManager.undo()
    }

    fun redo() {
        commandManager.redo()
    }

    // ==================== 笔刷设置 ====================

    fun setBrushType(type: BrushType) {
        BrushDescriptor.saveParams(currentBrush)
        val savedDescriptor = BrushDescriptor.getSavedOrDefault(type)
        currentBrush = savedDescriptor
    }

    fun setBrushSize(size: Float) {
        currentBrush = currentBrush.copy(size = size.coerceIn(1f, 100f))
    }

    fun setBrushOpacity(opacity: Float) {
        currentBrush = currentBrush.copy(opacity = opacity.coerceIn(0.01f, 1f))
    }

    fun setBrushSpacing(spacing: Float) {
        currentBrush = currentBrush.copy(spacing = spacing.coerceIn(0.01f, 1f))
    }

    fun setBrushJitter(jitter: Float) {
        currentBrush = currentBrush.copy(jitter = jitter.coerceIn(0f, 1f))
    }

    // ==================== 颜色设置 ====================

    fun setColor(color: Int) {
        currentColor = color
        addToColorHistory(color)
        paletteManager?.addToRecentColors(color)
        recentColors = paletteManager?.getRecentColors() ?: emptyList()
    }

    private fun addToColorHistory(color: Int) {
        val history = colorHistory.toMutableList()
        history.remove(color)
        history.add(0, color)
        if (history.size > 20) {
            history.removeAt(history.lastIndex)
        }
        colorHistory = history
    }

    // ==================== 画布变换 ====================

    fun updateCanvasState(scale: Float, rotation: Float, offsetX: Float, offsetY: Float) {
        canvasState = CanvasState(
            scale = scale.coerceIn(CanvasState.MIN_SCALE, CanvasState.MAX_SCALE),
            rotation = rotation,
            offsetX = offsetX,
            offsetY = offsetY
        )
    }

    fun resetCanvasState() {
        canvasState = CanvasState()
    }

    // ==================== 工具模式 ====================
    
    /**
     * 设置工具模式
     */
    fun setToolMode(mode: ToolMode) {
        toolMode = mode
        // 切换到非绘画模式时清除当前笔画
        if (mode != ToolMode.DRAW) {
            currentStroke = null
        }
    }
    
    /**
     * 切换到选区模式
     */
    fun enterSelectionMode(type: com.jiafenbu.androidpaint.model.SelectionType) {
        toolMode = ToolMode.SELECTION
        selectionType = type
        isSelectionToolbarVisible = true
    }
    
    /**
     * 退出选区模式
     */
    fun exitSelectionMode() {
        toolMode = ToolMode.DRAW
        isSelectionToolbarVisible = false
    }
    
    /**
     * 切换到吸管模式
     */
    fun enterEyedropperMode() {
        toolMode = ToolMode.EYEDROPPER
    }
    
    /**
     * 退出吸管模式
     */
    fun exitEyedropperMode() {
        toolMode = ToolMode.DRAW
    }
    
    /**
     * 切换到变形模式
     */
    fun enterTransformMode() {
        if (currentSelection != null) {
            toolMode = ToolMode.TRANSFORM
        }
    }
    
    /**
     * 退出变形模式
     */
    fun exitTransformMode() {
        toolMode = if (currentSelection != null) ToolMode.SELECTION else ToolMode.DRAW
    }

    // ==================== 选区操作 ====================
    
    /**
     * 开始选区绘制
     */
    fun startSelection(x: Float, y: Float) {
        selectionStartPoint = androidx.compose.ui.geometry.Offset(x, y)
        selectionPoints = mutableListOf(selectionStartPoint!!)
        
        when (selectionType) {
            com.jiafenbu.androidpaint.model.SelectionType.RECTANGLE,
            com.jiafenbu.androidpaint.model.SelectionType.ELLIPSE -> {
                // 矩形和椭圆选区只需要起始点
            }
            com.jiafenbu.androidpaint.model.SelectionType.LASSO -> {
                // 套索选区需要记录所有点
            }
            com.jiafenbu.androidpaint.model.SelectionType.MAGIC_WAND -> {
                // 魔棒选区在抬起时创建
            }
        }
    }
    
    /**
     * 继续选区绘制
     */
    fun continueSelection(x: Float, y: Float) {
        when (selectionType) {
            com.jiafenbu.androidpaint.model.SelectionType.LASSO -> {
                selectionPoints?.add(androidx.compose.ui.geometry.Offset(x, y))
            }
            else -> {
                // 其他类型不需要实时更新
            }
        }
    }
    
    /**
     * 结束选区绘制
     */
    fun endSelection(x: Float, y: Float) {
        val startPoint = selectionStartPoint ?: return
        
        currentSelection = when (selectionType) {
            com.jiafenbu.androidpaint.model.SelectionType.RECTANGLE -> {
                SelectionEngine.createRectangleSelection(
                    startPoint.x, startPoint.y,
                    x, y,
                    featherRadius
                )
            }
            com.jiafenbu.androidpaint.model.SelectionType.ELLIPSE -> {
                val left = minOf(startPoint.x, x)
                val top = minOf(startPoint.y, y)
                val right = maxOf(startPoint.x, x)
                val bottom = maxOf(startPoint.y, y)
                SelectionEngine.createEllipseSelection(
                    android.graphics.RectF(left, top, right, bottom),
                    featherRadius
                )
            }
            com.jiafenbu.androidpaint.model.SelectionType.LASSO -> {
                selectionPoints?.let {
                    SelectionEngine.createLassoSelection(it, featherRadius)
                }
            }
            com.jiafenbu.androidpaint.model.SelectionType.MAGIC_WAND -> {
                // 获取当前活动图层的位图
                val bitmap = getCompositedBitmap()
                SelectionEngine.createMagicWandSelection(
                    bitmap,
                    x.toInt(), y.toInt(),
                    magicWandTolerance,
                    featherRadius
                )
            }
        }
        
        selectionStartPoint = null
        selectionPoints = null
        toolMode = ToolMode.SELECTION
        isSelectionToolbarVisible = true
    }
    
    /**
     * 获取当前选区路径点（用于套索绘制预览）
     */
    fun getSelectionPoints(): List<androidx.compose.ui.geometry.Offset>? = selectionPoints?.toList()
    
    /**
     * 获取选区起始点
     */
    fun getSelectionStartPoint(): androidx.compose.ui.geometry.Offset? = selectionStartPoint
    
    /**
     * 清除选区
     */
    fun clearSelection() {
        currentSelection = null
        toolMode = ToolMode.DRAW
        isSelectionToolbarVisible = false
    }
    
    /**
     * 反选
     */
    fun invertSelection() {
        currentSelection?.let {
            currentSelection = SelectionEngine.invertSelection(it, canvasWidth, canvasHeight)
        }
    }
    
    /**
     * 羽化选区
     */
    fun featherSelection(radius: Float) {
        currentSelection?.let {
            currentSelection = SelectionEngine.featherSelection(it, radius)
        }
    }
    
    /**
     * 设置魔棒容差
     */
    fun setMagicWandTolerance(tolerance: Int) {
        magicWandTolerance = tolerance.coerceIn(0, 255)
    }
    
    /**
     * 设置羽化半径
     */
    fun setFeatherRadius(radius: Float) {
        featherRadius = radius.coerceIn(0f, 100f)
    }
    
    /**
     * 设置描边宽度
     */
    fun setStrokeWidth(width: Float) {
        strokeWidth = width.coerceIn(1f, 50f)
    }
    
    // ==================== 选区操作命令 ====================
    
    /**
     * 填充选区
     */
    fun fillSelection(color: Int) {
        val selection = currentSelection ?: return
        val layer = activeLayer ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.SelectionFillCommand(
            activeLayerIndex, bitmap, selection, color
        )
        commandManager.execute(command)
    }
    
    /**
     * 清除选区内容
     */
    fun clearSelectionContent() {
        val selection = currentSelection ?: return
        val layer = activeLayer ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.SelectionClearCommand(bitmap, selection)
        commandManager.execute(command)
    }
    
    /**
     * 描边选区
     */
    fun strokeSelection(color: Int, width: Float) {
        val selection = currentSelection ?: return
        val layer = activeLayer ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.SelectionStrokeCommand(
            bitmap, selection, color, width
        )
        commandManager.execute(command)
    }
    
    /**
     * 复制选区到新图层
     */
    fun copySelectionToNewLayer(): Int? {
        val selection = currentSelection ?: return null
        val layer = activeLayer ?: return null
        val bitmap = cachedBitmaps[layer.id] ?: return null
        
        val command = com.jiafenbu.androidpaint.command.SelectionCopyCommand(bitmap, selection)
        command.execute()
        
        val copiedBitmap = command.getCopiedBitmap()
        if (copiedBitmap != null) {
            // 创建新图层
            val newLayer = LayerModel.create("选区复制 ${_layers.size + 1}")
            _layers.add(activeLayerIndex + 1, newLayer)
            cachedBitmaps[newLayer.id] = copiedBitmap
            activeLayerIndex = activeLayerIndex + 1
            return activeLayerIndex
        }
        return null
    }

    // ==================== 变形操作 ====================
    
    /**
     * 设置变形目标范围
     */
    fun setTransformTarget(target: com.jiafenbu.androidpaint.model.TransformTarget) {
        transformTarget = target
    }
    
    /**
     * 缩放选区
     */
    fun scaleSelection(scaleX: Float, scaleY: Float, pivotX: Float, pivotY: Float) {
        val selection = currentSelection ?: return
        val layer = activeLayer ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.FreeScaleCommand(
            bitmap, selection, scaleX, scaleY, pivotX, pivotY
        )
        commandManager.execute(command)
    }
    
    /**
     * 旋转选区
     */
    fun rotateSelection(angle: Float, pivotX: Float, pivotY: Float) {
        val selection = currentSelection ?: return
        val layer = activeLayer ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.FreeRotateCommand(
            bitmap, selection, angle, pivotX, pivotY
        )
        commandManager.execute(command)
    }
    
    /**
     * 水平翻转
     */
    fun flipSelectionHorizontal() {
        val selection = currentSelection ?: return
        val layer = activeLayer ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.FlipCommand(bitmap, selection, true)
        commandManager.execute(command)
    }
    
    /**
     * 垂直翻转
     */
    fun flipSelectionVertical() {
        val selection = currentSelection ?: return
        val layer = activeLayer ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.FlipCommand(bitmap, selection, false)
        commandManager.execute(command)
    }

    // ==================== 对称绘制 ====================
    
    /**
     * 设置对称轴
     */
    fun setSymmetryAxis(axis: SymmetryAxis) {
        symmetryAxis = axis
    }
    
    /**
     * 切换对称绘制
     */
    fun toggleSymmetry() {
        symmetryAxis = when (symmetryAxis) {
            SymmetryAxis.NONE -> SymmetryAxis.VERTICAL
            SymmetryAxis.VERTICAL -> SymmetryAxis.HORIZONTAL
            SymmetryAxis.HORIZONTAL -> SymmetryAxis.RADIAL
            SymmetryAxis.RADIAL -> SymmetryAxis.NONE
        }
    }

    // ==================== 网格 ====================
    
    /**
     * 设置网格类型
     */
    fun setGridType(type: GridType) {
        gridType = type
    }
    
    /**
     * 切换网格
     */
    fun toggleGrid() {
        gridType = when (gridType) {
            GridType.NONE -> GridType.RULE_OF_THIRDS
            GridType.RULE_OF_THIRDS -> GridType.PERSPECTIVE
            GridType.PERSPECTIVE -> GridType.NONE
        }
    }

    // ==================== 参考图 ====================
    
    /**
     * 设置参考图
     */
    fun setReferenceImage(uri: String) {
        referenceImage = ReferenceImage(uri = uri)
        isReferencePanelVisible = true
    }
    
    /**
     * 清除参考图
     */
    fun clearReferenceImage() {
        referenceImage = null
        isReferencePanelVisible = false
    }
    
    /**
     * 更新参考图设置
     */
    fun updateReferenceImage(image: ReferenceImage) {
        referenceImage = image
    }
    
    /**
     * 切换参考图面板
     */
    fun toggleReferencePanel() {
        isReferencePanelVisible = !isReferencePanelVisible
    }

    // ==================== 色板 ====================
    
    /**
     * 切换色板面板
     */
    fun togglePalettePanel() {
        isPalettePanelVisible = !isPalettePanelVisible
    }
    
    /**
     * 关闭色板面板
     */
    fun hidePalettePanel() {
        isPalettePanelVisible = false
    }
    
    /**
     * 选择色板
     */
    fun selectPalette(paletteId: Long) {
        selectedPaletteId = paletteId
    }
    
    /**
     * 获取所有色板
     */
    fun getAllPalettes(): List<Palette> {
        val defaultPalettes = Palette.getDefaultPalettes()
        val customPalettes = paletteManager?.getCustomPalettes() ?: emptyList()
        return defaultPalettes + customPalettes
    }
    
    /**
     * 创建新色板
     */
    fun createPalette(name: String) {
        paletteManager?.createPalette(name)
    }
    
    /**
     * 删除色板
     */
    fun deletePalette(paletteId: Long) {
        paletteManager?.deletePalette(paletteId)
        if (selectedPaletteId == paletteId) {
            selectedPaletteId = null
        }
    }
    
    /**
     * 添加颜色到色板
     */
    fun addColorToPalette(paletteId: Long, color: Int) {
        paletteManager?.addColorToPalette(paletteId, com.jiafenbu.androidpaint.model.PaletteColor(color))
    }
    
    /**
     * 从图片提取颜色创建色板
     */
    fun extractColorsFromImage(bitmap: Bitmap, name: String, colorCount: Int = 8) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val colors = com.jiafenbu.androidpaint.palette.AseFileParser.extractColorsFromImage(
            pixels, bitmap.width, bitmap.height, colorCount
        )
        
        paletteManager?.createPalette(name, colors)
    }

    // ==================== 阶段7：文字工具方法 ====================
    
    /**
     * 切换到文字工具模式
     */
    fun enterTextMode() {
        toolMode = ToolMode.TEXT
        isTextInputMode = true
        closeAllPanels()
        // 创建新的文字模型
        currentTextLayer = TextLayerModel.create()
    }
    
    /**
     * 退出文字工具模式
     */
    fun exitTextMode() {
        toolMode = ToolMode.DRAW
        isTextInputMode = false
        isTextToolPanelVisible = false
        currentTextLayer = null
        editingTextLayerIndex = -1
    }
    
    /**
     * 设置文字输入位置（点击画布时调用）
     */
    fun setTextInputPosition(x: Float, y: Float) {
        textInputPosition = androidx.compose.ui.geometry.Offset(x, y)
    }
    
    /**
     * 切换文字工具面板
     */
    fun toggleTextToolPanel() {
        isTextToolPanelVisible = !isTextToolPanelVisible
        if (isTextToolPanelVisible) {
            closeAllPanels()
        }
    }
    
    /**
     * 更新当前文字内容
     */
    fun updateCurrentText(textModel: TextLayerModel) {
        currentTextLayer = textModel.copy(position = textInputPosition)
    }
    
    /**
     * 确认添加文字图层
     */
    fun confirmAddTextLayer() {
        val textModel = currentTextLayer ?: return
        if (textModel.text.isEmpty()) return
        
        val textLayer = textModel.copy(position = textInputPosition)
        val command = com.jiafenbu.androidpaint.command.AddTextLayerCommand(
            layers = _layers,
            cachedBitmaps = cachedBitmaps,
            activeLayerIndex = activeLayerIndex,
            textModel = textLayer,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            createBlankBitmap = { w, h -> drawEngine.createBlankBitmap(w, h) },
            layerIdGenerator = { LayerModel.getNextId().also { LayerModel.resetIdCounter() } }
        )
        commandManager.execute(command)
        
        // 选中新添加的图层
        activeLayerIndex = activeLayerIndex + 1
        
        // 重置状态
        currentTextLayer = null
        isTextInputMode = false
        exitTextMode()
    }
    
    /**
     * 编辑现有文字图层
     */
    fun editTextLayer(layerIndex: Int) {
        val layer = _layers.getOrNull(layerIndex) ?: return
        if (layer.layerType != com.jiafenbu.androidpaint.model.LayerType.TEXT) return
        if (layer.textLayerModel == null) return
        
        editingTextLayerIndex = layerIndex
        currentTextLayer = layer.textLayerModel
        textInputPosition = layer.textLayerModel.position
        isTextToolPanelVisible = true
        isTextInputMode = false
    }
    
    /**
     * 确认修改文字图层
     */
    fun confirmEditTextLayer(newTextModel: TextLayerModel) {
        val layerIndex = editingTextLayerIndex
        if (layerIndex < 0 || layerIndex >= _layers.size) return
        
        val layer = _layers[layerIndex]
        val oldTextModel = layer.textLayerModel ?: return
        
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.ModifyTextCommand(
            layer = layer,
            cachedBitmap = bitmap,
            oldTextModel = oldTextModel,
            newTextModel = newTextModel,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            createBlankBitmap = { w, h -> drawEngine.createBlankBitmap(w, h) }
        )
        commandManager.execute(command)
        
        editingTextLayerIndex = -1
        currentTextLayer = null
        isTextToolPanelVisible = false
    }
    
    /**
     * 光栅化文字图层
     */
    fun rasterizeTextLayer(layerIndex: Int) {
        val layer = _layers.getOrNull(layerIndex) ?: return
        if (layer.layerType != com.jiafenbu.androidpaint.model.LayerType.TEXT) return
        val textModel = layer.textLayerModel ?: return
        
        val bitmap = cachedBitmaps[layer.id] ?: return
        
        val command = com.jiafenbu.androidpaint.command.RasterizeTextLayerCommand(
            layer = layer,
            cachedBitmap = bitmap,
            textModel = textModel,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            createBlankBitmap = { w, h -> drawEngine.createBlankBitmap(w, h) }
        )
        commandManager.execute(command)
        
        editingTextLayerIndex = -1
        currentTextLayer = null
        isTextToolPanelVisible = false
    }
    
    /**
     * 取消文字输入
     */
    fun cancelTextInput() {
        currentTextLayer = null
        isTextInputMode = false
        editingTextLayerIndex = -1
        if (toolMode == ToolMode.TEXT) {
            exitTextMode()
        }
    }

    // ==================== 阶段7：水印方法 ====================
    
    /**
     * 切换水印面板
     */
    fun toggleWatermarkPanel() {
        isWatermarkPanelVisible = !isWatermarkPanelVisible
        if (isWatermarkPanelVisible) {
            closeAllPanels()
        }
    }
    
    /**
     * 退出水印模式
     */
    fun exitWatermarkMode() {
        isWatermarkEditMode = false
        if (toolMode == ToolMode.WATERMARK) {
            toolMode = ToolMode.DRAW
        }
    }
    
    /**
     * 更新水印配置
     */
    fun updateWatermarkConfig(config: WatermarkConfig) {
        val oldConfig = watermarkConfig
        if (oldConfig != null) {
            val command = com.jiafenbu.androidpaint.command.ModifyWatermarkCommand(
                oldConfig = oldConfig,
                newConfig = config,
                watermarkList = mutableListOf<WatermarkConfig>().apply { 
                    watermarkConfig?.let { add(it) } 
                }
            )
            commandManager.execute(command)
        }
        watermarkConfig = config
    }
    
    /**
     * 添加水印
     */
    fun addWatermark(config: WatermarkConfig) {
        val command = com.jiafenbu.androidpaint.command.AddWatermarkCommand(
            watermarkConfig = config,
            watermarkList = mutableListOf<WatermarkConfig>().apply { 
                watermarkConfig?.let { add(it) } 
            }
        )
        commandManager.execute(command)
        watermarkConfig = config
    }
    
    /**
     * 删除水印
     */
    fun deleteWatermark() {
        val config = watermarkConfig ?: return
        val command = com.jiafenbu.androidpaint.command.DeleteWatermarkCommand(
            watermarkConfig = config,
            watermarkList = mutableListOf<WatermarkConfig>().apply { 
                watermarkConfig?.let { add(it) } 
            }
        )
        commandManager.execute(command)
        watermarkConfig = null
    }
    
    /**
     * 启用/禁用水印
     */
    fun setWatermarkEnabled(enabled: Boolean) {
        watermarkConfig = watermarkConfig?.copy(isEnabled = enabled)
    }
    
    /**
     * 获取水印位图（用于导出）
     */
    fun getWatermarkBitmap(): Bitmap? {
        val config = watermarkConfig ?: return null
        if (!config.isEnabled) return null
        
        return com.jiafenbu.androidpaint.command.WatermarkRenderer.renderWatermarkOverlay(
            config = config,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight
        )
    }

    // ==================== 吸管工具 ====================
    
    /**
     * 从画布取色
     */
    fun pickColorFromCanvas(x: Float, y: Float) {
        val bitmap = getCompositedBitmap()
        val px = x.toInt().coerceIn(0, bitmap.width - 1)
        val py = y.toInt().coerceIn(0, bitmap.height - 1)
        
        val pixel = bitmap.getPixel(px, py)
        setColor(pixel)
        
        // 退出吸管模式
        exitEyedropperMode()
    }

    // ==================== 图层操作 ====================

    fun addLayer() {
        val command = AddLayerCommand()
        commandManager.execute(command)
    }

    fun deleteLayer(index: Int) {
        if (_layers.size <= 1) return
        val command = DeleteLayerCommand(index)
        commandManager.execute(command)
    }

    fun moveLayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex < 0 || fromIndex >= _layers.size) return
        if (toIndex < 0 || toIndex >= _layers.size) return

        val command = MoveLayerCommand(fromIndex, toIndex)
        commandManager.execute(command)
    }

    fun duplicateLayer(index: Int): Int {
        val command = DuplicateLayerCommand(index)
        commandManager.execute(command)
        return activeLayerIndex
    }

    fun mergeDown(index: Int) {
        if (index <= 0 || index >= _layers.size) return
        val command = MergeDownCommand(index)
        commandManager.execute(command)
    }

    fun mergeVisibleLayers() {
        val command = MergeVisibleCommand()
        commandManager.execute(command)
    }

    fun renameLayer(index: Int, name: String) {
        if (index < 0 || index >= _layers.size) return
        val command = SetLayerPropertyCommand(index, LayerProperty.NAME, name)
        commandManager.execute(command)
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        if (index < 0 || index >= _layers.size) return
        val command = SetLayerPropertyCommand(index, LayerProperty.OPACITY, opacity.coerceIn(0f, 1f))
        commandManager.execute(command)
    }

    fun setLayerBlendMode(index: Int, blendMode: BlendMode) {
        if (index < 0 || index >= _layers.size) return
        val command = SetLayerPropertyCommand(index, LayerProperty.BLEND_MODE, blendMode)
        commandManager.execute(command)
    }

    fun setLayerVisibility(index: Int, isVisible: Boolean) {
        if (index < 0 || index >= _layers.size) return
        val command = SetLayerPropertyCommand(index, LayerProperty.VISIBILITY, isVisible)
        commandManager.execute(command)
    }

    fun setLayerLocked(index: Int, isLocked: Boolean) {
        if (index < 0 || index >= _layers.size) return
        val command = SetLayerPropertyCommand(index, LayerProperty.LOCKED, isLocked)
        commandManager.execute(command)
    }

    fun clearLayer(index: Int) {
        if (index < 0 || index >= _layers.size) return
        val command = ClearLayerCommand(index)
        commandManager.execute(command)
    }

    fun selectLayer(index: Int) {
        if (index < 0 || index >= _layers.size) return
        activeLayerIndex = index
        updateLayerThumbnail(index)
    }

    fun updateLayerThumbnail(index: Int) {
        val layer = _layers.getOrNull(index) ?: return
        val bitmap = cachedBitmaps[layer.id] ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val thumbnail = layerCompositor.generateThumbnail(bitmap, 80)
            withContext(Dispatchers.Main) {
                _layers[index] = LayerModel(
                    id = layer.id,
                    name = layer.name,
                    strokes = layer.strokes,
                    isVisible = layer.isVisible,
                    opacity = layer.opacity,
                    blendMode = layer.blendMode,
                    isLocked = layer.isLocked,
                    thumbnail = thumbnail,
                    layerType = layer.layerType,
                    textLayerModel = layer.textLayerModel
                )
            }
        }
    }

    fun getLayerBitmap(layerId: Long): Bitmap? = cachedBitmaps[layerId]

    // ==================== 面板控制 ====================

    fun toggleBrushSettings() {
        isBrushSettingsVisible = !isBrushSettingsVisible
        if (isBrushSettingsVisible) {
            closeAllPanels()
        }
    }

    fun hideBrushSettings() {
        isBrushSettingsVisible = false
    }

    fun toggleColorPicker() {
        isColorPickerVisible = !isColorPickerVisible
        if (isColorPickerVisible) {
            closeAllPanels()
            isColorPickerVisible = true
        }
    }

    fun hideColorPicker() {
        isColorPickerVisible = false
    }

    fun toggleLayerPanel() {
        isLayerPanelVisible = !isLayerPanelVisible
        if (isLayerPanelVisible) {
            closeAllPanels()
            isLayerPanelVisible = true
        }
    }

    fun hideLayerPanel() {
        isLayerPanelVisible = false
    }

    fun toggleBrushLibrary() {
        isBrushLibraryVisible = !isBrushLibraryVisible
        if (isBrushLibraryVisible) {
            closeAllPanels()
            isBrushLibraryVisible = true
        }
    }

    fun hideBrushLibrary() {
        isBrushLibraryVisible = false
    }
    
    /**
     * 关闭所有面板
     */
    private fun closeAllPanels() {
        isBrushSettingsVisible = false
        isColorPickerVisible = false
        isLayerPanelVisible = false
        isBrushLibraryVisible = false
        isPalettePanelVisible = false
    }

    // ==================== 导出功能 ====================

    fun exportPng(exportManager: com.jiafenbu.androidpaint.export.ExportManager) {
        isExporting = true
        viewModelScope.launch {
            val bitmap = getCompositedBitmap()
            val result = exportManager.exportAsPng(bitmap)
            isExporting = false
            exportMessage = if (result.isSuccess) "PNG 导出成功" else "PNG 导出失败: ${result.exceptionOrNull()?.message}"
        }
    }

    fun exportJpeg(exportManager: com.jiafenbu.androidpaint.export.ExportManager) {
        isExporting = true
        viewModelScope.launch {
            val bitmap = getCompositedBitmap()
            val result = exportManager.exportAsJpeg(bitmap)
            isExporting = false
            exportMessage = if (result.isSuccess) "JPEG 导出成功" else "JPEG 导出失败: ${result.exceptionOrNull()?.message}"
        }
    }

    fun clearExportMessage() {
        exportMessage = null
    }

    // ==================== 合成 ====================

    fun getCompositedBitmap(): Bitmap {
        val layerRenderDataList = _layers.mapNotNull { layer ->
            val bitmap = cachedBitmaps[layer.id] ?: return@mapNotNull null
            LayerRenderData(
                bitmap = bitmap,
                opacity = layer.opacity,
                isVisible = layer.isVisible,
                blendMode = layer.blendMode
            )
        }

        return layerCompositor.composite(canvasWidth, canvasHeight, layerRenderDataList)
    }

    // ==================== 命令实现 ====================

    inner class StrokeCommand(
        private val strokeData: StrokeData,
        private val layerIndex: Int
    ) : DrawCommand {

        override fun execute() {
            if (layerIndex < 0 || layerIndex >= _layers.size) return

            val layer = _layers[layerIndex]
            layer.strokes.add(strokeData)

            val bitmap = cachedBitmaps[layer.id] ?: return

            viewModelScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.Default) {
                    if (layer.isLocked) {
                        drawEngine.addStrokeWithLock(bitmap, bitmap, strokeData)
                    } else {
                        drawEngine.addStroke(bitmap, strokeData)
                    }
                }
            }
        }

        override fun undo() {
            if (layerIndex < 0 || layerIndex >= _layers.size) return

            val layer = _layers[layerIndex]
            layer.strokes.remove(strokeData)

            val bitmap = cachedBitmaps[layer.id] ?: return
            viewModelScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.Default) {
                    drawEngine.rerenderLayer(bitmap, layer.strokes)
                }
            }
        }
    }

    inner class ClearLayerCommand(private val layerIndex: Int) : DrawCommand {
        private var clearedStrokes: List<StrokeData> = emptyList()

        override fun execute() {
            if (layerIndex < 0 || layerIndex >= _layers.size) return

            val layer = _layers[layerIndex]
            clearedStrokes = layer.strokes.toList()
            layer.strokes.clear()

            val bitmap = cachedBitmaps[layer.id] ?: return
            viewModelScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.Default) {
                    drawEngine.rerenderLayer(bitmap, emptyList())
                }
            }
        }

        override fun undo() {
            if (layerIndex < 0 || layerIndex >= _layers.size) return

            val layer = _layers[layerIndex]
            layer.strokes.addAll(clearedStrokes)

            val bitmap = cachedBitmaps[layer.id] ?: return
            viewModelScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.Default) {
                    drawEngine.rerenderLayer(bitmap, clearedStrokes)
                }
            }
        }
    }

    inner class AddLayerCommand : DrawCommand {
        private var newLayer: LayerModel? = null
        private var insertedIndex: Int = -1

        override fun execute() {
            val layerName = "图层 ${_layers.size + 1}"
            newLayer = LayerModel.create(layerName)
            insertedIndex = activeLayerIndex + 1

            _layers.add(insertedIndex, newLayer!!)

            val layerBitmap = drawEngine.createBlankBitmap(canvasWidth, canvasHeight)
            cachedBitmaps[newLayer!!.id] = layerBitmap

            activeLayerIndex = insertedIndex
        }

        override fun undo() {
            newLayer?.let { layer ->
                _layers.remove(layer)
                cachedBitmaps[layer.id]?.let {
                    if (!it.isRecycled) it.recycle()
                }
                cachedBitmaps.remove(layer.id)
                activeLayerIndex = (insertedIndex - 1).coerceAtLeast(0)
            }
        }
    }

    inner class DeleteLayerCommand(private val index: Int) : DrawCommand {
        private var deletedLayer: LayerModel? = null
        private var deletedBitmap: Bitmap? = null

        override fun execute() {
            if (index < 0 || index >= _layers.size) return
            if (_layers.size <= 1) return

            deletedLayer = _layers[index]
            deletedBitmap = cachedBitmaps[deletedLayer!!.id]

            _layers.removeAt(index)

            activeLayerIndex = when {
                index >= _layers.size -> _layers.size - 1
                index > activeLayerIndex -> activeLayerIndex - 1
                else -> activeLayerIndex
            }
        }

        override fun undo() {
            deletedLayer?.let { layer ->
                _layers.add(index, layer)
                deletedBitmap?.let {
                    cachedBitmaps[layer.id] = it
                }

                if (index <= activeLayerIndex) {
                    activeLayerIndex++
                }
            }
        }
    }

    inner class MoveLayerCommand(
        private val fromIndex: Int,
        private val toIndex: Int
    ) : DrawCommand {
        private var actualToIndex: Int = toIndex

        override fun execute() {
            if (fromIndex == toIndex) return
            if (fromIndex < 0 || fromIndex >= _layers.size) return
            if (toIndex < 0 || toIndex >= _layers.size) return

            val layer = _layers.removeAt(fromIndex)
            actualToIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
            _layers.add(actualToIndex, layer)

            activeLayerIndex = when (activeLayerIndex) {
                fromIndex -> actualToIndex
                in (fromIndex + 1)..toIndex -> activeLayerIndex - 1
                in toIndex until fromIndex -> activeLayerIndex + 1
                else -> activeLayerIndex
            }
        }

        override fun undo() {
            val layer = _layers.removeAt(actualToIndex)
            _layers.add(fromIndex, layer)

            activeLayerIndex = when (activeLayerIndex) {
                actualToIndex -> fromIndex
                in (fromIndex + 1)..actualToIndex -> activeLayerIndex - 1
                in actualToIndex until fromIndex -> activeLayerIndex + 1
                else -> activeLayerIndex
            }
        }
    }

    inner class DuplicateLayerCommand(private val index: Int) : DrawCommand {
        private var duplicatedLayer: LayerModel? = null
        private var insertedIndex: Int = -1

        override fun execute() {
            if (index < 0 || index >= _layers.size) return

            val original = _layers[index]
            duplicatedLayer = original.duplicate()
            insertedIndex = index + 1

            _layers.add(insertedIndex, duplicatedLayer!!)

            val originalBitmap = cachedBitmaps[original.id] ?: return
            val newBitmap = Bitmap.createBitmap(
                originalBitmap.width,
                originalBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(newBitmap)
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)
            cachedBitmaps[duplicatedLayer!!.id] = newBitmap

            activeLayerIndex = insertedIndex
        }

        override fun undo() {
            duplicatedLayer?.let { layer ->
                _layers.removeAt(insertedIndex)
                cachedBitmaps[layer.id]?.let {
                    if (!it.isRecycled) it.recycle()
                }
                cachedBitmaps.remove(layer.id)
                activeLayerIndex = index
            }
        }
    }

    inner class MergeDownCommand(private val upperIndex: Int) : DrawCommand {
        private var upperLayer: LayerModel? = null
        private var lowerLayer: LayerModel? = null
        private var newBitmap: Bitmap? = null
        private var oldLowerStrokes: List<StrokeData> = emptyList()
        private var oldLowerBlendMode: BlendMode = BlendMode.NORMAL

        override fun execute() {
            if (upperIndex <= 0 || upperIndex >= _layers.size) return

            upperLayer = _layers[upperIndex]
            lowerLayer = _layers[upperIndex - 1]

            oldLowerStrokes = lowerLayer!!.strokes.toList()
            oldLowerBlendMode = lowerLayer!!.blendMode

            val upperBitmap = cachedBitmaps[upperLayer!!.id] ?: return
            val lowerBitmap = cachedBitmaps[lowerLayer!!.id] ?: return

            newBitmap = layerCompositor.mergeLayers(
                lowerBitmap,
                upperBitmap,
                upperLayer!!.blendMode,
                upperLayer!!.opacity
            )

            lowerLayer!!.strokes.addAll(upperLayer!!.strokes)
            lowerLayer!!.blendMode = BlendMode.NORMAL

            cachedBitmaps[lowerLayer!!.id] = newBitmap!!

            _layers.removeAt(upperIndex)

            activeLayerIndex = (upperIndex - 1).coerceAtLeast(0)
        }

        override fun undo() {
            upperLayer?.let { upper ->
                lowerLayer?.let { lower ->
                    _layers.add(upperIndex, upper)

                    lower.strokes.clear()
                    lower.strokes.addAll(oldLowerStrokes)
                    lower.blendMode = oldLowerBlendMode

                    val lowerBitmap = cachedBitmaps[lower.id]
                    newBitmap?.let {
                        if (!it.isRecycled) it.recycle()
                    }
                    cachedBitmaps[lower.id] = lowerBitmap ?: return

                    activeLayerIndex = upperIndex
                }
            }
        }
    }

    inner class MergeVisibleCommand : DrawCommand {
        private var mergedLayer: LayerModel? = null
        private var mergedBitmap: Bitmap? = null
        private var originalLayers: List<LayerModel> = emptyList()
        private var originalBitmaps: Map<Long, Bitmap> = emptyMap()

        override fun execute() {
            val visibleLayers = _layers.filter { it.isVisible }
            if (visibleLayers.size <= 1) return

            originalLayers = _layers.toList()
            originalBitmaps = cachedBitmaps.toMap()

            val newLayer = LayerModel.create("合并图层")
            mergedLayer = newLayer

            val mergedBitmap = getCompositedBitmap()
            this.mergedBitmap = mergedBitmap

            _layers.clear()
            _layers.add(newLayer)
            cachedBitmaps.clear()
            cachedBitmaps[newLayer.id] = mergedBitmap

            activeLayerIndex = 0
        }

        override fun undo() {
            _layers.clear()
            _layers.addAll(originalLayers)

            cachedBitmaps.values.forEach {
                if (!it.isRecycled) it.recycle()
            }
            cachedBitmaps.clear()
            cachedBitmaps.putAll(originalBitmaps)

            mergedBitmap?.let {
                if (!it.isRecycled) it.recycle()
            }
        }
    }

    inner class SetLayerPropertyCommand(
        private val index: Int,
        private val property: LayerProperty,
        private val value: Any
    ) : DrawCommand {

        private var oldValue: Any? = null

        override fun execute() {
            if (index < 0 || index >= _layers.size) return

            val layer = _layers[index]

            oldValue = when (property) {
                LayerProperty.NAME -> layer.name
                LayerProperty.OPACITY -> layer.opacity
                LayerProperty.BLEND_MODE -> layer.blendMode
                LayerProperty.VISIBILITY -> layer.isVisible
                LayerProperty.LOCKED -> layer.isLocked
            }

            _layers[index] = when (property) {
                LayerProperty.NAME -> layer.copy(name = value as String)
                LayerProperty.OPACITY -> layer.copy(opacity = value as Float)
                LayerProperty.BLEND_MODE -> layer.copy(blendMode = value as BlendMode)
                LayerProperty.VISIBILITY -> layer.copy(isVisible = value as Boolean)
                LayerProperty.LOCKED -> layer.copy(isLocked = value as Boolean)
            }
        }

        override fun undo() {
            if (index < 0 || index >= _layers.size) return
            if (oldValue == null) return

            val layer = _layers[index]

            _layers[index] = when (property) {
                LayerProperty.NAME -> layer.copy(name = oldValue as String)
                LayerProperty.OPACITY -> layer.copy(opacity = oldValue as Float)
                LayerProperty.BLEND_MODE -> layer.copy(blendMode = oldValue as BlendMode)
                LayerProperty.VISIBILITY -> layer.copy(isVisible = oldValue as Boolean)
                LayerProperty.LOCKED -> layer.copy(isLocked = oldValue as Boolean)
            }
        }
    }
}
