package com.jiafenbu.androidpaint.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.jiafenbu.androidpaint.project.ProjectManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 同步管理器
 * 
 * 负责项目的云端同步功能：
 * - 上传本地项目到云端
 * - 从云端下载项目到本地
 * - 检测并解决同步冲突
 * - 自动同步（保存后触发）
 * 
 * 同步状态：
 * - IDLE: 空闲状态
 * - UPLOADING: 正在上传
 * - DOWNLOADING: 正在下载
 * - CONFLICT: 检测到冲突
 * - ERROR: 同步错误
 * - OFFLINE: 离线状态
 */
class SyncManager(
    private val context: Context,
    private val authManager: AuthManager,
    private val projectManager: ProjectManager
) {
    
    // ==================== 状态定义 ====================
    
    /**
     * 同步状态枚举
     */
    enum class SyncStatus {
        IDLE,       // 空闲
        UPLOADING,  // 上传中
        DOWNLOADING,// 下载中
        CONFLICT,   // 冲突
        ERROR,      // 错误
        OFFLINE     // 离线
    }
    
    // 同步状态流
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus
    
    // 上次同步时间
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime
    
    // 同步进度 (0-1)
    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress
    
    // 当前冲突信息
    private val _currentConflict = MutableStateFlow<ConflictInfo?>(null)
    val currentConflict: StateFlow<ConflictInfo?> = _currentConflict
    
    // 同步错误消息
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError
    
    // 设备 ID
    private val deviceId: String by lazy {
        // 尝试获取 Android ID
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        if (androidId.isNullOrEmpty()) {
            UUID.randomUUID().toString()
        } else {
            "android-$androidId"
        }
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 检查网络连接
     */
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * 上传项目到云端
     * 
     * 流程：
     * 1. 读取本地 ORA 文件
     * 2. 获取本地版本号
     * 3. 调用 API 检查远程版本
     * 4. 如果无冲突，上传
     * 5. 如果有冲突，返回冲突信息
     * 
     * @param projectId 项目 ID
     * @return 成功返回 null，冲突返回 ConflictInfo，失败返回错误消息
     */
    suspend fun uploadProject(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            _syncStatus.value = SyncStatus.OFFLINE
            return@withContext Result.failure(Exception("无网络连接"))
        }
        
        _syncStatus.value = SyncStatus.UPLOADING
        _syncProgress.value = 0f
        
        try {
            // 1. 获取 access token
            val accessTokenResult = authManager.ensureValidToken()
            if (accessTokenResult.isFailure) {
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(accessTokenResult.exceptionOrNull() ?: Exception("认证失败"))
            }
            val accessToken = accessTokenResult.getOrThrow()
            
            // 2. 获取本地项目元数据
            val localProject = projectManager.getProjectList().find { it.id == projectId }
            if (localProject == null) {
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("本地项目不存在"))
            }
            
            // 本地版本号（从文件修改时间生成一个简单的版本）
            val localVersion = generateLocalVersion(localProject.modifiedAt)
            _syncProgress.value = 0.2f
            
            // 3. 读取本地 ORA 文件
            val oraFile = File(projectManager.getProjectsDir(), "$projectId.ora")
            if (!oraFile.exists()) {
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("项目文件不存在"))
            }
            
            val fileData = oraFile.readBytes()
            _syncProgress.value = 0.4f
            
            // 4. 先尝试推送同步
            val pushResult = SyncApiService.pushSync(accessToken, projectId, localVersion, deviceId)
            
            if (pushResult.isFailure) {
                val error = pushResult.exceptionOrNull()?.message ?: "推送失败"
                // 如果是 401，可能是 token 过期
                if (error.contains("401")) {
                    authManager.logout()
                    _syncStatus.value = SyncStatus.ERROR
                    return@withContext Result.failure(Exception("登录已过期，请重新登录"))
                }
                _syncStatus.value = SyncStatus.ERROR
                _syncError.value = error
                return@withContext Result.failure(Exception(error))
            }
            
            val pushResponse = pushResult.getOrThrow()
            
            // 5. 检查是否有冲突
            if (pushResponse.conflict == true) {
                _syncStatus.value = SyncStatus.CONFLICT
                _currentConflict.value = ConflictInfo(
                    projectId = projectId,
                    localVersion = pushResponse.localVersion ?: localVersion,
                    remoteVersion = pushResponse.remoteVersion ?: 0,
                    remoteTimestamp = pushResponse.remoteTimestamp ?: 0,
                    remoteDevice = pushResponse.remoteDevice ?: "unknown"
                )
                _syncProgress.value = 1f
                return@withContext Result.failure(ConflictException("检测到版本冲突", _currentConflict.value))
            }
            
            // 6. 无冲突，上传文件
            _syncProgress.value = 0.6f
            
            val uploadResult = SyncApiService.uploadFile(accessToken, projectId, fileData, deviceId)
            
            if (uploadResult.isFailure) {
                _syncStatus.value = SyncStatus.ERROR
                _syncError.value = uploadResult.exceptionOrNull()?.message
                return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception("上传失败"))
            }
            
            _syncProgress.value = 1f
            _syncStatus.value = SyncStatus.IDLE
            _lastSyncTime.value = System.currentTimeMillis()
            
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            _syncError.value = e.message
            Result.failure(e)
        }
    }
    
    /**
     * 下载项目到本地
     * 
     * 流程：
     * 1. 调用 API 获取元数据
     * 2. 下载 ORA 文件
     * 3. 如果本地有修改，检测冲突
     * 
     * @param projectId 项目 ID
     */
    suspend fun downloadProject(projectId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            _syncStatus.value = SyncStatus.OFFLINE
            return@withContext Result.failure(Exception("无网络连接"))
        }
        
        _syncStatus.value = SyncStatus.DOWNLOADING
        _syncProgress.value = 0f
        
        try {
            // 1. 获取 access token
            val accessTokenResult = authManager.ensureValidToken()
            if (accessTokenResult.isFailure) {
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(accessTokenResult.exceptionOrNull() ?: Exception("认证失败"))
            }
            val accessToken = accessTokenResult.getOrThrow()
            
            // 2. 获取远程项目信息
            val remoteProjectResult = SyncApiService.getProject(accessToken, projectId)
            
            if (remoteProjectResult.isFailure) {
                _syncStatus.value = SyncStatus.ERROR
                _syncError.value = remoteProjectResult.exceptionOrNull()?.message
                return@withContext Result.failure(remoteProjectResult.exceptionOrNull() ?: Exception("获取项目信息失败"))
            }
            
            val remoteProject = remoteProjectResult.getOrThrow()
            _syncProgress.value = 0.3f
            
            // 3. 下载文件
            val downloadResult = SyncApiService.downloadFile(accessToken, projectId)
            
            if (downloadResult.isFailure) {
                _syncStatus.value = SyncStatus.ERROR
                _syncError.value = downloadResult.exceptionOrNull()?.message
                return@withContext Result.failure(downloadResult.exceptionOrNull() ?: Exception("下载失败"))
            }
            
            val fileData = downloadResult.getOrThrow()
            _syncProgress.value = 0.7f
            
            // 4. 保存到本地
            val oraFile = File(projectManager.getProjectsDir(), "$projectId.ora")
            oraFile.writeBytes(fileData)
            
            // 5. 更新本地元数据
            projectManager.updateProjectMeta(
                projectId = projectId,
                name = remoteProject.name,
                width = remoteProject.width,
                height = remoteProject.height,
                layerCount = remoteProject.layerCount,
                fileSize = remoteProject.fileSize,
                modifiedAt = remoteProject.modifiedAt
            )
            
            _syncProgress.value = 1f
            _syncStatus.value = SyncStatus.IDLE
            _lastSyncTime.value = System.currentTimeMillis()
            
            Result.success(Unit)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            _syncError.value = e.message
            Result.failure(e)
        }
    }
    
    /**
     * 自动同步：每次保存后自动调用
     * 
     * 检查网络 → 上传 → 更新同步状态
     * 
     * @param projectId 项目 ID
     */
    suspend fun autoSync(projectId: String): Result<Unit> {
        if (!isOnline()) {
            _syncStatus.value = SyncStatus.OFFLINE
            return Result.failure(Exception("无网络连接"))
        }
        
        // 异步执行上传
        return uploadProject(projectId)
    }
    
    /**
     * 拉取所有远程项目列表
     * 
     * @return 远程项目列表
     */
    suspend fun fetchRemoteProjects(): Result<List<RemoteProjectInfo>> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(Exception("无网络连接"))
        }
        
        try {
            val accessTokenResult = authManager.ensureValidToken()
            if (accessTokenResult.isFailure) {
                return@withContext Result.failure(accessTokenResult.exceptionOrNull() ?: Exception("认证失败"))
            }
            val accessToken = accessTokenResult.getOrThrow()
            
            val result = SyncApiService.getProjects(accessToken)
            result.map { it }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解决冲突
     * 
     * @param projectId 项目 ID
     * @param resolution 解决方案："local" = 保留本地版本, "remote" = 使用远程版本
     */
    suspend fun resolveConflict(projectId: String, resolution: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            _syncStatus.value = SyncStatus.OFFLINE
            return@withContext Result.failure(Exception("无网络连接"))
        }
        
        try {
            val accessTokenResult = authManager.ensureValidToken()
            if (accessTokenResult.isFailure) {
                return@withContext Result.failure(accessTokenResult.exceptionOrNull() ?: Exception("认证失败"))
            }
            val accessToken = accessTokenResult.getOrThrow()
            
            val result = SyncApiService.resolveConflict(accessToken, projectId, resolution)
            
            if (result.isFailure) {
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(result.exceptionOrNull() ?: Exception("解决冲突失败"))
            }
            
            _currentConflict.value = null
            
            when (resolution) {
                "local" -> {
                    // 上传本地版本
                    uploadProject(projectId)
                }
                "remote" -> {
                    // 下载远程版本
                    downloadProject(projectId)
                }
                else -> {
                    Result.failure(Exception("无效的解决方案"))
                }
            }
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            _syncError.value = e.message
            Result.failure(e)
        }
    }
    
    /**
     * 获取项目的同步状态
     * 
     * @param projectId 项目 ID
     * @return 同步状态信息
     */
    suspend fun getProjectSyncStatus(projectId: String): Result<SyncStatusResponse> = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext Result.failure(Exception("无网络连接"))
        }
        
        try {
            val accessTokenResult = authManager.ensureValidToken()
            if (accessTokenResult.isFailure) {
                return@withContext Result.failure(accessTokenResult.exceptionOrNull() ?: Exception("认证失败"))
            }
            val accessToken = accessTokenResult.getOrThrow()
            
            SyncApiService.getSyncStatus(accessToken, projectId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清除错误状态
     */
    fun clearError() {
        _syncError.value = null
        if (_syncStatus.value == SyncStatus.ERROR) {
            _syncStatus.value = SyncStatus.IDLE
        }
    }
    
    /**
     * 清除冲突状态
     */
    fun clearConflict() {
        _currentConflict.value = null
        if (_syncStatus.value == SyncStatus.CONFLICT) {
            _syncStatus.value = SyncStatus.IDLE
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 从修改时间生成简单版本号
     */
    private fun generateLocalVersion(modifiedAt: Long): Int {
        // 使用时间戳的高位部分作为版本号
        return (modifiedAt / 1000).toInt()
    }
    
    /**
     * 获取同步状态图标
     */
    fun getSyncStatusIcon(status: SyncStatus): String {
        return when (status) {
            SyncStatus.IDLE -> "☁️"        // 已同步
            SyncStatus.UPLOADING -> "⬆️"   // 待上传
            SyncStatus.DOWNLOADING -> "⬇️" // 待下载
            SyncStatus.CONFLICT -> "⚠️"   // 冲突
            SyncStatus.ERROR -> "❌"      // 错误
            SyncStatus.OFFLINE -> "🚫"    // 离线
        }
    }
    
    /**
     * 获取同步状态颜色
     */
    fun getSyncStatusColor(status: SyncStatus): Long {
        return when (status) {
            SyncStatus.IDLE -> 0xFF4CAF50 // 绿色
            SyncStatus.UPLOADING -> 0xFFFFC107 // 黄色
            SyncStatus.DOWNLOADING -> 0xFF2196F3 // 蓝色
            SyncStatus.CONFLICT -> 0xFFF44336 // 红色
            SyncStatus.ERROR -> 0xFFF44336 // 红色
            SyncStatus.OFFLINE -> 0xFF9E9E9E // 灰色
        }
    }
}

/**
 * 冲突异常
 */
class ConflictException(
    message: String,
    val conflictInfo: ConflictInfo?
) : Exception(message)
