package com.jiafenbu.androidpaint.palette

import android.content.Context
import android.content.SharedPreferences
import com.jiafenbu.androidpaint.model.Palette
import com.jiafenbu.androidpaint.model.PaletteColor
import org.json.JSONArray
import org.json.JSONObject

/**
 * 色板管理器
 * 管理色板的创建、保存、加载和预设
 */
class PaletteManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // 内存缓存
    private val customPalettes = mutableListOf<Palette>()
    
    init {
        loadCustomPalettes()
    }
    
    // ==================== 预设色板 ====================
    
    /**
     * 获取所有预设色板
     */
    fun getDefaultPalettes(): List<Palette> = Palette.getDefaultPalettes()
    
    /**
     * 根据类型获取预设色板
     */
    fun getDefaultPalette(type: com.jiafenbu.androidpaint.model.PaletteType): Palette? {
        return getDefaultPalettes().find { it.type == type }
    }
    
    // ==================== 自定义色板 ====================
    
    /**
     * 获取所有自定义色板
     */
    fun getCustomPalettes(): List<Palette> = customPalettes.toList()
    
    /**
     * 创建新色板
     */
    fun createPalette(name: String, colors: List<PaletteColor> = emptyList()): Palette {
        val palette = Palette.create(
            name = name,
            type = com.jiafenbu.androidpaint.model.PaletteType.CUSTOM,
            colors = colors
        )
        customPalettes.add(palette)
        saveCustomPalettes()
        return palette
    }
    
    /**
     * 添加颜色到色板
     */
    fun addColorToPalette(paletteId: Long, color: PaletteColor): Boolean {
        val index = customPalettes.indexOfFirst { it.id == paletteId }
        if (index == -1) return false
        
        customPalettes[index] = customPalettes[index].addColor(color)
        saveCustomPalettes()
        return true
    }
    
    /**
     * 从色板移除颜色
     */
    fun removeColorFromPalette(paletteId: Long, colorIndex: Int): Boolean {
        val index = customPalettes.indexOfFirst { it.id == paletteId }
        if (index == -1) return false
        
        customPalettes[index] = customPalettes[index].removeColor(colorIndex)
        saveCustomPalettes()
        return true
    }
    
    /**
     * 更新色板中的颜色
     */
    fun updateColorInPalette(paletteId: Long, colorIndex: Int, color: PaletteColor): Boolean {
        val index = customPalettes.indexOfFirst { it.id == paletteId }
        if (index == -1) return false
        
        customPalettes[index] = customPalettes[index].updateColor(colorIndex, color)
        saveCustomPalettes()
        return true
    }
    
    /**
     * 重命名色板
     */
    fun renamePalette(paletteId: Long, newName: String): Boolean {
        val index = customPalettes.indexOfFirst { it.id == paletteId }
        if (index == -1) return false
        
        customPalettes[index] = customPalettes[index].copy(name = newName)
        saveCustomPalettes()
        return true
    }
    
    /**
     * 删除色板
     */
    fun deletePalette(paletteId: Long): Boolean {
        val removed = customPalettes.removeAll { it.id == paletteId }
        if (removed) saveCustomPalettes()
        return removed
    }
    
    /**
     * 复制色板
     */
    fun duplicatePalette(paletteId: Long): Palette? {
        val original = customPalettes.find { it.id == paletteId } ?: return null
        
        val copy = Palette.create(
            name = "${original.name} 副本",
            type = com.jiafenbu.androidpaint.model.PaletteType.CUSTOM,
            colors = original.colors
        )
        customPalettes.add(copy)
        saveCustomPalettes()
        return copy
    }
    
    // ==================== 持久化 ====================
    
    /**
     * 从 SharedPreferences 加载自定义色板
     */
    private fun loadCustomPalettes() {
        val json = prefs.getString(KEY_CUSTOM_PALETTES, null) ?: return
        
        try {
            val jsonArray = JSONArray(json)
            customPalettes.clear()
            
            for (i in 0 until jsonArray.length()) {
                val paletteJson = jsonArray.getJSONObject(i)
                val palette = parsePaletteFromJson(paletteJson)
                if (palette != null) {
                    customPalettes.add(palette)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 保存自定义色板到 SharedPreferences
     */
    private fun saveCustomPalettes() {
        val jsonArray = JSONArray()
        
        for (palette in customPalettes) {
            jsonArray.put(paletteToJson(palette))
        }
        
        prefs.edit().putString(KEY_CUSTOM_PALETTES, jsonArray.toString()).apply()
    }
    
    /**
     * 将色板序列化为 JSON
     */
    private fun paletteToJson(palette: Palette): JSONObject {
        val json = JSONObject()
        json.put("id", palette.id)
        json.put("name", palette.name)
        json.put("type", palette.type.name)
        json.put("isDefault", palette.isDefault)
        json.put("createdAt", palette.createdAt)
        
        val colorsArray = JSONArray()
        for (color in palette.colors) {
            val colorJson = JSONObject()
            colorJson.put("color", color.color)
            colorJson.put("name", color.name)
            colorsArray.put(colorJson)
        }
        json.put("colors", colorsArray)
        
        return json
    }
    
    /**
     * 从 JSON 反序列化色板
     */
    private fun parsePaletteFromJson(json: JSONObject): Palette? {
        return try {
            val colorsArray = json.getJSONArray("colors")
            val colors = mutableListOf<PaletteColor>()
            
            for (i in 0 until colorsArray.length()) {
                val colorJson = colorsArray.getJSONObject(i)
                colors.add(
                    PaletteColor(
                        color = colorJson.getInt("color"),
                        name = colorJson.optString("name", "")
                    )
                )
            }
            
            Palette(
                id = json.getLong("id"),
                name = json.getString("name"),
                type = com.jiafenbu.androidpaint.model.PaletteType.valueOf(
                    json.optString("type", "CUSTOM")
                ),
                colors = colors,
                isDefault = json.optBoolean("isDefault", false),
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // ==================== ASE 导入/导出 ====================
    
    /**
     * 从 ASE 文件导入色板
     */
    fun importFromAse(data: ByteArray): List<Palette> {
        val imported = AseFileParser.importAse(data)
        customPalettes.addAll(imported)
        saveCustomPalettes()
        return imported
    }
    
    /**
     * 导出色板为 ASE 文件
     */
    fun exportToAse(palette: Palette): ByteArray {
        return AseFileParser.exportAse(palette)
    }
    
    /**
     * 导出所有自定义色板为 ASE 文件
     */
    fun exportAllToAse(): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        AseFileParser.exportAllAse(customPalettes, outputStream)
        return outputStream.toByteArray()
    }
    
    // ==================== 常用颜色管理 ====================
    
    /**
     * 添加到最近使用颜色
     */
    fun addToRecentColors(color: Int) {
        val recent = getRecentColors().toMutableList()
        
        // 移除已存在的相同颜色
        recent.remove(color)
        
        // 添加到开头
        recent.add(0, color)
        
        // 限制数量
        while (recent.size > MAX_RECENT_COLORS) {
            recent.removeAt(recent.size - 1)
        }
        
        // 保存
        val colorsStr = recent.joinToString(",")
        prefs.edit().putString(KEY_RECENT_COLORS, colorsStr).apply()
    }
    
    /**
     * 获取最近使用颜色
     */
    fun getRecentColors(): List<Int> {
        val colorsStr = prefs.getString(KEY_RECENT_COLORS, null) ?: return emptyList()
        
        return try {
            colorsStr.split(",").mapNotNull { it.toIntOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 清除最近使用颜色
     */
    fun clearRecentColors() {
        prefs.edit().remove(KEY_RECENT_COLORS).apply()
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取所有色板（预设 + 自定义）
     */
    fun getAllPalettes(): List<Palette> {
        return getDefaultPalettes() + getCustomPalettes()
    }
    
    /**
     * 根据 ID 查找色板
     */
    fun findPaletteById(id: Long): Palette? {
        return getAllPalettes().find { it.id == id }
    }
    
    /**
     * 重置为默认设置
     */
    fun reset() {
        customPalettes.clear()
        prefs.edit()
            .remove(KEY_CUSTOM_PALETTES)
            .remove(KEY_RECENT_COLORS)
            .apply()
    }
    
    companion object {
        private const val PREFS_NAME = "palette_prefs"
        private const val KEY_CUSTOM_PALETTES = "custom_palettes"
        private const val KEY_RECENT_COLORS = "recent_colors"
        private const val MAX_RECENT_COLORS = 12
    }
}
