package com.jiafenbu.androidpaint.model

import android.graphics.Bitmap
import java.lang.ref.SoftReference

/**
 * 图层数据模型
 * 每个图层包含独立的笔画列表，支持混合模式、透明度、锁定等功能
 * 
 * @param id 图层唯一标识
 * @param name 图层名称
 * @param strokes 笔画列表
 * @param isVisible 是否可见
 * @param opacity 图层透明度 (0f - 1f)
 * @param blendMode 混合模式，默认为正常混合
 * @param isLocked 是否锁定透明像素（锁定后只能修改已有像素区域）
 * @param thumbnail 缩略图缓存（使用 SoftReference 以便 GC 回收）
 * @param layerType 图层类型（NORMAL 普通图层 / TEXT 文字图层）
 * @param textLayerModel 文字图层数据（仅当 layerType 为 TEXT 时有效）
 */
data class LayerModel(
    val id: Long,
    val name: String,
    val strokes: MutableList<StrokeData> = mutableListOf(),
    val isVisible: Boolean = true,
    val opacity: Float = 1f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val isLocked: Boolean = false,
    var thumbnail: Bitmap? = null,
    var layerType: LayerType = LayerType.NORMAL,
    var textLayerModel: TextLayerModel? = null
) {
    // 使用软引用保存缩略图，便于内存紧张时回收
    private var _thumbnailRef: SoftReference<Bitmap>? = thumbnail?.let { SoftReference(it) }
    
    /**
     * 获取缩略图
     * @return 缩略图位图，如果已被回收则返回 null
     */
    var thumbnailBitmap: Bitmap?
        get() = _thumbnailRef?.get()
        private set(value) {
            _thumbnailRef = value?.let { SoftReference(it) }
        }

    companion object {
        private var nextId = 0L
        
        /**
         * 创建新的图层，自动分配唯一ID
         * @param name 图层名称
         * @return 新创建的图层
         */
        fun create(name: String): LayerModel {
            return LayerModel(
                id = nextId++,
                name = name,
                strokes = mutableListOf(),
                isVisible = true,
                opacity = 1f,
                blendMode = BlendMode.NORMAL,
                isLocked = false,
                thumbnail = null
            )
        }

        /**
         * 重置 ID 计数器（主要用于测试或清空画布）
         */
        fun resetIdCounter() {
            nextId = 0L
        }

        /**
         * 获取下一个将分配的 ID
         */
        fun getNextId(): Long = nextId
    }

    /**
     * 更新缩略图
     * @param bitmap 新的缩略图位图
     */
    fun updateThumbnail(bitmap: Bitmap?) {
        // 回收旧的缩略图
        _thumbnailRef?.get()?.let { oldBitmap ->
            if (!oldBitmap.isRecycled && oldBitmap != bitmap) {
                // 不在这里回收，让 GC 处理
            }
        }
        thumbnailBitmap = bitmap
    }

    /**
     * 清除缩略图缓存
     */
    fun clearThumbnail() {
        thumbnailBitmap = null
    }

    /**
     * 复制图层（深拷贝）
     * 创建一个新的图层，包含相同的笔画和设置
     * @return 新的图层副本
     */
    fun copy(
        id: Long = this.id,
        name: String = "${this.name} 副本",
        strokes: MutableList<StrokeData> = this.strokes.toMutableList(),
        isVisible: Boolean = this.isVisible,
        opacity: Float = this.opacity,
        blendMode: BlendMode = this.blendMode,
        isLocked: Boolean = this.isLocked,
        layerType: LayerType = this.layerType,
        textLayerModel: TextLayerModel? = this.textLayerModel?.copy()
    ): LayerModel {
        return LayerModel(
            id = id,
            name = name,
            strokes = strokes,
            isVisible = isVisible,
            opacity = opacity,
            blendMode = blendMode,
            isLocked = isLocked,
            thumbnail = this.thumbnailBitmap?.copy(this.thumbnailBitmap?.config ?: Bitmap.Config.ARGB_8888, false),
            layerType = layerType,
            textLayerModel = textLayerModel
        )
    }

    /**
     * 复制图层并生成新的 ID
     * 用于图层复制功能
     * @return 新的图层副本，带有新 ID
     */
    fun duplicate(): LayerModel {
        val newId = Companion.nextId++
        return copy(
            id = newId,
            name = "${this.name} 副本"
        )
    }

    /**
     * 检查图层是否有内容
     * @return 是否有任何笔画
     */
    fun hasContent(): Boolean = strokes.isNotEmpty()

    /**
     * 获取图层内容数量
     * @return 笔画数量
     */
    fun getContentCount(): Int = strokes.size
}

/**
 * 图层属性类型
 * 用于 SetLayerPropertyCommand 通用属性修改命令
 */
enum class LayerProperty {
    NAME,
    VISIBILITY,
    OPACITY,
    BLEND_MODE,
    LOCKED
}

/**
 * 图层层级关系
 * 用于描述图层合并时的层级处理
 */
enum class LayerOrder {
    TOP,
    BOTTOM,
    SPECIFIC_INDEX
}
