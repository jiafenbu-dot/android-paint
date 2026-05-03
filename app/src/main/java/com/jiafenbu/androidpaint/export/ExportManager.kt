package com.jiafenbu.androidpaint.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 导出管理器
 * 负责将画布导出为 PNG 或 JPEG 格式
 */
class ExportManager(private val context: Context) {
    
    companion object {
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
        private const val PNG_MIME_TYPE = "image/png"
        private const val JPEG_MIME_TYPE = "image/jpeg"
    }
    
    /**
     * 导出为 PNG 格式
     * @param bitmap 要导出的位图
     * @param fileName 文件名（不含扩展名）
     * @return 导出是否成功
     */
    suspend fun exportAsPng(bitmap: Bitmap, fileName: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val name = fileName ?: generateFileName("png")
                val result = saveBitmapToGallery(bitmap, name, PNG_MIME_TYPE, "image/png")
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 导出为 JPEG 格式
     * @param bitmap 要导出的位图
     * @param quality 质量（0-100）
     * @param fileName 文件名（不含扩展名）
     * @return 导出是否成功
     */
    suspend fun exportAsJpeg(
        bitmap: Bitmap,
        quality: Int = 95,
        fileName: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val name = fileName ?: generateFileName("jpg")
                val result = saveBitmapToGallery(bitmap, name, JPEG_MIME_TYPE, "image/jpeg", quality)
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 使用 MediaStore API 保存图片到相册
     * 兼容 Android 10+ (API 29)
     */
    private fun saveBitmapToGallery(
        bitmap: Bitmap,
        fileName: String,
        mimeType: String,
        extraMimeType: String,
        quality: Int = 100
    ): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AndroidPaint")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create MediaStore entry")
        
        return try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                val format = if (mimeType == PNG_MIME_TYPE) {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                bitmap.compress(format, quality, outputStream)
            } ?: throw Exception("Failed to open output stream")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            
            uri.toString()
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
    
    /**
     * 生成带时间戳的文件名
     */
    private fun generateFileName(extension: String): String {
        val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
        return "AndroidPaint_$timestamp.$extension"
    }
}
