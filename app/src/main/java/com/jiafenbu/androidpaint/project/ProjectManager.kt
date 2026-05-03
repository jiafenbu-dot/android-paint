package com.jiafenbu.androidpaint.project

import android.content.Context
import android.graphics.Bitmap
import com.jiafenbu.androidpaint.engine.DrawEngine
import com.jiafenbu.androidpaint.engine.Canvas2DEngine
import com.jiafenbu.androidpaint.engine.LayerRenderData
import com.jiafenbu.androidpaint.model.LayerModel
import com.jiafenbu.androidpaint.model.OraFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 项目管理器
 * 负责项目的创建、保存、加载、删除、复制、重命名等操作
 * 
 * 项目文件存储在: context.filesDir/projects/{projectId}.ora
 * 项目元数据存储在: context.filesDir/projects/{projectId}.meta.json
 */
class ProjectManager(private val context: Context) {
    
    companion object {
        private const val PROJECTS_DIR = "projects"
        private const val META_EXTENSION = ".meta.json"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm"
    }
    
    /** 项目存储目录 */
    val projectsDir: File by lazy {
        File(context.filesDir, PROJECTS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /** 渲染引擎 */
    private val drawEngine: DrawEngine = Canvas2DEngine()
    
    // ==================== 项目创建 ====================
    
    /**
     * 创建新项目
     * @param name 项目名称
     * @param width 画布宽度
     * @param height 画布高度
     * @return 项目ID，如果失败返回 null
     */
    suspend fun createProject(name: String, width: Int, height: Int): String? = withContext(Dispatchers.IO) {
        try {
            val projectId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            
            // 创建项目元数据
            val meta = ProjectMeta(
                id = projectId,
                name = name,
                width = width,
                height = height,
                layerCount = 1,
                fileSize = 0L,
                createdAt = now,
                modifiedAt = now,
                thumbnailPath = null
            )
            
            // 保存元数据
            saveMeta(meta)
            
            // 创建初始图层
            val layers = listOf(LayerModel.create("图层 1"))
            val bitmaps = mutableMapOf<Long, Bitmap>()
            bitmaps[layers[0].id] = drawEngine.createBlankBitmap(width, height)
            
            // 保存项目
            saveProject(projectId, layers, bitmaps, width, height)
            
            projectId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // ==================== 项目保存 ====================
    
    /**
     * 保存项目（ORA格式）
     * @param projectId 项目ID
     * @param layers 图层列表
     * @param layerBitmaps 图层位图映射
     * @param width 画布宽度
     * @param height 画布高度
     */
    suspend fun saveProject(
        projectId: String,
        layers: List<LayerModel>,
        layerBitmaps: Map<Long, Bitmap>,
        width: Int,
        height: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val oraFile = File(projectsDir, "$projectId.ora")
            var mergedBitmap: Bitmap? = null
            
            // 生成合并图像
            if (layers.isNotEmpty()) {
                val renderDataList = layers.map { layer ->
                    val bitmap = layerBitmaps[layer.id]
                    if (bitmap != null && !bitmap.isRecycled) {
                        LayerRenderData(
                            bitmap = bitmap,
                            opacity = layer.opacity,
                            isVisible = layer.isVisible,
                            blendMode = layer.blendMode
                        )
                    } else {
                        LayerRenderData(
                            bitmap = drawEngine.createBlankBitmap(width, height),
                            opacity = layer.opacity,
                            isVisible = layer.isVisible,
                            blendMode = layer.blendMode
                        )
                    }
                }
                mergedBitmap = drawEngine.compositeLayers(renderDataList)
            }
            
            // 保存为 ORA 格式
            FileOutputStream(oraFile).use { fos ->
                OraFileFormat.saveProject(
                    layerModels = layers,
                    layerBitmaps = layerBitmaps,
                    canvasWidth = width,
                    canvasHeight = height,
                    outputStream = fos,
                    mergedBitmap = mergedBitmap
                )
            }
            
            // 回收合并图像
            mergedBitmap?.recycle()
            
            // 保存缩略图
            val thumbnailFile = File(projectsDir, "$projectId.thumb.png")
            saveThumbnail(projectId, layers, layerBitmaps, width, height, thumbnailFile)
            
            // 更新元数据
            val meta = getMeta(projectId)?.copy(
                layerCount = layers.size,
                fileSize = oraFile.length(),
                modifiedAt = System.currentTimeMillis(),
                thumbnailPath = thumbnailFile.absolutePath
            ) ?: ProjectMeta(
                id = projectId,
                name = "未命名",
                width = width,
                height = height,
                layerCount = layers.size,
                fileSize = oraFile.length(),
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                thumbnailPath = thumbnailFile.absolutePath
            )
            saveMeta(meta)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 保存缩略图
     */
    private fun saveThumbnail(
        projectId: String,
        layers: List<LayerModel>,
        layerBitmaps: Map<Long, Bitmap>,
        width: Int,
        height: Int,
        outputFile: File
    ) {
        try {
            // 生成合并图像
            val renderDataList = layers.map { layer ->
                val bitmap = layerBitmaps[layer.id]
                if (bitmap != null && !bitmap.isRecycled) {
                    LayerRenderData(
                        bitmap = bitmap,
                        opacity = layer.opacity,
                        isVisible = layer.isVisible,
                        blendMode = layer.blendMode
                    )
                } else {
                    LayerRenderData(
                        bitmap = drawEngine.createBlankBitmap(width, height),
                        opacity = layer.opacity,
                        isVisible = layer.isVisible,
                        blendMode = layer.blendMode
                    )
                }
            }
            val mergedBitmap = drawEngine.compositeLayers(renderDataList)
            
            // 生成缩略图
            val thumbSize = 256
            val scale = minOf(thumbSize.toFloat() / width, thumbSize.toFloat() / height)
            val thumbWidth = (width * scale).toInt()
            val thumbHeight = (height * scale).toInt()
            val thumbnail = Bitmap.createScaledBitmap(mergedBitmap, thumbWidth, thumbHeight, true)
            
            // 保存
            FileOutputStream(outputFile).use { fos ->
                thumbnail.compress(Bitmap.CompressFormat.PNG, 90, fos)
            }
            
            mergedBitmap.recycle()
            thumbnail.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // ==================== 项目加载 ====================
    
    /**
     * 加载项目
     * @param projectId 项目ID
     * @return 加载结果（图层列表+位图+画布尺寸），如果失败返回 null
     */
    suspend fun loadProject(projectId: String): ProjectLoadResult? = withContext(Dispatchers.IO) {
        try {
            val oraFile = File(projectsDir, "$projectId.ora")
            if (!oraFile.exists()) return@withContext null
            
            // 完整加载 ORA 项目
            val oraData = FileInputStream(oraFile).use { fis ->
                OraFileFormat.loadProjectComplete(fis)
            }
            
            // 转换为应用图层模型
            val layers = mutableListOf<LayerModel>()
            val layerBitmaps = mutableMapOf<Long, Bitmap>()
            
            oraData.layers.forEachIndexed { index, oraLayer ->
                val layerId = LayerModel.getNextId()
                val layer = LayerModel(
                    id = layerId,
                    name = oraLayer.name,
                    strokes = mutableListOf(), // ORA 格式只保存渲染后的位图，不保存笔画数据
                    isVisible = oraLayer.isVisible,
                    opacity = oraLayer.opacity,
                    blendMode = oraLayer.blendMode,
                    isLocked = false
                )
                layers.add(layer)
                
                // 加载图层位图
                val key = "data/layer$index.png"
                oraData.layerBitmaps[key]?.let { bitmap ->
                    layerBitmaps[layerId] = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
                }
            }
            
            // 如果没有加载到位图，创建空白位图
            layers.forEach { layer ->
                if (!layerBitmaps.containsKey(layer.id)) {
                    layerBitmaps[layer.id] = drawEngine.createBlankBitmap(oraData.width, oraData.height)
                }
            }
            
            ProjectLoadResult(
                layers = layers,
                layerBitmaps = layerBitmaps,
                width = oraData.width,
                height = oraData.height
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // ==================== 项目删除 ====================
    
    /**
     * 删除项目
     * @param projectId 项目ID
     * @return 是否成功
     */
    suspend fun deleteProject(projectId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val oraFile = File(projectsDir, "$projectId.ora")
            val metaFile = File(projectsDir, "$projectId$META_EXTENSION")
            val thumbFile = File(projectsDir, "$projectId.thumb.png")
            
            oraFile.delete()
            metaFile.delete()
            thumbFile.delete()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ==================== 项目复制 ====================
    
    /**
     * 复制项目
     * @param projectId 项目ID
     * @return 新项目ID，失败返回 null
     */
    suspend fun duplicateProject(projectId: String): String? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(projectsDir, "$projectId.ora")
            val sourceMeta = getMeta(projectId) ?: return@withContext null
            
            val newProjectId = UUID.randomUUID().toString()
            val newOraFile = File(projectsDir, "$newProjectId.ora")
            
            // 复制 ORA 文件
            sourceFile.copyTo(newOraFile)
            
            // 复制并更新元数据
            val newMeta = sourceMeta.copy(
                id = newProjectId,
                name = "${sourceMeta.name} 副本",
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            saveMeta(newMeta)
            
            // 复制缩略图
            val sourceThumb = File(projectsDir, "$projectId.thumb.png")
            val newThumb = File(projectsDir, "$newProjectId.thumb.png")
            if (sourceThumb.exists()) {
                sourceThumb.copyTo(newThumb)
            }
            
            newProjectId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // ==================== 项目重命名 ====================
    
    /**
     * 重命名项目
     * @param projectId 项目ID
     * @param newName 新名称
     */
    suspend fun renameProject(projectId: String, newName: String) = withContext(Dispatchers.IO) {
        try {
            val meta = getMeta(projectId) ?: return@withContext
            saveMeta(meta.copy(name = newName, modifiedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // ==================== 项目列表 ====================
    
    /**
     * 获取所有项目列表（按修改时间排序，最新在前）
     */
    suspend fun getProjectList(): List<ProjectInfo> = withContext(Dispatchers.IO) {
        val projects = mutableListOf<ProjectInfo>()
        
        projectsDir.listFiles { file ->
            file.extension == "ora"
        }?.forEach { oraFile ->
            try {
                val projectId = oraFile.nameWithoutExtension
                val meta = getMeta(projectId)
                
                if (meta != null) {
                    projects.add(ProjectInfo(
                        id = meta.id,
                        name = meta.name,
                        width = meta.width,
                        height = meta.height,
                        layerCount = meta.layerCount,
                        fileSize = oraFile.length(),
                        createdAt = meta.createdAt,
                        modifiedAt = meta.modifiedAt,
                        thumbnailPath = meta.thumbnailPath
                    ))
                } else {
                    // 没有元数据文件，创建默认信息
                    projects.add(ProjectInfo(
                        id = projectId,
                        name = oraFile.nameWithoutExtension,
                        width = 0,
                        height = 0,
                        layerCount = 0,
                        fileSize = oraFile.length(),
                        createdAt = oraFile.lastModified(),
                        modifiedAt = oraFile.lastModified(),
                        thumbnailPath = null
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 按修改时间排序（最新在前）
        projects.sortedByDescending { it.modifiedAt }
    }
    
    // ==================== 缩略图 ====================
    
    /**
     * 获取项目缩略图
     */
    fun getProjectThumbnail(projectId: String): Bitmap? {
        return try {
            val thumbFile = File(projectsDir, "$projectId.thumb.png")
            if (thumbFile.exists()) {
                android.graphics.BitmapFactory.decodeFile(thumbFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 加载项目元数据
     */
    private fun getMeta(projectId: String): ProjectMeta? {
        return try {
            val metaFile = File(projectsDir, "$projectId$META_EXTENSION")
            if (metaFile.exists()) {
                val json = JSONObject(metaFile.readText())
                ProjectMeta(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    width = json.getInt("width"),
                    height = json.getInt("height"),
                    layerCount = json.getInt("layerCount"),
                    fileSize = json.getLong("fileSize"),
                    createdAt = json.getLong("createdAt"),
                    modifiedAt = json.getLong("modifiedAt"),
                    thumbnailPath = json.optString("thumbnailPath", null)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 保存项目元数据
     */
    private fun saveMeta(meta: ProjectMeta) {
        val metaFile = File(projectsDir, "${meta.id}$META_EXTENSION")
        val json = JSONObject().apply {
            put("id", meta.id)
            put("name", meta.name)
            put("width", meta.width)
            put("height", meta.height)
            put("layerCount", meta.layerCount)
            put("fileSize", meta.fileSize)
            put("createdAt", meta.createdAt)
            put("modifiedAt", meta.modifiedAt)
            put("thumbnailPath", meta.thumbnailPath ?: "")
        }
        metaFile.writeText(json.toString(2))
    }
    
    /**
     * 检查项目是否存在
     */
    fun projectExists(projectId: String): Boolean {
        val oraFile = File(projectsDir, "$projectId.ora")
        return oraFile.exists()
    }
    
    /**
     * 获取项目文件路径
     */
    fun getProjectFile(projectId: String): File {
        return File(projectsDir, "$projectId.ora")
    }
    
    /**
     * 更新项目元数据（用于同步后更新本地元数据）
     * 
     * @param projectId 项目ID
     * @param name 项目名称
     * @param width 画布宽度
     * @param height 画布高度
     * @param layerCount 图层数
     * @param fileSize 文件大小
     * @param modifiedAt 修改时间
     */
    suspend fun updateProjectMeta(
        projectId: String,
        name: String? = null,
        width: Int? = null,
        height: Int? = null,
        layerCount: Int? = null,
        fileSize: Long? = null,
        modifiedAt: Long? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val currentMeta = getMeta(projectId) ?: return@withContext
            
            val updatedMeta = currentMeta.copy(
                name = name ?: currentMeta.name,
                width = width ?: currentMeta.width,
                height = height ?: currentMeta.height,
                layerCount = layerCount ?: currentMeta.layerCount,
                fileSize = fileSize ?: currentMeta.fileSize,
                modifiedAt = modifiedAt ?: System.currentTimeMillis()
            )
            
            saveMeta(updatedMeta)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取项目元数据
     */
    fun getProjectMeta(projectId: String): ProjectInfo? {
        return getMeta(projectId)?.let { meta ->
            val oraFile = File(projectsDir, "$projectId.ora")
            ProjectInfo(
                id = meta.id,
                name = meta.name,
                width = meta.width,
                height = meta.height,
                layerCount = meta.layerCount,
                fileSize = if (oraFile.exists()) oraFile.length() else meta.fileSize,
                createdAt = meta.createdAt,
                modifiedAt = meta.modifiedAt,
                thumbnailPath = meta.thumbnailPath
            )
        }
    }
}

/**
 * 项目元数据（内部使用）
 */
private data class ProjectMeta(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val layerCount: Int,
    val fileSize: Long,
    val createdAt: Long,
    val modifiedAt: Long,
    val thumbnailPath: String?
)

/**
 * 项目信息（对外暴露）
 */
data class ProjectInfo(
    val id: String,          // 项目ID = 文件名（不含.ora）
    val name: String,        // 项目名称
    val width: Int,          // 画布宽度
    val height: Int,         // 画布高度
    val layerCount: Int,     // 图层数
    val fileSize: Long,      // 文件大小（字节）
    val createdAt: Long,     // 创建时间
    val modifiedAt: Long,    // 最后修改时间
    val thumbnailPath: String? // 缩略图路径
) {
    /**
     * 格式化文件大小
     */
    fun formattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> String.format("%.1f KB", fileSize / 1024.0)
            else -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
        }
    }
    
    /**
     * 格式化创建时间
     */
    fun formattedCreatedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }
    
    /**
     * 格式化修改时间
     */
    fun formattedModifiedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(modifiedAt))
    }
    
    /**
     * 格式化完整创建时间
     */
    fun formattedFullCreatedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }
    
    /**
     * 格式化完整修改时间
     */
    fun formattedFullModifiedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(modifiedAt))
    }
}

/**
 * 项目加载结果
 */
data class ProjectLoadResult(
    val layers: List<LayerModel>,
    val layerBitmaps: Map<Long, Bitmap>,
    val width: Int,
    val height: Int
)
