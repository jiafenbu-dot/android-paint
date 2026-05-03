package com.jiafenbu.androidpaint.model

/**
 * 项目元数据
 * 定义项目文件的基本信息
 */
data class ProjectMetadata(
    val name: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
    val modifiedAt: Long,
    val layers: List<LayerMetadata>
)

/**
 * 图层元数据
 * 用于项目文件保存时记录图层信息
 */
data class LayerMetadata(
    val id: Long,
    val name: String,
    val isVisible: Boolean,
    val opacity: Float,
    val blendMode: BlendMode,
    val strokeCount: Int
)

/**
 * 项目文件格式定义
 * 包含序列化的笔画数据和元数据
 */
data class ProjectFile(
    val metadata: ProjectMetadata,
    val layerStrokes: Map<Long, List<StrokeData>>
)

/**
 * 项目创建配置
 * 用于创建新项目时传递参数
 */
data class ProjectCreateConfig(
    val name: String,
    val width: Int,
    val height: Int,
    val layerCount: Int = 1
) {
    companion object {
        /** 默认预设尺寸 */
        val PHONE_WALLPAPER = ProjectCreateConfig("手机壁纸", 1080, 1920)
        val SQUARE = ProjectCreateConfig("方形", 2048, 2048)
        val UHD_4K = ProjectCreateConfig("4K", 3840, 2160)
        val A4 = ProjectCreateConfig("A4", 2480, 3508)
        
        /** 默认配置 */
        val DEFAULT = PHONE_WALLPAPER
    }
}
