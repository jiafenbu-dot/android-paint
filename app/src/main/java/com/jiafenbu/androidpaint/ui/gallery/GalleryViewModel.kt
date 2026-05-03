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
import com.jiafenbu.androidpaint.sync.AuthManager
import com.jiafenbu.androidpaint.sync.ConflictInfo
import com.jiafenbu.androidpaint.sync.RemoteProjectInfo
import com.jiafenbu.androidpaint.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 画廊 ViewModel
 * 管理画廊界面的状态和数据
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    
    /** 项目管理器 */
    private val projectManager = ProjectManager(application)
    
    /** 认证管理器 */
    private val authManager = AuthManager(application)
    
    /** 同步管理器 */
    private val syncManager = SyncManager(application, authManager, projectManager)
    
    /** 是否已登录 */
    var isLoggedIn by mutableStateOf(authManager.isLoggedIn)
        private set
    
    /** 项目列表 */
    var projectList by mutableStateOf<List<ProjectInfo>>(emptyList())
        private set
    
    /** 远程项目列表 */
    var remoteProjectList by mutableStateOf<List<RemoteProjectInfo>>(emptyList())
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
    
    /** 当前冲突信息 */
    var currentConflict by mutableStateOf<ConflictInfo?>(null)
        private set
    
    /** 同步状态 */
    val syncStatus: StateFlow<SyncManager.SyncStatus> = syncManager.syncStatus
    
    /** 同步进度 */
    val syncProgress: StateFlow<Float> = syncManager.syncProgress
    
    /** 上次同步时间 */
    val lastSyncTime: StateFlow<Long?> = syncManager.lastSyncTime
    
    init {
        loadProjectList()
    }
    
    // ==================== 项目列表操作 ====================
    
    /**
     * 加载项目列表
     */
    fun loadProjectList() {
        viewModelScope.launch {
            isLoading = true
            try {
                projectList = projectManager.getProjectList()
                // 预加载缩略图
                loadThumbnails()
                
                // 如果已登录，刷新远程项目列表
                if (isLoggedIn && syncManager.isOnline()) {
                    refreshRemoteProjects()
                }
            } catch (e: Exception) {
                errorMessage = "加载项目列表失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * 刷新远程项目列表
     */
    private suspend fun refreshRemoteProjects() {
        val result = syncManager.fetchRemoteProjects()
        result.fold(
            onSuccess = { remoteProjects ->
                remoteProjectList = remoteProjects
            },
            onFailure = { /* 静默失败，不打扰用户 */ }
        )
    }
    
    /**
     * 刷新同步状态
     */
    fun refreshSyncStatus() {
        viewModelScope.launch {
            if (isLoggedIn) {
                refreshRemoteProjects()
            }
        }
    }
    
    /**
     * 加载所有项目的缩略图
     */
    private fun loadThumbnails() {
        projectList.forEach { project ->
            if (!thumbnailCache.containsKey(project.id)) {
                thumbnailCache[project.id] = projectManager.getProjectThumbnail(project.id)
            }
        }
    }
    
    /**
     * 获取项目缩略图
     */
    fun getThumbnail(projectId: String): Bitmap? {
        return thumbnailCache[projectId]
    }
    
    /**
     * 创建新项目
     */
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
    
    /**
     * 删除项目
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            try {
                val success = projectManager.deleteProject(projectId)
                if (success) {
                    thumbnailCache.remove(projectId)
                    selectedProjectIds = selectedProjectIds - projectId
                    loadProjectList()
                } else {
                    errorMessage = "删除项目失败"
                }
            } catch (e: Exception) {
                errorMessage = "删除项目失败: ${e.message}"
            }
        }
    }
    
    /**
     * 删除多个项目
     */
    fun deleteSelectedProjects() {
        viewModelScope.launch {
            try {
                selectedProjectIds.forEach { projectId ->
                    projectManager.deleteProject(projectId)
                    thumbnailCache.remove(projectId)
                }
                selectedProjectIds = emptySet()
                isMultiSelectMode = false
                loadProjectList()
            } catch (e: Exception) {
                errorMessage = "删除项目失败: ${e.message}"
            }
        }
    }
    
    /**
     * 复制项目
     */
    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            try {
                val newProjectId = projectManager.duplicateProject(projectId)
                if (newProjectId != null) {
                    loadProjectList()
                } else {
                    errorMessage = "复制项目失败"
                }
            } catch (e: Exception) {
                errorMessage = "复制项目失败: ${e.message}"
            }
        }
    }
    
    /**
     * 重命名项目
     */
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
    
    /**
     * 获取项目信息
     */
    fun getProjectInfo(projectId: String): ProjectInfo? {
        return projectList.find { it.id == projectId }
    }
    
    // ==================== 多选模式操作 ====================
    
    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode() {
        isMultiSelectMode = true
        selectedProjectIds = emptySet()
    }
    
    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedProjectIds = emptySet()
    }
    
    /**
     * 切换项目选中状态
     */
    fun toggleProjectSelection(projectId: String) {
        selectedProjectIds = if (projectId in selectedProjectIds) {
            selectedProjectIds - projectId
        } else {
            selectedProjectIds + projectId
        }
    }
    
    /**
     * 全选
     */
    fun selectAll() {
        selectedProjectIds = projectList.map { it.id }.toSet()
    }
    
    /**
     * 取消全选
     */
    fun deselectAll() {
        selectedProjectIds = emptySet()
    }
    
    /**
     * 是否全选
     */
    val isAllSelected: Boolean
        get() = projectList.isNotEmpty() && selectedProjectIds.size == projectList.size
    
    /**
     * 是否有选中项目
     */
    val hasSelection: Boolean
        get() = selectedProjectIds.isNotEmpty()
    
    /**
     * 选中项目数量
     */
    val selectionCount: Int
        get() = selectedProjectIds.size
    
    // ==================== 错误处理 ====================
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * 获取项目管理器实例
     */
    fun getProjectManager(): ProjectManager = projectManager
    
    /**
     * 获取同步管理器实例
     */
    fun getSyncManager(): SyncManager = syncManager
    
    /**
     * 获取认证管理器实例
     */
    fun getAuthManager(): AuthManager = authManager
    
    // ==================== 同步功能 ====================
    
    /**
     * 上传项目到云端
     */
    fun uploadProject(projectId: String) {
        viewModelScope.launch {
            val result = syncManager.uploadProject(projectId)
            result.fold(
                onSuccess = {
                    errorMessage = null
                    refreshSyncStatus()
                },
                onFailure = { e ->
                    when (e) {
                        is com.jiafenbu.androidpaint.sync.ConflictException -> {
                            currentConflict = e.conflictInfo
                        }
                        else -> {
                            errorMessage = "上传失败: ${e.message}"
                        }
                    }
                }
            )
        }
    }
    
    /**
     * 下载项目到本地
     */
    fun downloadProject(projectId: String) {
        viewModelScope.launch {
            val result = syncManager.downloadProject(projectId)
            result.fold(
                onSuccess = {
                    errorMessage = null
                    loadProjectList()
                },
                onFailure = { e ->
                    errorMessage = "下载失败: ${e.message}"
                }
            )
        }
    }
    
    /**
     * 解决同步冲突
     * @param resolution "local" = 保留本地版本, "remote" = 使用远程版本
     */
    fun resolveConflict(projectId: String, resolution: String) {
        viewModelScope.launch {
            val result = syncManager.resolveConflict(projectId, resolution)
            currentConflict = null
            
            result.fold(
                onSuccess = {
                    loadProjectList()
                },
                onFailure = { e ->
                    errorMessage = "解决冲突失败: ${e.message}"
                }
            )
        }
    }
    
    /**
     * 清除冲突状态
     */
    fun clearConflict() {
        currentConflict = null
        syncManager.clearConflict()
    }
    
    /**
     * 获取项目的同步状态图标
     */
    fun getProjectSyncStatusIcon(projectId: String): String {
        val localProject = projectList.find { it.id == projectId }
        val remoteProject = remoteProjectList.find { it.id == projectId }
        
        if (remoteProject == null) {
            // 远程不存在，说明是本地项目
            return if (isLoggedIn) "⬆️" else "☁️"
        }
        
        val localModified = localProject?.modifiedAt ?: 0
        val remoteModified = remoteProject.modifiedAt * 1000  // 服务器返回秒，本地是毫秒
        
        return when {
            localModified > remoteModified -> "⬆️"  // 本地更新
            localModified < remoteModified -> "⬇️"  // 远程更新
            else -> "☁️"  // 已同步
        }
    }
    
    /**
     * 更新登录状态
     */
    fun updateLoginState() {
        isLoggedIn = authManager.isLoggedIn
        if (isLoggedIn) {
            refreshSyncStatus()
        }
    }
    
    /**
     * 登出
     */
    fun logout() {
        authManager.logout()
        isLoggedIn = false
        remoteProjectList = emptyList()
    }
    
    override fun onCleared() {
        super.onCleared()
        // 清理缩略图缓存
        thumbnailCache.values.forEach { bitmap ->
            bitmap?.recycle()
        }
        thumbnailCache.clear()
    }
}
