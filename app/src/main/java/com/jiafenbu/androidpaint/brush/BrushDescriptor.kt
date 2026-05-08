package com.jiafenbu.androidpaint.brush

/**
 * 笔刷参数描述
 * 定义笔刷的所有可调参数
 * @param type 笔刷类型
 * @param size 大小（1-100）
 * @param opacity 透明度（0-1）
 * @param spacing 间距（笔刷大小的百分比，0-1）
 * @param jitter 抖动（0-1，线条随机偏移量）
 */
data class BrushDescriptor(
    val type: BrushType = BrushType.PENCIL,
    val size: Float = 10f,
    val opacity: Float = 1f,
    val spacing: Float = 0.1f,
    val jitter: Float = 0f
) {
    companion object {
        /**
         * 保存每种笔刷上次使用的参数（内存缓存）
         */
        private val savedParams = mutableMapOf<BrushType, BrushDescriptor>()

        /**
         * 获取指定类型的默认笔刷参数
         */
        fun getDefault(type: BrushType): BrushDescriptor = when (type) {
            BrushType.PENCIL -> BrushDescriptor(
                type = type,
                size = 4f,
                opacity = 1f,
                spacing = 0.05f,
                jitter = 0.1f
            )
            BrushType.INK_PEN -> BrushDescriptor(
                type = type,
                size = 6f,
                opacity = 1f,
                spacing = 0.02f,
                jitter = 0f
            )
            BrushType.WATERCOLOR -> BrushDescriptor(
                type = type,
                size = 20f,
                opacity = 0.6f,
                spacing = 0.05f,
                jitter = 0.05f
            )
            BrushType.MARKER -> BrushDescriptor(
                type = type,
                size = 15f,
                opacity = 0.5f,
                spacing = 0.03f,
                jitter = 0f
            )
            BrushType.SPRAY -> BrushDescriptor(
                type = type,
                size = 30f,
                opacity = 0.4f,
                spacing = 0.15f,
                jitter = 0.8f
            )
            BrushType.OIL_BRUSH -> BrushDescriptor(
                type = type,
                size = 25f,
                opacity = 0.9f,
                spacing = 0.08f,
                jitter = 0.15f
            )
            BrushType.CRAYON -> BrushDescriptor(
                type = type,
                size = 18f,
                opacity = 0.7f,
                spacing = 0.04f,
                jitter = 0.3f
            )
            BrushType.BLUR -> BrushDescriptor(
                type = type,
                size = 25f,
                opacity = 1f,
                spacing = 0.05f,
                jitter = 0f
            )
            BrushType.SMUDGE -> BrushDescriptor(
                type = type,
                size = 20f,
                opacity = 0.8f,
                spacing = 0.05f,
                jitter = 0f
            )
            BrushType.FILL -> BrushDescriptor(
                type = type,
                size = 1f,
                opacity = 1f,
                spacing = 0f,
                jitter = 0f
            )
            BrushType.ERASER -> BrushDescriptor(
                type = type,
                size = 20f,
                opacity = 1f,
                spacing = 0.05f,
                jitter = 0f
            )
            // 新增笔刷默认参数
            BrushType.CALLIGRAPHY -> BrushDescriptor(
                type = type,
                size = 12f,
                opacity = 1f,
                spacing = 0.02f,
                jitter = 0.05f
            )
            BrushType.AIRBRUSH -> BrushDescriptor(
                type = type,
                size = 40f,
                opacity = 0.3f,
                spacing = 0.1f,
                jitter = 0.5f
            )
            BrushType.PIXEL -> BrushDescriptor(
                type = type,
                size = 4f,
                opacity = 1f,
                spacing = 0.25f,
                jitter = 0f
            )
            BrushType.NEON -> BrushDescriptor(
                type = type,
                size = 8f,
                opacity = 1f,
                spacing = 0.03f,
                jitter = 0f
            )
            BrushType.PATTERN_BRUSH -> BrushDescriptor(
                type = type,
                size = 15f,
                opacity = 0.9f,
                spacing = 0.3f,
                jitter = 0.1f
            )
            BrushType.HAIR -> BrushDescriptor(
                type = type,
                size = 10f,
                opacity = 0.8f,
                spacing = 0.08f,
                jitter = 0.2f
            )
            // 第二批新增笔刷默认参数
            BrushType.CHARCOAL -> BrushDescriptor(
                type = type,
                size = 12f,
                opacity = 0.85f,
                spacing = 0.04f,
                jitter = 0.4f
            )
            BrushType.FOUNTAIN_PEN -> BrushDescriptor(
                type = type,
                size = 3f,
                opacity = 1f,
                spacing = 0.02f,
                jitter = 0f
            )
            BrushType.SPONGE -> BrushDescriptor(
                type = type,
                size = 30f,
                opacity = 0.6f,
                spacing = 0.25f,
                jitter = 0.3f
            )
            BrushType.RIBBON -> BrushDescriptor(
                type = type,
                size = 15f,
                opacity = 0.9f,
                spacing = 0.03f,
                jitter = 0.05f
            )
            BrushType.STAMP -> BrushDescriptor(
                type = type,
                size = 20f,
                opacity = 1f,
                spacing = 0.5f,
                jitter = 0f
            )
            BrushType.GLITTER -> BrushDescriptor(
                type = type,
                size = 25f,
                opacity = 0.8f,
                spacing = 0.15f,
                jitter = 0.6f
            )
        }

        /**
         * 保存笔刷参数
         * @param descriptor 要保存的笔刷参数
         */
        fun saveParams(descriptor: BrushDescriptor) {
            savedParams[descriptor.type] = descriptor
        }

        /**
         * 获取上次保存的参数，如果没有则返回默认值
         * @param type 笔刷类型
         * @return 保存的参数或默认值
         */
        fun getSavedOrDefault(type: BrushType): BrushDescriptor {
            return savedParams[type] ?: getDefault(type)
        }

        /**
         * 清除所有保存的参数
         */
        fun clearSavedParams() {
            savedParams.clear()
        }
    }
}
