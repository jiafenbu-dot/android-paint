package com.jiafenbu.androidpaint.command

import com.jiafenbu.androidpaint.model.Palette
import com.jiafenbu.androidpaint.model.PaletteColor
import com.jiafenbu.androidpaint.palette.PaletteManager

/**
 * 添加颜色到色板命令
 * 
 * @param paletteManager 色板管理器
 * @param paletteId 色板 ID
 * @param color 要添加的颜色
 */
class AddColorToPaletteCommand(
    private val paletteManager: PaletteManager,
    private val paletteId: Long,
    private val color: PaletteColor
) : DrawCommand {
    
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        paletteManager.addColorToPalette(paletteId, color)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted) return
        // 找到添加的颜色索引并移除
        val palette = paletteManager.findPaletteById(paletteId)
        palette?.let {
            if (it.colors.isNotEmpty()) {
                paletteManager.removeColorFromPalette(paletteId, it.colors.size - 1)
            }
        }
        isExecuted = false
    }
}

/**
 * 从色板移除颜色命令
 * 
 * @param paletteManager 色板管理器
 * @param paletteId 色板 ID
 * @param colorIndex 颜色索引
 */
class RemoveColorFromPaletteCommand(
    private val paletteManager: PaletteManager,
    private val paletteId: Long,
    private val colorIndex: Int
) : DrawCommand {
    
    private var removedColor: PaletteColor? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        // 保存被移除的颜色
        val palette = paletteManager.findPaletteById(paletteId)
        removedColor = palette?.getColor(colorIndex)
        
        paletteManager.removeColorFromPalette(paletteId, colorIndex)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || removedColor == null) return
        
        // 重新添加颜色
        paletteManager.addColorToPalette(paletteId, removedColor!!)
        isExecuted = false
    }
}

/**
 * 更新色板中颜色命令
 * 
 * @param paletteManager 色板管理器
 * @param paletteId 色板 ID
 * @param colorIndex 颜色索引
 * @param newColor 新的颜色
 */
class UpdatePaletteColorCommand(
    private val paletteManager: PaletteManager,
    private val paletteId: Long,
    private val colorIndex: Int,
    private val newColor: PaletteColor
) : DrawCommand {
    
    private var oldColor: PaletteColor? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        // 保存旧颜色
        val palette = paletteManager.findPaletteById(paletteId)
        oldColor = palette?.getColor(colorIndex)
        
        paletteManager.updateColorInPalette(paletteId, colorIndex, newColor)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || oldColor == null) return
        
        paletteManager.updateColorInPalette(paletteId, colorIndex, oldColor!!)
        isExecuted = false
    }
}

/**
 * 创建新色板命令
 * 
 * @param paletteManager 色板管理器
 * @param name 色板名称
 * @param colors 初始颜色
 */
class CreatePaletteCommand(
    private val paletteManager: PaletteManager,
    private val name: String,
    private val colors: List<PaletteColor> = emptyList()
) : DrawCommand {
    
    private var createdPalette: Palette? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        createdPalette = paletteManager.createPalette(name, colors)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || createdPalette == null) return
        
        paletteManager.deletePalette(createdPalette!!.id)
        isExecuted = false
    }
    
    /**
     * 获取创建的色板
     */
    fun getCreatedPalette(): Palette? = createdPalette
}

/**
 * 删除色板命令
 * 
 * @param paletteManager 色板管理器
 * @param paletteId 色板 ID
 */
class DeletePaletteCommand(
    private val paletteManager: PaletteManager,
    private val paletteId: Long
) : DrawCommand {
    
    private var deletedPalette: Palette? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        // 保存被删除的色板
        deletedPalette = paletteManager.findPaletteById(paletteId)
        
        paletteManager.deletePalette(paletteId)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || deletedPalette == null) return
        
        // 重新创建色板
        deletedPalette?.let { palette ->
            val newPalette = paletteManager.createPalette(palette.name, palette.colors)
            // 注意：ID 不会完全一致，因为新创建的会有新 ID
        }
        isExecuted = false
    }
}

/**
 * 重命名色板命令
 * 
 * @param paletteManager 色板管理器
 * @param paletteId 色板 ID
 * @param newName 新名称
 */
class RenamePaletteCommand(
    private val paletteManager: PaletteManager,
    private val paletteId: Long,
    private val newName: String
) : DrawCommand {
    
    private var oldName: String? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        // 保存旧名称
        val palette = paletteManager.findPaletteById(paletteId)
        oldName = palette?.name
        
        paletteManager.renamePalette(paletteId, newName)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || oldName == null) return
        
        paletteManager.renamePalette(paletteId, oldName!!)
        isExecuted = false
    }
}

/**
 * 导入 ASE 文件命令
 * 
 * @param paletteManager 色板管理器
 * @param data ASE 文件数据
 */
class ImportAsePaletteCommand(
    private val paletteManager: PaletteManager,
    private val data: ByteArray
) : DrawCommand {
    
    private var importedPalettes: List<Palette> = emptyList()
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        importedPalettes = paletteManager.importFromAse(data)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted) return
        
        // 移除导入的色板
        importedPalettes.forEach { palette ->
            paletteManager.deletePalette(palette.id)
        }
        isExecuted = false
    }
    
    /**
     * 获取导入的色板列表
     */
    fun getImportedPalettes(): List<Palette> = importedPalettes
}

/**
 * 复制色板命令
 * 
 * @param paletteManager 色板管理器
 * @param sourcePaletteId 源色板 ID
 */
class DuplicatePaletteCommand(
    private val paletteManager: PaletteManager,
    private val sourcePaletteId: Long
) : DrawCommand {
    
    private var duplicatedPalette: Palette? = null
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        duplicatedPalette = paletteManager.duplicatePalette(sourcePaletteId)
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted || duplicatedPalette == null) return
        
        paletteManager.deletePalette(duplicatedPalette!!.id)
        isExecuted = false
    }
    
    /**
     * 获取复制的色板
     */
    fun getDuplicatedPalette(): Palette? = duplicatedPalette
}

/**
 * 重置色板命令
 * 清除所有自定义色板和最近使用颜色
 * 
 * @param paletteManager 色板管理器
 * @param backupPalettes 备份的自定义色板
 * @param backupRecentColors 备份的最近使用颜色
 */
class ResetPalettesCommand(
    private val paletteManager: PaletteManager,
    private val backupPalettes: List<Palette>,
    private val backupRecentColors: List<Int>
) : DrawCommand {
    
    private var isExecuted = false
    
    override fun execute() {
        if (isExecuted) return
        
        paletteManager.reset()
        isExecuted = true
    }
    
    override fun undo() {
        if (!isExecuted) return
        
        // 恢复备份的色板
        backupPalettes.forEach { palette ->
            paletteManager.createPalette(palette.name, palette.colors)
        }
        
        // 恢复最近使用颜色
        backupRecentColors.forEach { color ->
            paletteManager.addToRecentColors(color)
        }
        
        isExecuted = false
    }
}
