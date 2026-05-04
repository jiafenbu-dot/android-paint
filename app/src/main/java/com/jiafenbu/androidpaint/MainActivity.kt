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
import com.jiafenbu.androidpaint.ui.MainScreen
import com.jiafenbu.androidpaint.ui.canvas.CanvasViewModel
import com.jiafenbu.androidpaint.ui.gallery.GalleryScreen
import com.jiafenbu.androidpaint.ui.gallery.GalleryViewModel

/**
 * 主 Activity
 * 纯本地版本：无需注册、无需云端同步
 * 自动保存到手机本地存储
 */
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val PREFS_NAME = "androidpaint_prefs"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
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
            var showGallery by remember { mutableStateOf(!isFirstLaunch()) }
            var currentProjectId by remember { mutableStateOf<String?>(null) }
            
            // 生命周期观察者 - 自动保存
            val lifecycleOwner = LocalLifecycleOwner.current
            androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
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
                if (showGallery) {
                    GalleryScreen(
                        viewModel = galleryViewModel,
                        onProjectSelected = { projectId ->
                            canvasViewModel.loadProject(projectId)
                            currentProjectId = projectId
                            showGallery = false
                        },
                        onNewProject = { projectId ->
                            currentProjectId = projectId
                            showGallery = false
                        }
                    )
                } else {
                    MainScreen(
                        viewModel = canvasViewModel,
                        projectId = currentProjectId,
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
    
    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirst = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        if (isFirst) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        }
        return isFirst
    }
}
