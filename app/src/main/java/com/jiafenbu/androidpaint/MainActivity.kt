package com.jiafenbu.androidpaint

import android.os.Bundle
import android.util.Log
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
import com.jiafenbu.androidpaint.ui.MainScreen
import com.jiafenbu.androidpaint.ui.canvas.CanvasViewModel
import com.jiafenbu.androidpaint.ui.gallery.GalleryScreen
import com.jiafenbu.androidpaint.ui.gallery.GalleryViewModel

/**
 * 主 Activity - 纯本地版
 * 无需注册、无需云端同步，自动保存到手机
 */
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            val galleryViewModel: GalleryViewModel = viewModel()
            val canvasViewModel: CanvasViewModel = viewModel()
            
            // 注入项目管理器
            LaunchedEffect(Unit) {
                canvasViewModel.setProjectManager(galleryViewModel.getProjectManager())
            }
            
            // 导航状态
            var showGallery by remember { mutableStateOf(false) }
            
            // 启动时自动创建新画布（如果没有当前项目）
            LaunchedEffect(Unit) {
                if (canvasViewModel.currentProjectId == null) {
                    canvasViewModel.createNewProject("未命名作品", 1080, 1920)
                }
            }
            
            // 生命周期观察者 - 自动保存
            val lifecycleOwner = LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            if (!showGallery) {
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
                if (showGallery) {
                    GalleryScreen(
                        viewModel = galleryViewModel,
                        onProjectSelected = { projectId ->
                            canvasViewModel.loadProject(projectId)
                            showGallery = false
                        },
                        onNewProject = { _ ->
                            // 直接用 createNewProject 在内存中创建画布
                            // 不走 loadProject（磁盘读取 ORA 可能失败）
                            canvasViewModel.createNewProject("未命名作品", 1080, 1920)
                            showGallery = false
                        }
                    )
                } else {
                    MainScreen(
                        viewModel = canvasViewModel,
                        onBackToGallery = {
                            canvasViewModel.saveCurrentProject()
                            canvasViewModel.clearProjectState()
                            galleryViewModel.loadProjectList()
                            showGallery = true
                        }
                    )
                }
            }
        }
    }
}
