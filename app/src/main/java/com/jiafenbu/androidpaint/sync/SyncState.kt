package com.jiafenbu.androidpaint.sync

/**
 * 同步状态数据模型
 * 定义与云端同步相关的所有数据结构
 */

/**
 * 远程项目信息
 */
data class RemoteProjectInfo(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val layerCount: Int,
    val fileSize: Long,
    val version: Int,
    val createdAt: Long,
    val modifiedAt: Long
)

/**
 * 项目元数据（用于 API 传输）
 */
data class ProjectMetadata(
    val name: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val layerCount: Int? = null,
    val fileSize: Long? = null,
    val version: Int? = null
)

/**
 * 认证响应
 */
data class AuthResponse(
    val success: Boolean,
    val userId: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isNewUser: Boolean? = null,
    val error: String? = null
)

/**
 * 认证结果（内部使用）
 */
data class AuthResult(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)

/**
 * 同步状态响应
 */
data class SyncStatusResponse(
    val projectId: String,
    val localVersion: Int,
    val lastModified: Long,
    val lastDevice: String
)

/**
 * 同步响应
 */
data class SyncResponse(
    val success: Boolean,
    val conflict: Boolean? = null,
    val localVersion: Int? = null,
    val remoteVersion: Int? = null,
    val remoteTimestamp: Long? = null,
    val remoteDevice: String? = null,
    val version: Int? = null,
    val message: String? = null,
    val hasUpdate: Boolean? = null,
    val hasFile: Boolean? = null,
    val metadata: RemoteProjectInfo? = null
)

/**
 * 冲突信息
 */
data class ConflictInfo(
    val projectId: String,
    val localVersion: Int,
    val remoteVersion: Int,
    val remoteTimestamp: Long,
    val remoteDevice: String
)

/**
 * API 错误响应
 */
data class ApiError(
    val error: String
)

/**
 * 发送验证码响应
 */
data class SendCodeResponse(
    val success: Boolean,
    val message: String? = null,
    val devCode: String? = null,  // 开发阶段返回的验证码
    val error: String? = null
)

/**
 * 项目列表响应
 */
data class ProjectListResponse(
    val projects: List<RemoteProjectInfo>
)

/**
 * 项目响应
 */
data class ProjectResponse(
    val project: RemoteProjectInfo
)

/**
 * 上传响应
 */
data class UploadResponse(
    val success: Boolean,
    val version: Int? = null,
    val error: String? = null
)
