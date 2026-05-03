package com.jiafenbu.androidpaint.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 同步 API 服务
 * 使用 Kotlin HttpURLConnection 实现 HTTP 请求（不依赖第三方库）
 * 
 * 超时设置：
 * - 连接超时：15秒
 * - 读写超时：60秒
 */
object SyncApiService {
    
    // TODO: 部署后替换为实际的 Worker URL
    private const val BASE_URL = "https://androidpaint-api.your-domain.workers.dev"
    
    // 超时配置
    private const val CONNECT_TIMEOUT = 15000  // 15秒
    private const val READ_TIMEOUT = 60000      // 60秒
    
    // 请求头
    private const val CONTENT_TYPE = "Content-Type: application/json; charset=utf-8"
    
    /**
     * 通用 HTTP POST 请求
     */
    private suspend fun post(endpoint: String, body: JSONObject, accessToken: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            val reader = BufferedReader(InputStreamReader(
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            ))
            
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            
            if (responseCode in 200..299) {
                Result.success(JSONObject(response))
            } else {
                val errorMsg = try {
                    JSONObject(response).optString("error", "Unknown error")
                } catch (e: Exception) {
                    response
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 通用 HTTP GET 请求
     */
    private suspend fun get(endpoint: String, accessToken: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Accept", "application/json")
                accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            
            val responseCode = connection.responseCode
            val reader = BufferedReader(InputStreamReader(
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            ))
            
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            
            if (responseCode in 200..299) {
                Result.success(JSONObject(response))
            } else {
                val errorMsg = try {
                    JSONObject(response).optString("error", "Unknown error")
                } catch (e: Exception) {
                    response
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 通用 HTTP PUT 请求（JSON）
     */
    private suspend fun put(endpoint: String, body: JSONObject, accessToken: String? = null, deviceId: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "PUT"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
                deviceId?.let { setRequestProperty("X-Device-Id", it) }
            }
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            val reader = BufferedReader(InputStreamReader(
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            ))
            
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            
            if (responseCode in 200..299) {
                Result.success(JSONObject(response))
            } else {
                val errorMsg = try {
                    JSONObject(response).optString("error", "Unknown error")
                } catch (e: Exception) {
                    response
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 通用 HTTP DELETE 请求
     */
    private suspend fun delete(endpoint: String, accessToken: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "DELETE"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Accept", "application/json")
                accessToken?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            
            val responseCode = connection.responseCode
            val reader = BufferedReader(InputStreamReader(
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            ))
            
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            
            if (responseCode in 200..299) {
                Result.success(JSONObject(response))
            } else {
                val errorMsg = try {
                    JSONObject(response).optString("error", "Unknown error")
                } catch (e: Exception) {
                    response
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 上传文件
     */
    private suspend fun uploadFile(endpoint: String, data: ByteArray, accessToken: String, deviceId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL$endpoint")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "PUT"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                doOutput = true
                setRequestProperty("Content-Type", "application/octet-stream")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("X-Device-Id", deviceId)
            }
            
            connection.outputStream.use { output ->
                output.write(data)
            }
            
            val responseCode = connection.responseCode
            val reader = BufferedReader(InputStreamReader(
                if (responseCode in 200..299) connection.inputStream else connection.errorStream
            ))
            
            val response = reader.readText()
            reader.close()
            connection.disconnect()
            
            if (responseCode in 200..299) {
                Result.success(JSONObject(response))
            } else {
                val errorMsg = try {
                    JSONObject(response).optString("error", "Unknown error")
                } catch (e: Exception) {
                    response
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 认证接口 ====================
    
    /**
     * 邮箱注册
     */
    suspend fun register(email: String, password: String): Result<AuthResponse> {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        
        return post("/api/auth/register", body).mapCatching { json ->
            AuthResponse(
                success = json.optBoolean("success"),
                userId = json.optString("userId").takeIf { it.isNotEmpty() },
                accessToken = json.optString("accessToken").takeIf { it.isNotEmpty() },
                refreshToken = json.optString("refreshToken").takeIf { it.isNotEmpty() },
                error = json.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }
    
    /**
     * 邮箱登录
     */
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
        
        return post("/api/auth/login", body).mapCatching { json ->
            AuthResponse(
                success = json.optBoolean("success"),
                userId = json.optString("userId").takeIf { it.isNotEmpty() },
                accessToken = json.optString("accessToken").takeIf { it.isNotEmpty() },
                refreshToken = json.optString("refreshToken").takeIf { it.isNotEmpty() },
                error = json.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }
    
    /**
     * 发送手机验证码
     */
    suspend fun sendPhoneCode(phone: String): Result<SendCodeResponse> {
        val body = JSONObject().apply {
            put("phone", phone)
        }
        
        return post("/api/auth/phone/send", body).mapCatching { json ->
            SendCodeResponse(
                success = json.optBoolean("success"),
                message = json.optString("message").takeIf { it.isNotEmpty() },
                devCode = json.optString("devCode").takeIf { it.isNotEmpty() },
                error = json.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }
    
    /**
     * 手机号登录
     */
    suspend fun phoneLogin(phone: String, code: String): Result<AuthResponse> {
        val body = JSONObject().apply {
            put("phone", phone)
            put("code", code)
        }
        
        return post("/api/auth/phone/login", body).mapCatching { json ->
            AuthResponse(
                success = json.optBoolean("success"),
                userId = json.optString("userId").takeIf { it.isNotEmpty() },
                accessToken = json.optString("accessToken").takeIf { it.isNotEmpty() },
                refreshToken = json.optString("refreshToken").takeIf { it.isNotEmpty() },
                isNewUser = json.optBoolean("isNewUser").takeIf { json.has("isNewUser") },
                error = json.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }
    
    /**
     * 微信登录
     */
    suspend fun wechatLogin(code: String): Result<AuthResponse> {
        val body = JSONObject().apply {
            put("code", code)
        }
        
        return post("/api/auth/wechat", body).mapCatching { json ->
            AuthResponse(
                success = json.optBoolean("success"),
                userId = json.optString("userId").takeIf { it.isNotEmpty() },
                accessToken = json.optString("accessToken").takeIf { it.isNotEmpty() },
                refreshToken = json.optString("refreshToken").takeIf { it.isNotEmpty() },
                isNewUser = json.optBoolean("isNewUser").takeIf { json.has("isNewUser") },
                error = json.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }
    
    /**
     * 刷新 Token
     */
    suspend fun refreshToken(refreshToken: String): Result<AuthResponse> {
        val body = JSONObject().apply {
            put("refreshToken", refreshToken)
        }
        
        return post("/api/auth/refresh", body).mapCatching { json ->
            AuthResponse(
                success = json.optBoolean("success"),
                accessToken = json.optString("accessToken").takeIf { it.isNotEmpty() },
                error = json.optString("error").takeIf { it.isNotEmpty() }
            )
        }
    }
    
    // ==================== 项目接口 ====================
    
    /**
     * 获取项目列表
     */
    suspend fun getProjects(accessToken: String): Result<List<RemoteProjectInfo>> {
        return get("/api/projects", accessToken).mapCatching { json ->
            val projectsArray = json.optJSONArray("projects") ?: JSONArray()
            (0 until projectsArray.length()).map { index ->
                val obj = projectsArray.getJSONObject(index)
                RemoteProjectInfo(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    width = obj.optInt("width"),
                    height = obj.optInt("height"),
                    layerCount = obj.optInt("layerCount"),
                    fileSize = obj.optLong("fileSize"),
                    version = obj.optInt("version"),
                    createdAt = obj.optLong("createdAt"),
                    modifiedAt = obj.optLong("modifiedAt")
                )
            }
        }
    }
    
    /**
     * 获取单个项目
     */
    suspend fun getProject(accessToken: String, projectId: String): Result<RemoteProjectInfo> {
        return get("/api/projects/$projectId", accessToken).mapCatching { json ->
            val obj = json.optJSONObject("project")
                ?: throw Exception("Project not found")
            RemoteProjectInfo(
                id = obj.optString("id"),
                name = obj.optString("name"),
                width = obj.optInt("width"),
                height = obj.optInt("height"),
                layerCount = obj.optInt("layerCount"),
                fileSize = obj.optLong("fileSize"),
                version = obj.optInt("version"),
                createdAt = obj.optLong("createdAt"),
                modifiedAt = obj.optLong("modifiedAt")
            )
        }
    }
    
    /**
     * 创建项目
     */
    suspend fun createProject(accessToken: String, name: String, width: Int, height: Int): Result<RemoteProjectInfo> {
        val body = JSONObject().apply {
            put("name", name)
            put("width", width)
            put("height", height)
        }
        
        return post("/api/projects", body, accessToken).mapCatching { json ->
            val obj = json.optJSONObject("project")
                ?: throw Exception("Failed to create project")
            RemoteProjectInfo(
                id = obj.optString("id"),
                name = obj.optString("name"),
                width = obj.optInt("width"),
                height = obj.optInt("height"),
                layerCount = obj.optInt("layerCount"),
                fileSize = obj.optLong("fileSize"),
                version = obj.optInt("version"),
                createdAt = obj.optLong("createdAt"),
                modifiedAt = obj.optLong("modifiedAt")
            )
        }
    }
    
    /**
     * 更新项目
     */
    suspend fun updateProject(accessToken: String, projectId: String, metadata: ProjectMetadata): Result<Unit> {
        val body = JSONObject().apply {
            metadata.name?.let { put("name", it) }
            metadata.width?.let { put("width", it) }
            metadata.height?.let { put("height", it) }
            metadata.layerCount?.let { put("layerCount", it) }
            metadata.fileSize?.let { put("fileSize", it) }
            metadata.version?.let { put("version", it) }
        }
        
        return put("/api/projects/$projectId", body, accessToken).mapCatching { }
    }
    
    /**
     * 删除项目
     */
    suspend fun deleteProject(accessToken: String, projectId: String): Result<Unit> {
        return delete("/api/projects/$projectId", accessToken).mapCatching { }
    }
    
    /**
     * 上传项目文件
     */
    suspend fun uploadFile(accessToken: String, projectId: String, fileData: ByteArray, deviceId: String): Result<Int> {
        return uploadFile("/api/projects/$projectId/upload", fileData, accessToken, deviceId).mapCatching { json ->
            json.optInt("version")
        }
    }
    
    /**
     * 下载项目文件
     */
    suspend fun downloadFile(accessToken: String, projectId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/api/projects/$projectId/download")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val data = connection.inputStream.readBytes()
                connection.disconnect()
                Result.success(data)
            } else {
                connection.disconnect()
                Result.failure(Exception("Download failed: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 同步接口 ====================
    
    /**
     * 获取同步状态
     */
    suspend fun getSyncStatus(accessToken: String, projectId: String): Result<SyncStatusResponse> {
        return get("/api/sync/status?projectId=$projectId", accessToken).mapCatching { json ->
            SyncStatusResponse(
                projectId = json.optString("projectId"),
                localVersion = json.optInt("localVersion"),
                lastModified = json.optLong("lastModified"),
                lastDevice = json.optString("lastDevice")
            )
        }
    }
    
    /**
     * 推送同步
     */
    suspend fun pushSync(accessToken: String, projectId: String, version: Int, deviceId: String): Result<SyncResponse> {
        val body = JSONObject().apply {
            put("projectId", projectId)
            put("version", version)
            put("deviceId", deviceId)
        }
        
        return post("/api/sync/push", body, accessToken).mapCatching { json ->
            SyncResponse(
                success = json.optBoolean("success"),
                conflict = json.optBoolean("conflict").takeIf { json.has("conflict") },
                localVersion = json.optInt("localVersion").takeIf { json.has("localVersion") },
                remoteVersion = json.optInt("remoteVersion").takeIf { json.has("remoteVersion") },
                remoteTimestamp = json.optLong("remoteTimestamp").takeIf { json.has("remoteTimestamp") },
                remoteDevice = json.optString("remoteDevice").takeIf { it.isNotEmpty() },
                message = json.optString("message").takeIf { it.isNotEmpty() },
                version = json.optInt("version").takeIf { json.has("version") }
            )
        }
    }
    
    /**
     * 拉取同步
     */
    suspend fun pullSync(accessToken: String, projectId: String, version: Int): Result<SyncResponse> {
        val body = JSONObject().apply {
            put("projectId", projectId)
            put("version", version)
        }
        
        return post("/api/sync/pull", body, accessToken).mapCatching { json ->
            val metadata = json.optJSONObject("metadata")?.let { obj ->
                RemoteProjectInfo(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    width = obj.optInt("width"),
                    height = obj.optInt("height"),
                    layerCount = obj.optInt("layerCount"),
                    fileSize = obj.optLong("fileSize"),
                    version = obj.optInt("version"),
                    createdAt = obj.optLong("createdAt"),
                    modifiedAt = obj.optLong("modifiedAt")
                )
            }
            
            SyncResponse(
                success = json.optBoolean("success"),
                hasUpdate = json.optBoolean("hasUpdate").takeIf { json.has("hasUpdate") },
                version = json.optInt("version").takeIf { json.has("version") },
                hasFile = json.optBoolean("hasFile").takeIf { json.has("hasFile") },
                metadata = metadata
            )
        }
    }
    
    /**
     * 解决冲突
     */
    suspend fun resolveConflict(accessToken: String, projectId: String, resolution: String): Result<SyncResponse> {
        val body = JSONObject().apply {
            put("projectId", projectId)
            put("resolution", resolution)
        }
        
        return post("/api/sync/resolve", body, accessToken).mapCatching { json ->
            SyncResponse(
                success = json.optBoolean("success"),
                message = json.optString("message").takeIf { it.isNotEmpty() }
            )
        }
    }
}
