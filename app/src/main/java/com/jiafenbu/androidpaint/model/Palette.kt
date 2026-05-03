package com.jiafenbu.androidpaint.model

/**
 * 色板类型
 */
enum class PaletteType {
    CUSTOM,      // 用户自定义色板
    SKIN,        // 肤色色板
    NATURE,      // 自然色色板
    MORANDI,     // 莫兰迪色板
    PASTEL,      // 粉彩色板
    VINTAGE,     // 复古色板
    NEON         // 霓虹色板
}

/**
 * 色板颜色条目
 * 
 * @param color 颜色值（ARGB格式）
 * @param name 颜色名称（可选）
 */
data class PaletteColor(
    val color: Int,
    val name: String = ""
)

/**
 * 色板数据模型
 * 存储一组相关颜色的集合
 * 
 * @param id 色板唯一标识
 * @param name 色板名称
 * @param type 色板类型
 * @param colors 颜色列表
 * @param isDefault 是否为内置预设
 * @param createdAt 创建时间
 */
data class Palette(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val type: PaletteType = PaletteType.CUSTOM,
    val colors: List<PaletteColor> = emptyList(),
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private var nextId = 0L
        
        /**
         * 创建新的色板
         */
        fun create(
            name: String,
            type: PaletteType = PaletteType.CUSTOM,
            colors: List<PaletteColor> = emptyList(),
            isDefault: Boolean = false
        ): Palette {
            return Palette(
                id = nextId++,
                name = name,
                type = type,
                colors = colors,
                isDefault = isDefault,
                createdAt = System.currentTimeMillis()
            )
        }
        
        /**
         * 重置 ID 计数器
         */
        fun resetIdCounter() {
            nextId = 0L
        }
        
        // ==================== 预设色板 ====================
        
        /**
         * 肤色色板
         * 包含各种肤色色调
         */
        val SKIN_TONES = Palette(
            id = -1,
            name = "肤色",
            type = PaletteType.SKIN,
            colors = listOf(
                PaletteColor(0xFFFFF5E6.toInt(), "浅白"),
                PaletteColor(0xFFFFE4C4.toInt(), "米白"),
                PaletteColor(0xFFFFDEAD.toInt(), "淡黄"),
                PaletteColor(0xFFD2B48C.toInt(), "浅棕"),
                PaletteColor(0xFFC4A484.toInt(), "自然"),
                PaletteColor(0xFFB8860B.toInt(), "健康"),
                PaletteColor(0xFF8B4513.toInt(), "古铜"),
                PaletteColor(0xFF5D3A1A.toInt(), "深棕"),
                PaletteColor(0xFFFFCBA4.toInt(), "粉嫩"),
                PaletteColor(0xFFE8B796.toInt(), "暖色"),
                PaletteColor(0xFFD4956A.toInt(), "偏深"),
                PaletteColor(0xFFA67B5B.toInt(), "自然深")
            ),
            isDefault = true
        )
        
        /**
         * 自然色色板
         * 包含蓝天、绿草、大海等自然色彩
         */
        val NATURE_COLORS = Palette(
            id = -2,
            name = "自然",
            type = PaletteType.NATURE,
            colors = listOf(
                PaletteColor(0xFF87CEEB.toInt(), "天空蓝"),
                PaletteColor(0xFF00CED1.toInt(), "青绿"),
                PaletteColor(0xFF228B22.toInt(), "森林绿"),
                PaletteColor(0xFF32CD32.toInt(), "草绿"),
                PaletteColor(0xFFFFA500.toInt(), "橙黄"),
                PaletteColor(0xFF8B4513.toInt(), "土地"),
                PaletteColor(0xFFFFFAFA.toInt(), "雪花"),
                PaletteColor(0xFF708090.toInt(), "岩石"),
                PaletteColor(0xFFFF6347.toInt(), "枫红"),
                PaletteColor(0xFFFFD700.toInt(), "金黄"),
                PaletteColor(0xFF9370DB.toInt(), "紫藤"),
                PaletteColor(0xFF20B2AA.toInt(), "海绿")
            ),
            isDefault = true
        )
        
        /**
         * 莫兰迪色色板
         * 低饱和度的柔和色调
         */
        val MORANDI_COLORS = Palette(
            id = -3,
            name = "莫兰迪",
            type = PaletteType.MORANDI,
            colors = listOf(
                PaletteColor(0xFFB8B5B1.toInt(), "雾霾灰"),
                PaletteColor(0xFFD4C4B5.toInt(), "奶茶色"),
                PaletteColor(0xFFE8D5C4.toInt(), "杏仁色"),
                PaletteColor(0xFFC9B7A3.toInt(), "卡其色"),
                PaletteColor(0xFFA8A39B.toInt(), "石灰色"),
                PaletteColor(0xFFD5CEC3.toInt(), "燕麦色"),
                PaletteColor(0xFFE5D1C3.toInt(), "藕荷色"),
                PaletteColor(0xFFB5A7A0.toInt(), "灰褐色"),
                PaletteColor(0xFFD7CCC8.toInt(), "暖灰色"),
                PaletteColor(0xFFC5B9B0.toInt(), "米灰色"),
                PaletteColor(0xFFE2D8D0.toInt(), "象牙白"),
                PaletteColor(0xFFAFA59C.toInt(), "深灰褐")
            ),
            isDefault = true
        )
        
        /**
         * 粉彩色色板
         * 柔和的少女色调
         */
        val PASTEL_COLORS = Palette(
            id = -4,
            name = "粉彩",
            type = PaletteType.PASTEL,
            colors = listOf(
                PaletteColor(0xFFFFB6C1.toInt(), "粉红"),
                PaletteColor(0xFFFFE4E1.toInt(), "浅粉"),
                PaletteColor(0xFFE6E6FA.toInt(), "淡紫"),
                PaletteColor(0xFFB0E0E6.toInt(), "浅蓝"),
                PaletteColor(0xFF98FB98.toInt(), "浅绿"),
                PaletteColor(0xFFFFFFE0.toInt(), "鹅黄"),
                PaletteColor(0xFFFFFACD.toInt(), "柠檬"),
                PaletteColor(0xFFADD8E6.toInt(), "天蓝"),
                PaletteColor(0xFF87CEFA.toInt(), "浅蓝"),
                PaletteColor(0xFFDDA0DD.toInt(), "梅红"),
                PaletteColor(0xFFF0E68C.toInt(), "淡黄"),
                PaletteColor(0xFFD8BFD8.toInt(), "藕粉")
            ),
            isDefault = true
        )
        
        /**
         * 复古色色板
         * 怀旧风格的色调
         */
        val VINTAGE_COLORS = Palette(
            id = -5,
            name = "复古",
            type = PaletteType.VINTAGE,
            colors = listOf(
                PaletteColor(0xFF8B0000.toInt(), "深红"),
                PaletteColor(0xFFB8860B.toInt(), "暗金"),
                PaletteColor(0xFF556B2F.toInt(), "墨绿"),
                PaletteColor(0xFF4A4A4A.toInt(), "炭灰"),
                PaletteColor(0xFF8B4513.toInt(), "赭石"),
                PaletteColor(0xFF483C32.toInt(), "咖啡"),
                PaletteColor(0xFF7CFC00.toInt(), "荧光"),
                PaletteColor(0xFFCD853F.toInt(), "秘鲁"),
                PaletteColor(0xFF6B8E23.toInt(), "橄榄"),
                PaletteColor(0xFFD2691E.toInt(), "巧克力"),
                PaletteColor(0xFFFF6347.toInt(), "番茄"),
                PaletteColor(0xFF2F4F4F.toInt(), "深青")
            ),
            isDefault = true
        )
        
        /**
         * 霓虹色色板
         * 鲜艳的霓虹色调
         */
        val NEON_COLORS = Palette(
            id = -6,
            name = "霓虹",
            type = PaletteType.NEON,
            colors = listOf(
                PaletteColor(0xFFFF1493.toInt(), "深粉"),
                PaletteColor(0xFFFF00FF.toInt(), "品红"),
                PaletteColor(0xFF00FF00.toInt(), "荧光绿"),
                PaletteColor(0xFF00FFFF.toInt(), "青色"),
                PaletteColor(0xFFFFFF00.toInt(), "明黄"),
                PaletteColor(0xFFFF6600.toInt(), "橙红"),
                PaletteColor(0xFFFF00CC.toInt(), "洋红"),
                PaletteColor(0xFF00FF7F.toInt(), "春绿"),
                PaletteColor(0xFF7FFFD4.toInt(), "薄荷"),
                PaletteColor(0xFFFF6B6B.toInt(), "珊瑚"),
                PaletteColor(0xFF4ECDC4.toInt(), "青绿"),
                PaletteColor(0xFFFFE66D.toInt(), "向日葵")
            ),
            isDefault = true
        )
        
        /**
         * 获取所有预设色板
         */
        fun getDefaultPalettes(): List<Palette> = listOf(
            SKIN_TONES,
            NATURE_COLORS,
            MORANDI_COLORS,
            PASTEL_COLORS,
            VINTAGE_COLORS,
            NEON_COLORS
        )
    }
    
    /**
     * 获取颜色数量
     */
    fun colorCount(): Int = colors.size
    
    /**
     * 获取颜色（按索引）
     */
    fun getColor(index: Int): PaletteColor? = colors.getOrNull(index)
    
    /**
     * 添加颜色
     */
    fun addColor(color: PaletteColor): Palette {
        return copy(colors = colors + color)
    }
    
    /**
     * 移除颜色
     */
    fun removeColor(index: Int): Palette {
        if (index < 0 || index >= colors.size) return this
        return copy(colors = colors.toMutableList().apply { removeAt(index) })
    }
    
    /**
     * 更新颜色
     */
    fun updateColor(index: Int, color: PaletteColor): Palette {
        if (index < 0 || index >= colors.size) return this
        val newColors = colors.toMutableList()
        newColors[index] = color
        return copy(colors = newColors)
    }
}

/**
 * 参考图数据
 * 
 * @param uri 图片 URI
 * @param opacity 透明度
 * @param isVisible 是否可见
 * @param positionX X 位置
 * @param positionY Y 位置
 * @param scale 缩放比例
 * @param rotation 旋转角度
 */
data class ReferenceImage(
    val uri: String,
    val opacity: Float = 0.5f,
    val isVisible: Boolean = true,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f
)
