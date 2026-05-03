package com.jiafenbu.androidpaint.sync

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 认证管理器
 * 
 * 管理用户的登录状态、Token 存储和刷新
 * - Access Token：存于内存，用于 API 请求
 * - Refresh Token：加密存储于 SharedPreferences
 * - 用户信息：加密存储
 * 
 * 注意：需要添加 AndroidX Security 依赖
 */
class AuthManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "auth_prefs"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ACCESS_TOKEN = "access_token"  // 备用，不推荐
    }
    
    // Token 存储（加密的 SharedPreferences）
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // 当前用户状态
    @Volatile
    var accessToken: String? = null
        private set
    
    @Volatile
    var refreshToken: String? = null
        private set
    
    @Volatile
    var userId: String? = null
        private set
    
    /**
     * 是否已登录
     */
    val isLoggedIn: Boolean
        get() = refreshToken != null && userId != null
    
    init {
        // 从加密存储加载
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        userId = prefs.getString(KEY_USER_ID, null)
        // Access token 不持久化到磁盘，只存内存
    }
    
    // ==================== 认证方法 ====================
    
    /**
     * 邮箱注册
     * @param email 邮箱地址
     * @param password 密码（至少8位，包含数字和字母）
     */
    suspend fun registerWithEmail(email: String, password: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        val response = SyncApiService.register(email, password)
        
        response.mapCatching { authResponse ->
            if (!authResponse.success || authResponse.accessToken == null || 
                authResponse.refreshToken == null || authResponse.userId == null) {
                throw Exception(authResponse.error ?: "注册失败")
            }
            
            // 保存认证信息
            saveAuthInfo(authResponse.userId, authResponse.accessToken, authResponse.refreshToken)
            
            AuthResult(
                userId = authResponse.userId,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )
        }
    }
    
    /**
     * 邮箱登录
     * @param email 邮箱地址
     * @param password 密码
     */
    suspend fun loginWithEmail(email: String, password: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        val response = SyncApiService.login(email, password)
        
        response.mapCatching { authResponse ->
            if (!authResponse.success || authResponse.accessToken == null || 
                authResponse.refreshToken == null || authResponse.userId == null) {
                throw Exception(authResponse.error ?: "登录失败")
            }
            
            // 保存认证信息
            saveAuthInfo(authResponse.userId, authResponse.accessToken, authResponse.refreshToken)
            
            AuthResult(
                userId = authResponse.userId,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )
        }
    }
    
    /**
     * 发送手机验证码
     * @param phone 手机号（中国大陆格式）
     * @return 开发阶段返回验证码
     */
    suspend fun sendPhoneCode(phone: String): Result<String> = withContext(Dispatchers.IO) {
        val response = SyncApiService.sendPhoneCode(phone)
        
        response.mapCatching { sendCodeResponse ->
            if (!sendCodeResponse.success) {
                throw Exception(sendCodeResponse.error ?: "发送验证码失败")
            }
            
            // 开发阶段返回验证码方便测试
            sendCodeResponse.devCode ?: sendCodeResponse.message ?: ""
        }
    }
    
    /**
     * 手机号登录
     * @param phone 手机号
     * @param code 验证码
     */
    suspend fun loginWithPhone(phone: String, code: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        val response = SyncApiService.phoneLogin(phone, code)
        
        response.mapCatching { authResponse ->
            if (!authResponse.success || authResponse.accessToken == null || 
                authResponse.refreshToken == null || authResponse.userId == null) {
                throw Exception(authResponse.error ?: "登录失败")
            }
            
            // 保存认证信息
            saveAuthInfo(authResponse.userId, authResponse.accessToken, authResponse.refreshToken)
            
            AuthResult(
                userId = authResponse.userId,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )
        }
    }
    
    /**
     * 微信登录
     * 
     * 需要集成微信 SDK 获取授权码
     * 
     * @param code 微信授权码
     */
    suspend fun loginWithWechat(code: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        val response = SyncApiService.wechatLogin(code)
        
        response.mapCatching { authResponse ->
            if (!authResponse.success || authResponse.accessToken == null || 
                authResponse.refreshToken == null || authResponse.userId == null) {
                throw Exception(authResponse.error ?: "微信登录失败")
            }
            
            // 保存认证信息
            saveAuthInfo(authResponse.userId, authResponse.accessToken, authResponse.refreshToken)
            
            AuthResult(
                userId = authResponse.userId,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken
            )
        }
    }
    
    /**
     * 刷新 Access Token
     * @return 新的 Access Token
     */
    suspend fun refreshAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        val currentRefreshToken = refreshToken
            ?: return@withContext Result.failure(Exception("未登录"))
        
        val response = SyncApiService.refreshToken(currentRefreshToken)
        
        response.mapCatching { authResponse ->
            if (!authResponse.success || authResponse.accessToken == null) {
                // Refresh token 可能已过期，需要重新登录
                logout()
                throw Exception(authResponse.error ?: "Token 刷新失败，请重新登录")
            }
            
            // 更新内存中的 access token
            accessToken = authResponse.accessToken
            
            authResponse.accessToken
        }
    }
    
    /**
     * 确保 Access Token 有效（如果即将过期则自动刷新）
     */
    suspend fun ensureValidToken(): Result<String> {
        // 如果 access token 为空，尝试刷新
        if (accessToken == null) {
            return refreshAccessToken()
        }
        
        // TODO: 解析 JWT 检查是否即将过期
        // 实际生产环境中应该检查 token 过期时间，提前刷新
        // 目前简化处理：直接返回当前 token
        
        return Result.success(accessToken!!)
    }
    
    /**
     * 登出
     * 清除所有认证信息
     */
    fun logout() {
        accessToken = null
        refreshToken = null
        userId = null
        
        // 清除加密存储
        prefs.edit()
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 保存认证信息
     */
    private fun saveAuthInfo(userId: String, accessToken: String, refreshToken: String) {
        this.userId = userId
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        
        // 只持久化 refresh token（安全）
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }
    
    /**
     * 获取带认证头的请求
     */
    suspend fun getAuthHeader(): Result<String> {
        return ensureValidToken().map { "Bearer $it" }
    }
}
