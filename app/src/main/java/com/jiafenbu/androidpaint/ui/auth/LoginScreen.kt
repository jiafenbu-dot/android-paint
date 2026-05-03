package com.jiafenbu.androidpaint.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiafenbu.androidpaint.sync.AuthManager

/**
 * 登录界面
 * 
 * 支持三种登录方式 Tab 切换：
 * 1. 邮箱登录/注册
 * 2. 手机号登录
 * 3. 微信登录（后续集成）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: LoginViewModel = viewModel()
    
    // 监听登录状态
    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) {
            onLoginSuccess()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Email, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            // Logo 和标题
            Text(
                text = "AndroidPaint",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "专业插画软件",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Tab 选择器
            var selectedTab by remember { mutableIntStateOf(0) }
            val tabs = listOf("邮箱登录", "手机登录", "微信")
            
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                when (selectedTab) {
                    0 -> EmailLoginContent(
                        viewModel = viewModel,
                        authManager = authManager,
                        onLoginSuccess = onLoginSuccess
                    )
                    1 -> PhoneLoginContent(
                        viewModel = viewModel,
                        authManager = authManager,
                        onLoginSuccess = onLoginSuccess
                    )
                    2 -> WechatLoginContent(
                        viewModel = viewModel,
                        authManager = authManager,
                        onLoginSuccess = onLoginSuccess
                    )
                }
            }
            
            // 错误消息
            viewModel.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            // 加载指示器
            if (viewModel.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 邮箱登录/注册内容
 */
@Composable
private fun EmailLoginContent(
    viewModel: LoginViewModel,
    authManager: AuthManager,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    
    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                viewModel.clearError()
            },
            label = { Text("邮箱") },
            leadingIcon = { Icon(Icons.Default.Email, "邮箱") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                viewModel.clearError()
            },
            label = { Text("密码") },
            leadingIcon = { Icon(Icons.Default.Lock, "密码") },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "切换密码可见性"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (isRegisterMode) {
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认密码") },
                leadingIcon = { Icon(Icons.Default.Lock, "确认密码") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "密码至少8位，必须包含数字和字母",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (isRegisterMode) {
                    if (password != confirmPassword) {
                        viewModel.setError("两次密码输入不一致")
                        return@Button
                    }
                    viewModel.registerWithEmail(authManager, email, password)
                } else {
                    viewModel.loginWithEmail(authManager, email, password)
                }
            },
            enabled = !viewModel.isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRegisterMode) "注册" else "登录")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = { 
                isRegisterMode = !isRegisterMode
                viewModel.clearError()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (isRegisterMode) "已有账号？登录"
                else "没有账号？注册"
            )
        }
    }
}

/**
 * 手机登录内容
 */
@Composable
private fun PhoneLoginContent(
    viewModel: LoginViewModel,
    authManager: AuthManager,
    onLoginSuccess: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }
    
    Column {
        OutlinedTextField(
            value = phone,
            onValueChange = { 
                phone = it.filter { c -> c.isDigit() }.take(11)
                viewModel.clearError()
                codeSent = false
            },
            label = { Text("手机号") },
            leadingIcon = { Icon(Icons.Default.Phone, "手机号") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { 
                    code = it.filter { c -> c.isDigit() }.take(6)
                    viewModel.clearError()
                },
                label = { Text("验证码") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            
            Button(
                onClick = {
                    viewModel.sendPhoneCode(authManager, phone) { remaining ->
                        countdown = remaining
                        codeSent = true
                    }
                },
                enabled = !viewModel.isLoading && phone.length == 11 && countdown == 0,
                modifier = Modifier.height(56.dp)
            ) {
                Text(if (countdown > 0) "${countdown}s" else "获取验证码")
            }
        }
        
        // 开发阶段提示
        if (codeSent && viewModel.devCode != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "开发阶段验证码: ${viewModel.devCode}",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 14.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                viewModel.loginWithPhone(authManager, phone, code)
            },
            enabled = !viewModel.isLoading && phone.length == 11 && code.length == 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("登录")
        }
    }
}

/**
 * 微信登录内容
 */
@Composable
private fun WechatLoginContent(
    viewModel: LoginViewModel,
    authManager: AuthManager,
    onLoginSuccess: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "微信登录",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "点击下方按钮跳转微信授权",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 微信登录按钮（后续集成微信 SDK）
        Button(
            onClick = {
                // TODO: 调用微信 SDK 获取授权码
                // 暂时显示提示
                viewModel.setError("微信登录需要集成微信 SDK，请联系开发者")
            },
            enabled = !viewModel.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF07C160)  // 微信绿
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("微信授权登录")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "提示：微信登录需要在微信开放平台注册应用",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
