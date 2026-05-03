package com.jiafenbu.androidpaint.model

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import org.json.JSONObject

/**
 * 水印配置模型
 * 存储水印的所有属性，支持文字水印和图片水印
 *
 * @param id 唯一标识
 * @param type 水印类型
 * @param text 文字水印内容
 * @param imagePath 图片水印路径
 * @param imageBitmap 图片水印位图
 * @param opacity 透明度 (0f - 1f)
 * @param position 位置
 * @param rotation 旋转角度
 * @param tileMode 平铺模式
 * @param fontSize 文字水印字号
 * @param fontColor 文字水印颜色
 * @param scale 图片水印缩放比例
 * @param isEnabled 是否启用
 */
data class WatermarkConfig(
    val id: Long = System.currentTimeMillis(),
    val type: WatermarkType = WatermarkType.TEXT,
    val text: String = "Watermark",
    val imagePath: String? = null,
    val imageBitmap: Bitmap? = null,
    val opacity: Float = 0.3f,
    val position: Offset = Offset(50f, 50f),
    val rotation: Float = -30f,
    val tileMode: WatermarkTileMode = WatermarkTileMode.DIAGONAL,
    val fontSize: Float = 24f,
    val fontColor: Int = 0x80000000.toInt(),
    val scale: Float = 1f,
    val isEnabled: Boolean = true
) {
    companion object {
        private var nextId = 0L

        /**
         * 创建新的水印配置
         */
        fun create(type: WatermarkType = WatermarkType.TEXT): WatermarkConfig {
            return WatermarkConfig(
                id = nextId++,
                type = type
            )
        }

        /**
         * 重置 ID 计数器
         */
        fun resetIdCounter() {
            nextId = 0L
        }

        /**
         * 从 JSON 解析
         */
        fun fromJson(json: String): WatermarkConfig {
            return try {
                val obj = JSONObject(json)
                WatermarkConfig(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    type = WatermarkType.valueOf(obj.optString("type", "TEXT")),
                    text = obj.optString("text", "Watermark"),
                    imagePath = obj.optString("imagePath", "").takeIf { it.isNotEmpty() },
                    opacity = obj.optDouble("opacity", 0.3).toFloat(),
                    position = Offset(
                        obj.optDouble("positionX", 50.0).toFloat(),
                        obj.optDouble("positionY", 50.0).toFloat()
                    ),
                    rotation = obj.optDouble("rotation", -30.0).toFloat(),
                    tileMode = WatermarkTileMode.valueOf(obj.optString("tileMode", "DIAGONAL")),
                    fontSize = obj.optDouble("fontSize", 24.0).toFloat(),
                    fontColor = obj.optInt("fontColor", 0x80000000.toInt()),
                    scale = obj.optDouble("scale", 1.0).toFloat(),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                WatermarkConfig()
            }
        }
    }

    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("type", type.name)
            put("text", text)
            imagePath?.let { put("imagePath", it) }
            put("opacity", opacity.toDouble())
            put("positionX", position.x.toDouble())
            put("positionY", position.y.toDouble())
            put("rotation", rotation.toDouble())
            put("tileMode", tileMode.name)
            put("fontSize", fontSize.toDouble())
            put("fontColor", fontColor)
            put("scale", scale.toDouble())
            put("isEnabled", isEnabled)
        }.toString()
    }

    /**
     * 复制并修改
     */
    fun copy(
        type: WatermarkType = this.type,
        text: String = this.text,
        imagePath: String? = this.imagePath,
        imageBitmap: Bitmap? = this.imageBitmap,
        opacity: Float = this.opacity,
        position: Offset = this.position,
        rotation: Float = this.rotation,
        tileMode: WatermarkTileMode = this.tileMode,
        fontSize: Float = this.fontSize,
        fontColor: Int = this.fontColor,
        scale: Float = this.scale,
        isEnabled: Boolean = this.isEnabled
    ): WatermarkConfig {
        return WatermarkConfig(
            id = this.id,
            type = type,
            text = text,
            imagePath = imagePath,
            imageBitmap = imageBitmap,
            opacity = opacity,
            position = position,
            rotation = rotation,
            tileMode = tileMode,
            fontSize = fontSize,
            fontColor = fontColor,
            scale = scale,
            isEnabled = isEnabled
        )
    }
}

/**
 * 水印类型
 */
enum class WatermarkType {
    TEXT,   // 文字水印
    IMAGE   // 图片水印
}

/**
 * 水印平铺模式
 */
enum class WatermarkTileMode {
    SINGLE,     // 单个放置
    DIAGONAL,   // 对角线平铺（斜着重复铺满）
    GRID        // 网格平铺（水平垂直重复）
}
