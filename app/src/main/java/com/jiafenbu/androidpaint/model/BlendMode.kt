package com.jiafenbu.androidpaint.model

import android.graphics.BlendMode as AndroidBlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/**
 * 混合模式枚举
 * 定义了16种图层混合模式，用于控制上下图层的叠加效果
 * 
 * 对于 Android 原生 PorterDuff.Mode 不完全覆盖所有混合模式的情况：
 * - API 29+ 使用 android.graphics.BlendMode
 * - API 26-28 回退到近似 PorterDuff.Mode
 * - HUE/SATURATION/COLOR/LUMINOSITY 使用 ColorMatrix 近似实现
 */
enum class BlendMode(
    val displayName: String,
    val description: String
) {
    /**
     * 正常模式
     * 上层完全覆盖下层（默认）
     */
    NORMAL("正常", "上层完全覆盖下层"),

    /**
     * 正片叠底模式
     * 将上下层的色彩值相乘，出值会更暗
     */
    MULTIPLY("正片叠底", "色彩值相乘，结果更暗"),

    /**
     * 滤色模式
     * 消除了黑色和白色，产生更亮的颜色
     */
    SCREEN("滤色", "消除黑色和白色，结果更亮"),

    /**
     * 叠加模式
     * 亮的地方更亮，暗的地方更暗
     * API 29+ 使用 android.graphics.BlendMode.OVERLAY
     * API 26-28 近似使用 PorterDuff.Mode.MULTIPLY + SRC_OVER
     */
    OVERLAY("叠加", "亮更亮暗更暗"),

    /**
     * 柔光模式
     * 根据上层颜色产生漫射效果
     * 使用自定义实现
     */
    SOFT_LIGHT("柔光", "产生漫射柔光效果"),

    /**
     * 强光模式
     * 根据上层颜色产生高光效果
     * 使用自定义实现
     */
    HARD_LIGHT("强光", "产生高光效果"),

    /**
     * 颜色减淡模式
     * 亮化底层以反映上层颜色
     * 使用自定义实现
     */
    COLOR_DODGE("颜色减淡", "亮化底层反映上层颜色"),

    /**
     * 颜色加深模式
     * 暗化底层以反映上层颜色
     * 使用自定义实现
     */
    COLOR_BURN("颜色加深", "暗化底层反映上层颜色"),

    /**
     * 变暗模式
     * 取上下层中较暗的像素值
     */
    DARKEN("变暗", "取较暗的像素"),

    /**
     * 变亮模式
     * 取上下层中较亮的像素值
     */
    LIGHTEN("变亮", "取较亮的像素"),

    /**
     * 差值模式
     * 上下层像素的绝对差值
     * 使用自定义实现
     */
    DIFFERENCE("差值", "取像素绝对差值"),

    /**
     * 色相模式
     * 使用上层的色相和下层的饱和度/明度
     * 使用 ColorMatrix 实现
     */
    HUE("色相", "使用上层的色相"),

    /**
     * 饱和度模式
     * 使用上层的饱和度和下层的色相/明度
     * 使用 ColorMatrix 实现
     */
    SATURATION("饱和度", "使用上层的饱和度"),

    /**
     * 颜色模式
     * 使用上层的色相和饱和度，下层的明度
     * 使用 ColorMatrix 实现
     */
    COLOR("颜色", "使用上层的色相和饱和度"),

    /**
     * 明度模式
     * 使用上层的明度和下层的色相/饱和度
     * 使用 ColorMatrix 实现
     */
    LUMINOSITY("明度", "使用上层的明度"),

    /**
     * 源覆盖模式
     * 等同于 NORMAL，主要用于兼容性
     */
    SRC_OVER("源覆盖", "上层覆盖下层（源覆盖）");

    /**
     * 转换为 Android PorterDuff.Mode
     * 注意：不是所有混合模式都有对应的 PorterDuff.Mode
     * 对于不支持的模式，返回 null
     */
    fun toPorterDuffMode(): PorterDuff.Mode? = when (this) {
        NORMAL -> PorterDuff.Mode.SRC_OVER
        MULTIPLY -> PorterDuff.Mode.MULTIPLY
        SCREEN -> PorterDuff.Mode.SCREEN
        OVERLAY -> PorterDuff.Mode.OVERLAY // API 19+
        DARKEN -> PorterDuff.Mode.DARKEN
        LIGHTEN -> PorterDuff.Mode.LIGHTEN
        SRC_OVER -> PorterDuff.Mode.SRC_OVER
        else -> null // 需要自定义实现
    }

    /**
     * 转换为 Android BlendMode (API 29+)
     * 返回 null 表示需要使用其他方式实现
     */
    fun toAndroidBlendMode(): AndroidBlendMode? = when (this) {
        NORMAL -> AndroidBlendMode.SRC_OVER
        MULTIPLY -> AndroidBlendMode.MULTIPLY
        SCREEN -> AndroidBlendMode.SCREEN
        OVERLAY -> AndroidBlendMode.OVERLAY
        SOFT_LIGHT -> AndroidBlendMode.SOFT_LIGHT
        HARD_LIGHT -> AndroidBlendMode.HARD_LIGHT
        COLOR_DODGE -> AndroidBlendMode.COLOR_DODGE
        COLOR_BURN -> AndroidBlendMode.COLOR_BURN
        DARKEN -> AndroidBlendMode.DARKEN
        LIGHTEN -> AndroidBlendMode.LIGHTEN
        DIFFERENCE -> AndroidBlendMode.DIFFERENCE
        HUE -> null // 使用 ColorMatrix
        SATURATION -> null // 使用 ColorMatrix
        COLOR -> null // 使用 ColorMatrix
        LUMINOSITY -> null // 使用 ColorMatrix
        SRC_OVER -> AndroidBlendMode.SRC_OVER
    }

    /**
     * 检查是否需要使用 ColorMatrix 实现
     */
    fun requiresColorMatrix(): Boolean = when (this) {
        HUE, SATURATION, COLOR, LUMINOSITY -> true
        else -> false
    }

    /**
     * 检查是否需要自定义像素级实现
     * (当前版本对于 SOFT_LIGHT, HARD_LIGHT 等使用近似实现)
     */
    fun requiresCustomImplementation(): Boolean = when (this) {
        OVERLAY, SOFT_LIGHT, HARD_LIGHT, COLOR_DODGE, COLOR_BURN, DIFFERENCE -> true
        else -> false
    }

    companion object {
        /**
         * 获取所有混合模式的列表
         */
        fun getAllBlendModes(): List<BlendMode> = entries.toList()
    }
}

