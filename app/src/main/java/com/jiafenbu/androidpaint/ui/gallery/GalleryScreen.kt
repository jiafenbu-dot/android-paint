package com.jiafenbu.androidpaint.ui.gallery

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiafenbu.androidpaint.project.ProjectInfo
import kotlinx.coroutines.launch

/**
 * 画廊界面
 * 显示所有项目，支持项目选择、新建、删除等操作
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = viewModel(),
    onProjectSelected: (String) -> Unit,
    onNewProject: (String) -> Unit,
    onLoginClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val projectList by viewModel.projectList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEmpty by viewModel.isEmpty.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedProjectIds by viewModel.selectedProjectIds.collectAsState()
    val isLoggedIn = viewModel.isLoggedIn
    val currentConflict by viewModel.currentConflict.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var showDetailDialog by remember { mutableStateOf<ProjectInfo?>(null) }
    var contextMenuProject by remember { mutableStateOf<ProjectInfo?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // 显示错误消息
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) {
                        Text("${viewModel.selectionCount} 已选中")
                    } else {
                        Text("我的作品")
                    }
                },
                navigationIcon = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { viewModel.exitMultiSelectMode() }) {
                            Icon(Icons.Default.Close, "取消")
                        }
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        // 多选模式操作
                        IconButton(onClick = {
                            if (viewModel.isAllSelected) {
                                viewModel.deselectAll()
                            } else {
                                viewModel.selectAll()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, "全选")
                        }
                    } else {
                        // 普通模式
                        IconButton(onClick = { viewModel.enterMultiSelectMode() }) {
                            Icon(Icons.Default.Check, "多选")
                        }
                        // 登录/登出按钮
                        if (isLoggedIn) {
                            IconButton(onClick = onLogout) {
                                Icon(Icons.Default.Info, "登出")
                            }
                        } else {
                            IconButton(onClick = onLoginClick) {
                                Icon(Icons.Default.Info, "登录")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!isMultiSelectMode) {
                FloatingActionButton(
                    onClick = { showNewProjectDialog = true }
                ) {
                    Icon(Icons.Default.Add, "新建项目")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                // 同步状态栏
                if (isLoggedIn) {
                    SyncStatusBar(
                        syncStatus = syncStatus,
                        syncProgress = syncProgress,
                        isLoggedIn = isLoggedIn
                    )
                }
                // 多选模式底部操作栏
                if (isMultiSelectMode && viewModel.hasSelection) {
                    MultiSelectBottomBar(
                        selectedCount = viewModel.selectionCount,
                        onDelete = { viewModel.deleteSelectedProjects() }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // 加载中
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                isEmpty -> {
                    // 空状态
                    EmptyGalleryState(
                        onNewProject = { showNewProjectDialog = true }
                    )
                }
                else -> {
                    // 项目列表
                    ProjectList(
                        projects = projectList,
                        isMultiSelectMode = isMultiSelectMode,
                        selectedProjectIds = selectedProjectIds,
                        getThumbnail = { viewModel.getThumbnail(it) },
                        getSyncStatusIcon = { if (viewModel.isLoggedIn) viewModel.getProjectSyncStatusIcon(it) else null },
                        onProjectClick = { project ->
                            if (isMultiSelectMode) {
                                viewModel.toggleProjectSelection(project.id)
                            } else {
                                onProjectSelected(project.id)
                            }
                        },
                        onProjectLongClick = { project ->
                            contextMenuProject = project
                        },
                        onDeleteProject = { project ->
                            viewModel.deleteProject(project.id)
                        }
                    )
                }
            }
        }
    }
    
    // 新建项目对话框
    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onConfirm = { name, width, height ->
                viewModel.createProject(name, width, height) { projectId ->
                    showNewProjectDialog = false
                    onNewProject(projectId)
                }
            }
        )
    }
    
    // 重命名对话框
    showRenameDialog?.let { projectId ->
        val project = viewModel.getProjectInfo(projectId)
        if (project != null) {
            RenameProjectDialog(
                currentName = project.name,
                onDismiss = { showRenameDialog = null },
                onConfirm = { newName ->
                    viewModel.renameProject(projectId, newName)
                    showRenameDialog = null
                }
            )
        }
    }
    
    // 详情对话框
    showDetailDialog?.let { project ->
        ProjectDetailDialog(
            projectInfo = project,
            onDismiss = { showDetailDialog = null }
        )
    }
    
    // 长按上下文菜单
    contextMenuProject?.let { project ->
        ProjectContextMenu(
            project = project,
            onDismiss = { contextMenuProject = null },
            onRename = {
                contextMenuProject = null
                showRenameDialog = project.id
            },
            onDuplicate = {
                contextMenuProject = null
                viewModel.duplicateProject(project.id)
            },
            onDelete = {
                contextMenuProject = null
                viewModel.deleteProject(project.id)
            },
            onDetails = {
                contextMenuProject = null
                showDetailDialog = project
            }
        )
    }
    
    // 同步冲突对话框
    currentConflict?.let { conflict ->
        SyncConflictDialog(
            conflict = conflict,
            onDismiss = { viewModel.clearConflict() },
            onKeepLocal = {
                viewModel.resolveConflict(conflict.projectId, "local")
            },
            onUseRemote = {
                viewModel.resolveConflict(conflict.projectId, "remote")
            }
        )
    }
}

/**
 * 项目列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectList(
    projects: List<ProjectInfo>,
    isMultiSelectMode: Boolean,
    selectedProjectIds: Set<String>,
    getThumbnail: (String) -> Bitmap?,
    getSyncStatusIcon: (String) -> String? = { null },
    onProjectClick: (ProjectInfo) -> Unit,
    onProjectLongClick: (ProjectInfo) -> Unit,
    onDeleteProject: (ProjectInfo) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = projects,
            key = { it.id }
        ) { project ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        onDeleteProject(project)
                        true
                    } else {
                        false
                    }
                }
            )
            
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = 0.8f)
                            else -> Color.Transparent
                        },
                        label = "swipe_color"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color, RoundedCornerShape(12.dp))
                            .padding(end = 24.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = Color.White
                            )
                        }
                    }
                },
                enableDismissFromStartToEnd = false
            ) {
                ProjectCard(
                    project = project,
                    thumbnail = getThumbnail(project.id),
                    isSelected = project.id in selectedProjectIds,
                    isMultiSelectMode = isMultiSelectMode,
                    syncStatusIcon = getSyncStatusIcon(project.id),
                    onClick = { onProjectClick(project) },
                    onLongClick = { onProjectLongClick(project) }
                )
            }
        }
    }
}

/**
 * 项目卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCard(
    project: ProjectInfo,
    thumbnail: Bitmap?,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    syncStatusIcon: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "card_bg"
    )
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选复选框
            if (isMultiSelectMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // 缩略图
            ThumbnailView(
                thumbnail = thumbnail,
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 项目信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 同步状态图标
                    syncStatusIcon?.let { icon ->
                        Text(
                            text = icon,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = project.formattedModifiedTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${project.layerCount}图层",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = project.formattedFileSize(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 缩略图视图
 */
