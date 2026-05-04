package com.jiafenbu.androidpaint.ui.gallery

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jiafenbu.androidpaint.project.ProjectInfo
import com.jiafenbu.androidpaint.project.ProjectManager
import kotlinx.coroutines.launch

/**
 * 画廊 ViewModel - 纯本地版
 * 去掉了云端同步和登录功能，只保留本地项目管理
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    
    /** 项目管理器 */
    private val projectManager = ProjectManager(application)
    
    /** 项目列表 */
    var projectList by mutableStateOf<List<ProjectInfo>>(emptyList())
        private set
    
    /** 是否正在加载 */
    var isLoading by mutableStateOf(false)
        private set
    
    /** 是否为空状态 */
    val isEmpty: Boolean
        get() = projectList.isEmpty() && !isLoading
    
    /** 是否为多选模式 */
    var isMultiSelectMode by mutableStateOf(false)
        private set
    
    /** 已选中的项目 ID 集合 */
    var selectedProjectIds by mutableStateOf<Set<String>>(emptySet())
        private set
    
    /** 选中的项目对应的缩略图缓存 */
    private val thumbnailCache = mutableMapOf<String, Bitmap?>()
    
    /** 错误消息 */
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    init {
        loadProjectList()
    }
    
    // ==================== 项目列表操作 ====================
    
    fun loadProjectList() {
        viewModelScope.launch {
            isLoading = true
            try {
                projectList = projectManager.getProjectList()
                loadThumbnails()
            } catch (e: Exception) {
                errorMessage = "加载项目列表失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    private fun loadThumbnails() {
        projectList.forEach { project ->
            if (!thumbnailCache.containsKey(project.id)) {
                thumbnailCache[project.id] = projectManager.getProjectThumbnail(project.id)
            }
        }
    }
    
    fun getThumbnail(projectId: String): Bitmap? {
        return thumbnailCache[projectId]
    }
    
    // ==================== 项目创建 ====================
    
    fun createProject(name: String, width: Int, height: Int, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            try {
                val projectId = projectManager.createProject(name, width, height)
                if (projectId != null) {
                    loadProjectList()
                    onSuccess(projectId)
                } else {
                    errorMessage = "创建项目失败"
                }
            } catch (e: Exception) {
                errorMessage = "创建项目失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // ==================== 项目操作 ====================
    
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                projectManager.deleteProject(projectId)
                thumbnailCache.remove(projectId)
                loadProjectList()
            } catch (e: Exception) {
                errorMessage = "删除项目失败: ${e.message}"
            }
        }
    }
    
    fun deleteSelectedProjects() {
        viewModelScope.launch {
            selectedProjectIds.forEach { projectId ->
                projectManager.deleteProject(projectId)
                thumbnailCache.remove(projectId)
            }
            exitMultiSelectMode()
            loadProjectList()
        }
    }
    
    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            try {
                projectManager.renameProject(projectId, newName)
                loadProjectList()
            } catch (e: Exception) {
                errorMessage = "重命名失败: ${e.message}"
            }
        }
    }
    
    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            try {
                projectManager.duplicateProject(projectId)
                loadProjectList()
            } catch (e: Exception) {
                errorMessage = "复制项目失败: ${e.message}"
            }
        }
    }
    
    // ==================== 多选模式 ====================
    
    fun toggleMultiSelectMode() {
        isMultiSelectMode = !isMultiSelectMode
        if (!isMultiSelectMode) {
            selectedProjectIds = emptySet()
        }
    }
    
    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedProjectIds = emptySet()
    }
    
    fun toggleProjectSelection(projectId: String) {
        selectedProjectIds = if (selectedProjectIds.contains(projectId)) {
            selectedProjectIds - projectId
        } else {
            selectedProjectIds + projectId
        }
    }
    
    // ==================== 工具方法 ====================
    
    fun getProjectManager(): ProjectManager = projectManager
    
    fun clearErrorMessage() {
        errorMessage = null
    }
    
    // ==================== 多选辅助 ====================
    
    val selectionCount: Int
        get() = selectedProjectIds.size
    
    val hasSelection: Boolean
        get() = selectedProjectIds.isNotEmpty()
    
    val isAllSelected: Boolean
        get() = selectedProjectIds.size == projectList.size && projectList.isNotEmpty()
    
    fun enterMultiSelectMode() {
        isMultiSelectMode = true
    }
    
    fun selectAll() {
        selectedProjectIds = projectList.map { it.id }.toSet()
    }
    
    fun deselectAll() {
        selectedProjectIds = emptySet()
    }
    
    fun clearError() {
        errorMessage = null
    }
    
    fun getProjectInfo(projectId: String): ProjectInfo? {
        return projectList.find { it.id == projectId }
    }
}