/**
 * 混合模式工具类
 * 提供混合模式相关的辅助方法
 */
object BlendModeUtils {

    /**
     * 为 Paint 设置混合模式
     * 自动选择合适的实现方式（BlendMode / PorterDuff.Mode / ColorMatrix）
     * @param paint 目标 Paint 对象
     * @param blendMode 混合模式
     * @param applyNow 是否立即应用
     */
    @Suppress("DEPRECATION")
    fun applyBlendModeToPaint(paint: Paint, blendMode: BlendMode, applyNow: Boolean = true) {
        paint.blendMode = null // 先清除
        paint.xfermode = null

        // 尝试使用 Android BlendMode (API 29+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            blendMode.toAndroidBlendMode()?.let {
                paint.blendMode = it
                return
            }
        }

        // 回退到 PorterDuff.Mode
        blendMode.toPorterDuffMode()?.let {
            paint.xfermode = PorterDuffXfermode(it)
            return
        }

        // 对于需要 ColorMatrix 的模式，创建 ColorFilter
        if (blendMode.requiresColorMatrix()) {
            val colorMatrix = createColorMatrixForBlendMode(blendMode)
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            // 设置为 SRC_OVER 以应用 ColorFilter
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
    }

    /**
     * 为特定混合模式创建 ColorMatrix
     * 主要用于 HUE/SATURATION/COLOR/LUMINOSITY 模式
     */
    fun createColorMatrixForBlendMode(blendMode: BlendMode, sourceColor: Int = 0): ColorMatrix {
        return when (blendMode) {
            HUE -> createHueShiftColorMatrix(sourceColor)
            SATURATION -> createSaturationColorMatrix(sourceColor)
            COLOR -> createColorModeColorMatrix(sourceColor)
            LUMINOSITY -> createLuminosityColorMatrix(sourceColor)
            else -> ColorMatrix() // 默认恒等矩阵
        }
    }

    /**
     * 创建色相偏移 ColorMatrix
     */
    private fun createHueShiftColorMatrix(sourceColor: Int): ColorMatrix {
        val hsv = FloatArray(3)
        android.graphics.Color.HSVToColor(android.graphics.Color.alpha(sourceColor), hsv)
        return ColorMatrix().apply {
            setRotate(0, hsv[0]) // R 通道色相
            setRotate(1, hsv[0]) // G 通道色相
            setRotate(2, hsv[0]) // B 通道色相
        }
    }

    /**
     * 创建饱和度 ColorMatrix
     */
    private fun createSaturationColorMatrix(sourceColor: Int): ColorMatrix {
        val hsv = FloatArray(3)
        android.graphics.Color.HSVToColor(android.graphics.Color.alpha(sourceColor), hsv)
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(hsv[1]) // 使用源颜色的饱和度
        return satMatrix
    }

    /**
     * 创建颜色模式 ColorMatrix
     */
    private fun createColorModeColorMatrix(sourceColor: Int): ColorMatrix {
        val hsv = FloatArray(3)
        android.graphics.Color.HSVToColor(android.graphics.Color.alpha(sourceColor), hsv)
        return ColorMatrix().apply {
            // 结合色相和饱和度
            setSaturation(hsv[1])
            setRotate(0, hsv[0])
            setRotate(1, hsv[0])
            setRotate(2, hsv[0])
        }
    }

    /**
     * 创建明度 ColorMatrix
     */
    private fun createLuminosityColorMatrix(sourceColor: Int): ColorMatrix {
        val hsv = FloatArray(3)
        android.graphics.Color.HSVToColor(android.graphics.Color.alpha(sourceColor), hsv)
        // 明度影响整体亮度
        val luminosityMatrix = ColorMatrix()
        val brightness = hsv[2] / 255f
        luminosityMatrix.setScale(brightness, brightness, brightness, 1f)
        return luminosityMatrix
    }

    /**
     * 创建近似 OVERLAY 效果的 ColorMatrix
     * 这是一个简化的近似实现
     */
    fun createOverlayApproximationMatrix(): ColorMatrix {
        // OVERLAY 的简化实现
        // 实际上需要根据每个像素是暗还是亮来选择 MULTIPLY 或 SCREEN
        // 这里使用一个近似：结合对比度和亮度调整
        return ColorMatrix().apply {
            val contrast = 1.2f // 增加对比度
            val translate = (-.5f * contrast + .5f) * 255f
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
    }

    /**
     * 创建离屏渲染使用的 Paint（包含混合模式）
     */
    fun createBlendedPaint(blendMode: BlendMode, alpha: Float = 1f): Paint {
        return Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            applyBlendModeToPaint(this, blendMode)
        }
    }
}
