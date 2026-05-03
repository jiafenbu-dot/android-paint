package com.jiafenbu.androidpaint.ui.brush

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiafenbu.androidpaint.brush.BrushDescriptor
import com.jiafenbu.androidpaint.brush.BrushRenderer
import com.jiafenbu.androidpaint.brush.BrushType
import com.jiafenbu.androidpaint.model.StrokeData
import com.jiafenbu.androidpaint.model.StrokePoint

/**
 * 笔刷库面板
 * 从底部滑出的笔刷选择器，显示所有笔刷的示例曲线预览
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrushLibrary(
    isVisible: Boolean,
    selectedBrushType: BrushType,
    currentColor: Int,
    onBrushSelected: (BrushType) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xEE1A1A1A),
        contentColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            // 标题栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "笔刷库",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 笔刷列表（横向滚动）
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(BrushType.entries) { brushType ->
                    BrushItem(
                        brushType = brushType,
                        isSelected = brushType == selectedBrushType,
                        currentColor = currentColor,
                        onClick = { onBrushSelected(brushType) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 单个笔刷项组件
 * 包含 80×60 的示例曲线预览和笔刷名称
 */
@Composable
private fun BrushItem(
    brushType: BrushType,
    isSelected: Boolean,
    currentColor: Int,
    onClick: () -> Unit
) {
    // 生成预览位图（带缓存）
    val previewBitmap = remember(brushType, currentColor) {
        generatePreviewBitmap(brushType, currentColor)
    }

    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Transparent
    val backgroundColor = if (isSelected) Color(0xFF2A2A2A) else Color(0xFF2A2A2A)

    Surface(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // 笔刷预览图
            Box(
                modifier = Modifier
                    .size(80.dp, 50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                previewBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = brushType.displayName,
                        modifier = Modifier.size(80.dp, 50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 笔刷图标 + 名称
            Text(
                text = "${brushType.icon} ${brushType.displayName}",
                color = Color.White,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * 生成笔刷预览位图
 * 在 80×60 的白色背景上画一条 S 形贝塞尔曲线
 */
private fun generatePreviewBitmap(brushType: BrushType, color: Int): Bitmap? {
    return try {
        val width = 80
        val height = 50
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 白色背景
        canvas.drawColor(android.graphics.Color.WHITE)

        val previewDescriptor = BrushDescriptor.getDefault(brushType).copy(
            // 预览时缩小笔刷尺寸
            size = (BrushDescriptor.getDefault(brushType).size * 0.4f).coerceAtLeast(1f)
        )

        val previewStrokeData = StrokeData(
            points = listOf(
                StrokePoint(10f, 35f),
                StrokePoint(20f, 25f),
                StrokePoint(35f, 40f),
                StrokePoint(50f, 15f),
                StrokePoint(65f, 30f),
                StrokePoint(75f, 20f)
            ),
            brushDescriptor = previewDescriptor,
            color = color
        )

        // 填充笔不渲染路径预览
        if (brushType == BrushType.FILL) {
            val fillPaint = Paint().apply {
                this.color = color
                alpha = 255
                style = Paint.Style.FILL
            }
            // 画一个小圆表示填充笔
            canvas.drawCircle(40f, 25f, 10f, fillPaint)

            // 画虚线框表示填充区域
            val outlinePaint = Paint().apply {
                this.color = color
                alpha = 100
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            canvas.drawCircle(40f, 25f, 18f, outlinePaint)
        } else if (brushType == BrushType.BLUR) {
            // 模糊笔预览：画一个彩色块再模糊
            val colorBlock = Paint().apply {
                this.color = color
                alpha = 200
                style = Paint.Style.FILL
            }
            canvas.drawCircle(40f, 25f, 15f, colorBlock)
        } else if (brushType == BrushType.SMUDGE) {
            // 涂抹笔预览：画渐变色块
            val c1 = android.graphics.Color.RED
            val c2 = android.graphics.Color.BLUE
            val paint1 = Paint().apply {
                color = c1
                alpha = 200
            }
            val paint2 = Paint().apply {
                color = c2
                alpha = 200
            }
            canvas.drawCircle(25f, 25f, 12f, paint1)
            canvas.drawCircle(55f, 25f, 12f, paint2)
        } else {
            BrushRenderer.renderStroke(canvas, previewStrokeData)
        }

        bitmap
    } catch (e: Exception) {
        null
    }
}