@Composable
private fun ThumbnailView(
    thumbnail: Bitmap?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail.asImageBitmap(),
                contentDescription = "缩略图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 多选模式底部操作栏
 */
@Composable
private fun MultiSelectBottomBar(
    selectedCount: Int,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 删除按钮
            androidx.compose.material3.Button(
                onClick = onDelete,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("删除 ($selectedCount)")
            }
        }
    }
}

/**
 * 空状态画廊
 */
@Composable
private fun EmptyGalleryState(
    onNewProject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "还没有作品",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击 + 开始创作",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 快速创建选项
        Text(
            text = "快速开始",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CanvasPreset.values().take(2).forEach { preset ->
                QuickCreateCard(
                    preset = preset,
                    onClick = onNewProject
                )
            }
        }
    }
}

/**
 * 项目上下文菜单
 */
@Composable
private fun ProjectContextMenu(
    project: ProjectInfo,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(project.name)
        },
        text = {
            Column {
                ContextMenuItem(
                    icon = Icons.Default.Edit,
                    text = "重命名",
                    onClick = onRename
                )
                ContextMenuItem(
                    icon = Icons.Default.FileCopy,
                    text = "复制",
                    onClick = onDuplicate
                )
                ContextMenuItem(
                    icon = Icons.Default.Info,
                    text = "详情",
                    onClick = onDetails
                )
                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    text = "删除",
                    onClick = onDelete,
                    isDestructive = true
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 上下文菜单项
 */
@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDestructive) Color.Red else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = if (isDestructive) Color.Red else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 同步冲突对话框
 */
@Composable
fun SyncConflictDialog(
    conflict: com.jiafenbu.androidpaint.sync.ConflictInfo,
    onDismiss: () -> Unit,
    onKeepLocal: () -> Unit,
    onUseRemote: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text("⚠️", style = MaterialTheme.typography.headlineMedium)
        },
        title = {
            Text("同步冲突")
        },
        text = {
            Column {
                Text("检测到版本冲突，请选择保留哪个版本：")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "本地版本: v${conflict.localVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "远程版本: v${conflict.remoteVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (conflict.remoteTimestamp > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    Text(
                        text = "远程更新于: ${dateFormat.format(java.util.Date(conflict.remoteTimestamp * 1000))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepLocal) {
                Text("保留本地")
            }
        },
        dismissButton = {
            TextButton(onClick = onUseRemote) {
                Text("使用远程")
            }
        }
    )
}

/**
 * 同步状态栏
 */
@Composable
fun SyncStatusBar(
    syncStatus: com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus,
    syncProgress: Float,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLoggedIn) return
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 同步状态
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusIcon = when (syncStatus) {
                com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.IDLE -> "☁️"
                com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.UPLOADING -> "⬆️"
                com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.DOWNLOADING -> "⬇️"
                com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.CONFLICT -> "⚠️"
                com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.ERROR -> "❌"
                com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.OFFLINE -> "🚫"
            }
            Text(
                text = statusIcon,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (syncStatus) {
                    com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.IDLE -> "已同步"
                    com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.UPLOADING -> "上传中..."
                    com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.DOWNLOADING -> "下载中..."
                    com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.CONFLICT -> "存在冲突"
                    com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.ERROR -> "同步错误"
                    com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.OFFLINE -> "离线"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 进度条
        if (syncStatus == com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.UPLOADING ||
            syncStatus == com.jiafenbu.androidpaint.sync.SyncManager.SyncStatus.DOWNLOADING) {
            LinearProgressIndicator(
                progress = { syncProgress },
                modifier = Modifier.width(100.dp),
            )
        }
    }
}
