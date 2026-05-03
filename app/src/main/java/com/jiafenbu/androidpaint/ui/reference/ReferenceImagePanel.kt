package com.jiafenbu.androidpaint.ui.reference

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.R
import com.jiafenbu.androidpaint.model.ReferenceImage
import kotlin.math.roundToInt

/**
 * 参考图面板
 * 浮动显示参考图片，支持透明度调整和位置调整
 * 
 * @param referenceImage 参考图数据
 * @param onUpdate 更新参考图数据
 * @param onRemove 移除参考图
 * @param onDismiss 关闭面板
 */
@Composable
fun ReferenceImagePanel(
    referenceImage: ReferenceImage?,
    onUpdate: (ReferenceImage) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (referenceImage == null) return
    
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // 加载图片
    remember(referenceImage.uri) {
        try {
            context.contentResolver.openInputStream(Uri.parse(referenceImage.uri))?.use { stream ->
                bitmap = BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    var showControls by remember { mutableStateOf(false) }
    var positionX by remember { mutableFloatStateOf(referenceImage.positionX) }
    var positionY by remember { mutableFloatStateOf(referenceImage.positionY) }
    var opacity by remember { mutableFloatStateOf(referenceImage.opacity) }
    var scale by remember { mutableFloatStateOf(referenceImage.scale) }
    var rotation by remember { mutableFloatStateOf(referenceImage.rotation) }
    var isLocked by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 参考图
        if (bitmap != null && referenceImage.isVisible) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(positionX.roundToInt(), positionY.roundToInt()) }
                    .alpha(opacity)
                    .then(
                        if (isLocked) Modifier else {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    positionX += dragAmount.x
                                    positionY += dragAmount.y
                                }
                            }
                        }
                    )
                    .then(
                        if (isLocked) Modifier else {
                            Modifier.pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    scale = (scale * zoom).coerceIn(0.1f, 3f)
                                    rotation += rotationChange
                                    positionX += pan.x
                                    positionY += pan.y
                                }
                            }
                        }
                    )
            ) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "参考图",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size((200 * scale).dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
        
        // 控制按钮
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xCC000000),
            onClick = { showControls = !showControls }
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isLocked = !isLocked },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isLocked) "解锁" else "锁定",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        // 控制面板
        if (showControls) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEE1A1A1A),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "参考图设置",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 透明度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "透明度",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Slider(
                            value = opacity,
                            onValueChange = { opacity = it },
                            valueRange = 0.1f..1f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2196F3),
                                activeTrackColor = Color(0xFF2196F3)
                            )
                        )
                        
                        Text(
                            text = "${(opacity * 100).roundToInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 缩放
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "缩放",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Slider(
                            value = scale,
                            onValueChange = { scale = it },
                            valueRange = 0.1f..3f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2196F3),
                                activeTrackColor = Color(0xFF2196F3)
                            )
                        )
                        
                        Text(
                            text = "${(scale * 100).roundToInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 操作按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onUpdate(referenceImage.copy(isVisible = !referenceImage.isVisible))
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipToBack,
                                contentDescription = if (referenceImage.isVisible) "隐藏" else "显示",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 保存位置更新
    remember(positionX, positionY, opacity, scale, rotation) {
        onUpdate(referenceImage.copy(
            positionX = positionX,
            positionY = positionY,
            opacity = opacity,
            scale = scale,
            rotation = rotation
        ))
    }
}

/**
 * 参考图导入器
 * 提供选择图片的界面
 */
@Composable
fun ReferenceImagePicker(
    onImageSelected: (Uri) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 这里需要集成图片选择器
    // 可以使用 ActivityResultContracts.GetContent()
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xEE1A1A1A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "选择参考图",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 暂时显示提示信息
            Text(
                text = "请从相册选择图片作为参考",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}
