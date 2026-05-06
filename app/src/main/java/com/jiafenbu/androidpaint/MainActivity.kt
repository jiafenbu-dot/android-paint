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
import com.jiafenbu.androidpaint.ui.gallery.NewProjectDialog

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
            
            // 新建项目对话框状态
            var showNewProjectDialog by remember { mutableStateOf(false) }
            
            // 是否显示画布（首次或新建项目后）
            var showCanvas by remember { mutableStateOf(false) }
            
            // 生命周期观察者 - 自动保存
            val lifecycleOwner = LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                            if (showCanvas) {
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
                // 新建项目对话框（首次进入或新建项目时）
                if (showNewProjectDialog) {
                    NewProjectDialog(
                        onDismiss = {
                            // 用户取消时，如果有现有项目则显示画布，否则返回画廊
                            if (canvasViewModel.currentProjectId != null) {
                                showNewProjectDialog = false
                                showCanvas = true
                            } else {
                                showNewProjectDialog = false
                                showGallery = true
                            }
                        },
                        onConfirm = { name, width, height ->
                            // 用户确认创建新项目
                            canvasViewModel.createNewProject(name, width, height)
                            showNewProjectDialog = false
                            showCanvas = true
                        }
                    )
                }
                
                // 画廊界面
                if (showGallery && !showNewProjectDialog) {
                    GalleryScreen(
                        viewModel = galleryViewModel,
                        onProjectSelected = { projectId ->
                            canvasViewModel.loadProject(projectId)
                            showGallery = false
                            showCanvas = true
                        },
                        onNewProject = { _ ->
                            // 点击新建时弹出画布尺寸选择对话框
                            showGallery = false
                            showNewProjectDialog = true
                        }
                    )
                }
                
                // 画布界面
                if (showCanvas && !showNewProjectDialog) {
                    MainScreen(
                        viewModel = canvasViewModel,
                        onBackToGallery = {
                            canvasViewModel.saveCurrentProject()
                            canvasViewModel.clearProjectState()
                            galleryViewModel.loadProjectList()
                            showCanvas = false
                            showGallery = true
                        }
                    )
                }
            }
            
            // 首次进入或无项目时，显示画布尺寸选择对话框
            LaunchedEffect(Unit) {
                // 延迟一下确保 UI 已经准备好
                kotlinx.coroutines.delay(100)
                if (canvasViewModel.currentProjectId == null && !showGallery && !showCanvas) {
                    showNewProjectDialog = true
                } else if (canvasViewModel.currentProjectId != null) {
                    showCanvas = true
                }
            }
        }
    }
}
