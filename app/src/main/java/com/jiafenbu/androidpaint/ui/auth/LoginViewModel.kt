package com.jiafenbu.androidpaint.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jiafenbu.androidpaint.sync.AuthManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 登录 ViewModel
 * 
 * 管理登录界面的状态和业务逻辑
 */
class LoginViewModel : ViewModel() {
    
    // 加载状态
    var isLoading by mutableStateOf(false)
        private set
    
    // 登录成功
    var loginSuccess by mutableStateOf(false)
        private set
    
    // 错误消息
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // 开发阶段返回的验证码
    var devCode by mutableStateOf<String?>(null)
        private set
    
    // 倒计时
    private var countdownJob: Job? = null
    
    // ==================== 邮箱登录/注册 ====================
    
    /**
     * 邮箱注册
     */
    fun registerWithEmail(authManager: AuthManager, email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "邮箱和密码不能为空"
            return
        }
        
        if (!isValidEmail(email)) {
            errorMessage = "邮箱格式不正确"
            return
        }
        
        if (!isValidPassword(password)) {
            errorMessage = "密码至少8位，必须包含数字和字母"
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = authManager.registerWithEmail(email, password)
            
            isLoading = false
            
            result.fold(
                onSuccess = {
                    loginSuccess = true
                },
                onFailure = { e ->
                    errorMessage = e.message ?: "注册失败"
                }
            )
        }
    }
    
    /**
     * 邮箱登录
     */
    fun loginWithEmail(authManager: AuthManager, email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "邮箱和密码不能为空"
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = authManager.loginWithEmail(email, password)
            
            isLoading = false
            
            result.fold(
                onSuccess = {
                    loginSuccess = true
                },
                onFailure = { e ->
                    errorMessage = e.message ?: "登录失败"
                }
            )
        }
    }
    
    // ==================== 手机登录 ====================
    
    /**
     * 发送手机验证码
     */
    fun sendPhoneCode(authManager: AuthManager, phone: String, onCodeSent: (remaining: Int) -> Unit) {
        if (!isValidPhone(phone)) {
            errorMessage = "手机号格式不正确"
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            devCode = null
            
            val result = authManager.sendPhoneCode(phone)
            
            isLoading = false
            
            result.fold(
                onSuccess = { code ->
                    // 保存开发阶段验证码
                    devCode = code
                    onCodeSent(0)
                    
                    // 开始倒计时
                    startCountdown(60) { }
                },
                onFailure = { e ->
                    errorMessage = e.message ?: "发送验证码失败"
                }
            )
        }
    }
    
    /**
     * 手机号登录
     */
    fun loginWithPhone(authManager: AuthManager, phone: String, code: String) {
        if (!isValidPhone(phone)) {
            errorMessage = "手机号格式不正确"
            return
        }
        
        if (code.length != 6) {
            errorMessage = "验证码应为6位"
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = authManager.loginWithPhone(phone, code)
            
            isLoading = false
            
            result.fold(
                onSuccess = {
                    loginSuccess = true
                },
                onFailure = { e ->
                    errorMessage = e.message ?: "登录失败"
                }
            )
        }
    }
    
    // ==================== 微信登录 ====================
    
    /**
     * 微信登录
     * 
     * 需要微信 SDK 获取授权码后调用
     */
    fun loginWithWechat(authManager: AuthManager, code: String) {
        if (code.isBlank()) {
            errorMessage = "授权码不能为空"
            return
        }
        
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            val result = authManager.loginWithWechat(code)
            
            isLoading = false
            
            result.fold(
                onSuccess = {
                    loginSuccess = true
                },
                onFailure = { e ->
                    errorMessage = e.message ?: "微信登录失败"
                }
            )
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        errorMessage = null
    }
    
    /**
     * 设置错误消息
     */
    fun setError(message: String) {
        errorMessage = message
    }
    
    /**
     * 开始倒计时
     */
    private fun startCountdown(seconds: Int, onFinish: () -> Unit) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
            }
            onFinish()
        }
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证邮箱格式
     */
    private fun isValidEmail(email: String): Boolean {
        val regex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        return regex.matches(email)
    }
    
    /**
     * 验证密码强度
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 && 
               password.any { it.isLetter() } && 
               password.any { it.isDigit() }
    }
    
    /**
     * 验证手机号（中国大陆）
     */
    private fun isValidPhone(phone: String): Boolean {
        val regex = Regex("^1[3-9]\\d{9}$")
        return regex.matches(phone)
    }
    
    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
