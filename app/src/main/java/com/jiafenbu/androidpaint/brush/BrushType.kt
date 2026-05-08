package com.jiafenbu.androidpaint.brush

/**
 * 笔刷类型枚举
 * @param displayName 显示名称
 * @param icon 图标emoji
 */
enum class BrushType(val displayName: String, val icon: String) {
    PENCIL("铅笔", "✏️"),
    INK_PEN("钢笔", "🖊️"),
    WATERCOLOR("水彩", "🎨"),
    MARKER("马克笔", "📝"),
    SPRAY("喷枪", "💨"),
    OIL_BRUSH("油画笔", "🖌️"),
    CRAYON("蜡笔", "🖍️"),
    BLUR("模糊笔", "🌫️"),
    SMUDGE("涂抹笔", "👆"),
    FILL("填充笔", "🪣"),
    ERASER("橡皮擦", "🧹"),
    // 新增笔刷类型
    CALLIGRAPHY("书法笔", "🖌️"),
    AIRBRUSH("气笔", "✨"),
    PIXEL("像素笔", "📱"),
    NEON("霓虹笔", "🌈"),
    PATTERN_BRUSH("图案笔", "⭐"),
    HAIR("毛发笔", "💇"),
    // 第二批新增笔刷
    CHARCOAL("炭笔", "🔲"),
    FOUNTAIN_PEN("蘸水笔", "🖋️"),
    SPONGE("海绵笔", "🧽"),
    RIBBON("丝带笔", "🎀"),
    STAMP("印章笔", "🔖"),
    GLITTER("闪粉笔", "💎")
}
