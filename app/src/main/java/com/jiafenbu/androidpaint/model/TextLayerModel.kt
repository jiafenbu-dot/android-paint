package com.jiafenbu.androidpaint.model

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import org.json.JSONObject

/**
 * 文字图层数据模型
 * 存储文字的所有属性信息，支持序列化保存
 *
 * @param id 唯一标识
 * @param text 文字内容
 * @param fontFamily 字体名称
 * @param fontSize 字号（sp）
 * @param isBold 是否粗体
 * @param isItalic 是否斜体
 * @param letterSpacing 字间距
 * @param lineSpacing 行间距
 * @param textColor 文字颜色
 * @param alignment 文字对齐方式
 * @param position 文字位置
 * @param rotation 旋转角度
 * @param strokeEnabled 是否启用描边
 * @param strokeWidth 描边宽度
 * @param strokeColor 描边颜色
 * @param shadowEnabled 是否启用阴影
 * @param shadowOffsetX 阴影X偏移
 * @param shadowOffsetY 阴影Y偏移
 * @param shadowBlurRadius 阴影模糊半径
 * @param shadowColor 阴影颜色
 * @param customFontPath 自定义字体文件路径（可选）
 */
data class TextLayerModel(
    val id: Long = System.currentTimeMillis(),
    val text: String = "",
    val fontFamily: String = "默认",
    val fontSize: Float = 32f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1.2f,
    val textColor: Int = 0xFF000000.toInt(),
    val alignment: TextAlignment = TextAlignment.LEFT,
    val position: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val strokeEnabled: Boolean = false,
    val strokeWidth: Float = 2f,
    val strokeColor: Int = 0xFF000000.toInt(),
    val shadowEnabled: Boolean = false,
    val shadowOffsetX: Float = 2f,
    val shadowOffsetY: Float = 2f,
    val shadowBlurRadius: Float = 4f,
    val shadowColor: Int = 0x80000000.toInt(),
    val customFontPath: String? = null
) {
    companion object {
        private var nextId = 0L

        /**
         * 创建新的文字图层
         */
        fun create(text: String = ""): TextLayerModel {
            return TextLayerModel(
                id = nextId++,
                text = text
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
        fun fromJson(json: String): TextLayerModel {
            return try {
                val obj = JSONObject(json)
                TextLayerModel(
                    id = obj.optLong("id", System.currentTimeMillis()),
                    text = obj.optString("text", ""),
                    fontFamily = obj.optString("fontFamily", "默认"),
                    fontSize = obj.optDouble("fontSize", 32.0).toFloat(),
                    isBold = obj.optBoolean("isBold", false),
                    isItalic = obj.optBoolean("isItalic", false),
                    letterSpacing = obj.optDouble("letterSpacing", 0.0).toFloat(),
                    lineSpacing = obj.optDouble("lineSpacing", 1.2).toFloat(),
                    textColor = obj.optInt("textColor", 0xFF000000.toInt()),
                    alignment = TextAlignment.fromString(obj.optString("alignment", "LEFT")),
                    position = Offset(
                        obj.optDouble("positionX", 0.0).toFloat(),
                        obj.optDouble("positionY", 0.0).toFloat()
                    ),
                    rotation = obj.optDouble("rotation", 0.0).toFloat(),
                    strokeEnabled = obj.optBoolean("strokeEnabled", false),
                    strokeWidth = obj.optDouble("strokeWidth", 2.0).toFloat(),
                    strokeColor = obj.optInt("strokeColor", 0xFF000000.toInt()),
                    shadowEnabled = obj.optBoolean("shadowEnabled", false),
                    shadowOffsetX = obj.optDouble("shadowOffsetX", 2.0).toFloat(),
                    shadowOffsetY = obj.optDouble("shadowOffsetY", 2.0).toFloat(),
                    shadowBlurRadius = obj.optDouble("shadowBlurRadius", 4.0).toFloat(),
                    shadowColor = obj.optInt("shadowColor", 0x80000000.toInt()),
                    customFontPath = obj.optString("customFontPath", "").takeIf { it.isNotEmpty() }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                TextLayerModel()
            }
        }
    }

    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String {
        return JSONObject().apply {
            put("id", id)
            put("text", text)
            put("fontFamily", fontFamily)
            put("fontSize", fontSize.toDouble())
            put("isBold", isBold)
            put("isItalic", isItalic)
            put("letterSpacing", letterSpacing.toDouble())
            put("lineSpacing", lineSpacing.toDouble())
            put("textColor", textColor)
            put("alignment", alignment.name)
            put("positionX", position.x.toDouble())
            put("positionY", position.y.toDouble())
            put("rotation", rotation.toDouble())
            put("strokeEnabled", strokeEnabled)
            put("strokeWidth", strokeWidth.toDouble())
            put("strokeColor", strokeColor)
            put("shadowEnabled", shadowEnabled)
            put("shadowOffsetX", shadowOffsetX.toDouble())
            put("shadowOffsetY", shadowOffsetY.toDouble())
            put("shadowBlurRadius", shadowBlurRadius.toDouble())
            put("shadowColor", shadowColor)
            customFontPath?.let { put("customFontPath", it) }
        }.toString()
    }

    /**
     * 复制并修改
     */
    fun copy(
        text: String = this.text,
        fontFamily: String = this.fontFamily,
        fontSize: Float = this.fontSize,
        isBold: Boolean = this.isBold,
        isItalic: Boolean = this.isItalic,
        letterSpacing: Float = this.letterSpacing,
        lineSpacing: Float = this.lineSpacing,
        textColor: Int = this.textColor,
        alignment: TextAlignment = this.alignment,
        position: Offset = this.position,
        rotation: Float = this.rotation,
        strokeEnabled: Boolean = this.strokeEnabled,
        strokeWidth: Float = this.strokeWidth,
        strokeColor: Int = this.strokeColor,
        shadowEnabled: Boolean = this.shadowEnabled,
        shadowOffsetX: Float = this.shadowOffsetX,
        shadowOffsetY: Float = this.shadowOffsetY,
        shadowBlurRadius: Float = this.shadowBlurRadius,
        shadowColor: Int = this.shadowColor,
        customFontPath: String? = this.customFontPath
    ): TextLayerModel {
        return TextLayerModel(
            id = this.id,
            text = text,
            fontFamily = fontFamily,
            fontSize = fontSize,
            isBold = isBold,
            isItalic = isItalic,
            letterSpacing = letterSpacing,
            lineSpacing = lineSpacing,
            textColor = textColor,
            alignment = alignment,
            position = position,
            rotation = rotation,
            strokeEnabled = strokeEnabled,
            strokeWidth = strokeWidth,
            strokeColor = strokeColor,
            shadowEnabled = shadowEnabled,
            shadowOffsetX = shadowOffsetX,
            shadowOffsetY = shadowOffsetY,
            shadowBlurRadius = shadowBlurRadius,
            shadowColor = shadowColor,
            customFontPath = customFontPath
        )
    }

    /**
     * 创建 Paint 对象用于渲染
     */
    fun createPaint(): Paint {
        return Paint().apply {
            color = this@TextLayerModel.textColor
            textSize = this@TextLayerModel.fontSize * 3 // 将 sp 转换为 px（假设 3x 密度）
            isAntiAlias = true

            // 设置字体样式
            val style = when {
                isBold && isItalic -> Typeface.BOLD_ITALIC
                isBold -> Typeface.BOLD
                isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            // 设置字体
            typeface = createTypeface(fontFamily, style, customFontPath)

            // 设置阴影
            if (shadowEnabled) {
                setShadowLayer(shadowBlurRadius, shadowOffsetX, shadowOffsetY, shadowColor)
            }
        }
    }

    /**
     * 根据字体名称创建 Typeface
     */
    private fun createTypeface(family: String, style: Int, customPath: String?): Typeface {
        return when {
            // 自定义字体优先
            !customPath.isNullOrEmpty() -> {
                try {
                    Typeface.createFromFile(customPath)
                } catch (e: Exception) {
                    Typeface.create(family, style)
                }
            }
            // 系统字体
            else -> Typeface.create(family, style)
        }
    }

    /**
     * 计算文字边界
     */
    fun measureTextBounds(): android.graphics.RectF {
        val paint = createPaint()
        val bounds = android.graphics.RectF()
        val lines = text.split("\n")

        var maxWidth = 0f
        var totalHeight = 0f

        lines.forEach { line ->
            val lineWidth = paint.measureText(line)
            maxWidth = maxOf(maxWidth, lineWidth)
            totalHeight += paint.fontSpacing
        }

        bounds.set(0f, 0f, maxWidth, totalHeight)
        return bounds
    }
}

/**
 * 文字对齐方式
 */
enum class TextAlignment {
    LEFT,
    CENTER,
    RIGHT;

    companion object {
        fun fromString(value: String): TextAlignment {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                LEFT
            }
        }
    }

    fun toAndroidAlign(): android.graphics.Paint.Align {
        return when (this) {
            LEFT -> android.graphics.Paint.Align.LEFT
            CENTER -> android.graphics.Paint.Align.CENTER
            RIGHT -> android.graphics.Paint.Align.RIGHT
        }
    }
}

/**
 * 图层类型枚举
 */
enum class LayerType {
    NORMAL,  // 普通图层
    TEXT     // 文字图层
}
