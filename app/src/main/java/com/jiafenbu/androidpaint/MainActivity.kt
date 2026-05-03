package com.jiafenbu.androidpaint

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiafenbu.androidpaint.sync.SyncManager
import com.jiafenbu.androidpaint.ui.MainScreen
import com.jiafenbu.androidpaint.ui.auth.LoginScreen
import com.jiafenbu.androidpaint.ui.canvas.CanvasViewModel
import com.jiafenbu.androidpaint.ui.gallery.GalleryScreen
import com.jiafenbu.androidpaint.ui.gallery.GalleryViewModel

/**
 * 主 Activity
 * 应用入口点，负责导航和生命周期管理
 */
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val PREFS_NAME = "androidpaint_prefs"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用沉浸式边缘到边缘显示
        enableEdgeToEdge()
        
        setContent {
            val galleryViewModel: GalleryViewModel = viewModel()
            val canvasViewModel: CanvasViewModel = viewModel()
            
            // 注入项目管理器和同步管理器
            LaunchedEffect(Unit) {
                canvasViewModel.setProjectManager(galleryViewModel.getProjectManager())
                canvasViewModel.setSyncManager(galleryViewModel.getSyncManager())
            }
            
            // 导航状态
            var showGallery by remember { mutableStateOf(!isFirstLaunch()) }
            var currentProjectId by remember { mutableStateOf<String?>(null) }
            var showLogin by remember { mutableStateOf(false) }
            
            // 监听登录状态变化
            LaunchedEffect(galleryViewModel.isLoggedIn) {
                if (!galleryViewModel.isLoggedIn) {
                    // 登出时返回画廊
                }
            }
            
            // 生命周期观察者 - 自动保存
            val lifecycleOwner = LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            // 自动保存
                            if (currentProjectId != null && !showGallery) {
                                canvasViewModel.quickSave()
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // 显示登录界面
                if (showLogin) {
                    LoginScreen(
                        authManager = galleryViewModel.getAuthManager(),
                        onLoginSuccess = {
                            galleryViewModel.updateLoginState()
                            showLogin = false
                        },
                        onBack = {
                            showLogin = false
                        }
                    )
                } else if (showGallery) {
                    // 画廊界面
                    GalleryScreen(
                        viewModel = galleryViewModel,
                        onProjectSelected = { projectId ->
                            // 加载项目
                            canvasViewModel.loadProject(projectId)
                            currentProjectId = projectId
                            showGallery = false
                        },
                        onNewProject = { projectId ->
                            // 新项目已创建，直接进入画布
                            currentProjectId = projectId
                            showGallery = false
                        },
                        onLoginClick = {
                            showLogin = true
                        },
                        onLogout = {
                            galleryViewModel.logout()
                        }
                    )
                } else {
                    // 画布界面
                    MainScreen(
                        viewModel = canvasViewModel,
                        projectId = currentProjectId,
                        onBackToGallery = {
                            // 返回前自动保存
                            canvasViewModel.saveCurrentProject()
                            canvasViewModel.clearProjectState()
                            // 刷新画廊列表
                            galleryViewModel.loadProjectList()
                            showGallery = true
                        }
                    )
                }
            }
        }
    }
    
    /**
     * 检查是否首次启动
     * 首次启动直接进入画布，之后进入画廊
     */
    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        if (isFirst) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        }
        return isFirst
    }
}
