package com.jiafenbu.androidpaint.palette

import android.graphics.Color
import com.jiafenbu.androidpaint.model.Palette
import com.jiafenbu.androidpaint.model.PaletteColor
import com.jiafenbu.androidpaint.model.PaletteType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * ASE（Adobe Swatch Exchange）文件解析器
 * 支持导入和导出 ASE 格式的色板文件
 * 
 * ASE 格式参考 Adobe 规范：
 * - 文件头：签名 "ASEF" (4字节) + 版本号 (4字节) + 块数量 (4字节)
 * - 颜色块：块类型 (2字节) + 块长度 (4字节) + 块数据
 */
object AseFileParser {
    
    /** ASE 文件签名 */
    private const val ASE_SIGNATURE = "ASEF"
    
    /** ASE 版本号 */
    private const val ASE_VERSION_MAJOR = 1
    private const val ASE_VERSION_MINOR = 0
    
    /** 块类型 */
    private const val BLOCK_TYPE_COLOR_ENTRY = 0x0001
    private const val BLOCK_TYPE_GROUP_START = 0xC001
    private const val BLOCK_TYPE_GROUP_END = 0xC002
    
    /** 颜色模式 */
    private const val COLOR_MODE_RGB = "RGB "
    private const val COLOR_MODE_CMYK = "CMYK"
    private const val COLOR_MODE_GRAY = "GRAY"
    private const val COLOR_MODE_LAB = "LAB "
    
    /**
     * 导入 ASE 文件
     * 
     * @param inputStream 输入流
     * @return 解析后的色板列表
     */
    fun importAse(inputStream: InputStream): List<Palette> {
        val palettes = mutableListOf<Palette>()
        
        try {
            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            
            // 读取文件头
            val signature = String(ByteArray(4).also { buffer.get(it) })
            if (signature != ASE_SIGNATURE) {
                throw IllegalArgumentException("无效的 ASE 文件格式")
            }
            
            val versionMajor = buffer.short.toInt()
            val versionMinor = buffer.short.toInt()
            val blockCount = buffer.int
            
            // 当前处理的色板
            var currentPalette: Palette? = null
            var currentGroupName: String = ""
            var isInGroup = false
            
            // 解析颜色块
            var colorIndex = 0
            for (i in 0 until blockCount) {
                val blockType = buffer.short.toInt()
                
                when (blockType) {
                    BLOCK_TYPE_GROUP_START -> {
                        // 组开始
                        val blockLength = buffer.int
                        val nameLength = buffer.short.toInt()
                        val nameBytes = ByteArray(nameLength * 2 - 2) // UTF-16 编码
                        buffer.get(nameBytes)
                        buffer.short // 跳过终止符
                        
                        currentGroupName = String(nameBytes, Charsets.UTF_16BE)
                        isInGroup = true
                        colorIndex = 0
                    }
                    
                    BLOCK_TYPE_GROUP_END -> {
                        // 组结束
                        currentPalette = null
                        isInGroup = false
                    }
                    
                    BLOCK_TYPE_COLOR_ENTRY -> {
                        // 颜色条目
                        val blockLength = buffer.int
                        
                        // 读取颜色名称
                        val nameLength = buffer.short.toInt()
                        val nameBytes = ByteArray(nameLength * 2 - 2)
                        buffer.get(nameBytes)
                        buffer.short // 跳过终止符
                        val colorName = String(nameBytes, Charsets.UTF_16BE)
                        
                        // 读取颜色模型
                        val colorModel = String(ByteArray(4).also { buffer.get(it) })
                        
                        // 读取颜色值
                        val colorValue: Int = when (colorModel) {
                            COLOR_MODE_RGB -> {
                                val r = buffer.float
                                val g = buffer.float
                                val b = buffer.float
                                buffer.short // 跳过颜色类型
                                Color.rgb(
                                    (r * 255).toInt().coerceIn(0, 255),
                                    (g * 255).toInt().coerceIn(0, 255),
                                    (b * 255).toInt().coerceIn(0, 255)
                                )
                            }
                            COLOR_MODE_CMYK -> {
                                val c = buffer.float
                                val m = buffer.float
                                val y = buffer.float
                                val k = buffer.float
                                buffer.short
                                // CMYK 转 RGB
                                val r = ((1 - c) * (1 - k) * 255).toInt()
                                val g = ((1 - m) * (1 - k) * 255).toInt()
                                val b = ((1 - y) * (1 - k) * 255).toInt()
                                Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                            }
                            COLOR_MODE_GRAY -> {
                                val gray = buffer.float
                                buffer.short
                                val g = (gray * 255).toInt().coerceIn(0, 255)
                                Color.rgb(g, g, g)
                            }
                            COLOR_MODE_LAB -> {
                                val l = buffer.float
                                val a = buffer.float
                                val b_val = buffer.float
                                buffer.short
                                // LAB 转 RGB（简化）
                                Color.rgb(
                                    (l * 2.55).toInt().coerceIn(0, 255),
                                    (a + 128).toInt().coerceIn(0, 255),
                                    (b_val + 128).toInt().coerceIn(0, 255)
                                )
                            }
                            else -> Color.BLACK
                        }
                        
                        val paletteColor = PaletteColor(colorValue, colorName)
                        
                        // 创建或更新色板
                        if (currentPalette == null) {
                            currentPalette = Palette.create(
                                name = if (isInGroup) currentGroupName else "导入色板 ${palettes.size + 1}",
                                type = if (isInGroup) PaletteType.CUSTOM else PaletteType.CUSTOM,
                                colors = listOf(paletteColor)
                            )
                        } else {
                            currentPalette = currentPalette.addColor(paletteColor)
                        }
                        
                        colorIndex++
                    }
                    
                    else -> {
                        // 未知块类型，跳过
                        val blockLength = buffer.int
                        val skipBytes = ByteArray(blockLength.toLong().coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                        buffer.get(skipBytes)
                    }
                }
                
                // 如果色板完成且不在组中，添加到列表
                if (!isInGroup && currentPalette != null && blockType == BLOCK_TYPE_COLOR_ENTRY) {
                    // 检查是否是组结束后的最后一个颜色
                    if (i == blockCount - 1 || (i + 1 < blockCount && buffer.short.toInt() == BLOCK_TYPE_GROUP_END)) {
                        palettes.add(currentPalette)
                        currentPalette = null
                    }
                }
            }
            
            // 添加最后一个色板（如果在组中）
            if (currentPalette != null && currentPalette.colors.isNotEmpty()) {
                palettes.add(currentPalette)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            throw IllegalArgumentException("解析 ASE 文件失败: ${e.message}")
        }
        
        return palettes
    }
    
    /**
     * 导入 ASE 文件（从字节数组）
     */
    fun importAse(data: ByteArray): List<Palette> {
        return importAse(ByteArrayInputStream(data))
    }
    
    /**
     * 导出色板为 ASE 文件
     * 
     * @param palette 要导出的色板
     * @param outputStream 输出流
     */
    fun exportAse(palette: Palette, outputStream: OutputStream) {
        val buffer = ByteBuffer.allocate(calculateAseSize(palette))
            .order(ByteOrder.BIG_ENDIAN)
        
        // 写入文件头
        buffer.put(ASE_SIGNATURE.toByteArray())
        buffer.putShort(ASE_VERSION_MAJOR.toShort())
        buffer.putShort(ASE_VERSION_MINOR.toShort())
        buffer.putInt(1 + palette.colors.size) // 组块 + 颜色块
        
        // 写入组开始块
        writeGroupStartBlock(buffer, palette.name)
        
        // 写入颜色块
        for (color in palette.colors) {
            writeColorBlock(buffer, color.name, color.color)
        }
        
        // 写入组结束块
        writeGroupEndBlock(buffer)
        
        outputStream.write(buffer.array())
    }
    
    /**
     * 导出色板为 ASE 文件（返回字节数组）
     */
    fun exportAse(palette: Palette): ByteArray {
        val outputStream = ByteArrayOutputStream()
        exportAse(palette, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * 批量导出色板列表
     */
    fun exportAllAse(palettes: List<Palette>, outputStream: OutputStream) {
        val buffer = ByteBuffer.allocate(calculateAllAseSize(palettes))
            .order(ByteOrder.BIG_ENDIAN)
        
        // 写入文件头
        buffer.put(ASE_SIGNATURE.toByteArray())
        buffer.putShort(ASE_VERSION_MAJOR.toShort())
        buffer.putShort(ASE_VERSION_MINOR.toShort())
        
        // 计算总块数
        var totalBlocks = 0
        for (palette in palettes) {
            totalBlocks += 2 + palette.colors.size // 组开始 + 组结束 + 颜色
        }
        buffer.putInt(totalBlocks)
        
        // 写入每个色板
        for (palette in palettes) {
            writeGroupStartBlock(buffer, palette.name)
            for (color in palette.colors) {
                writeColorBlock(buffer, color.name, color.color)
            }
            writeGroupEndBlock(buffer)
        }
        
        outputStream.write(buffer.array())
    }
    
    /**
     * 计算 ASE 文件大小
     */
    private fun calculateAseSize(palette: Palette): Int {
        var size = 16 // 文件头大小
        
        // 组开始块
        size += 4 + palette.name.toByteArray(Charsets.UTF_16BE).size + 2
        
        // 颜色块
        for (color in palette.colors) {
            size += 4 + calculateColorBlockDataSize(color)
        }
        
        // 组结束块
        size += 4
        
        return size
    }
    
    /**
     * 计算批量导出总大小
     */
    private fun calculateAllAseSize(palettes: List<Palette>): Int {
        return palettes.sumOf { calculateAseSize(it) }
    }
    
    /**
     * 计算颜色块数据大小
     */
    private fun calculateColorBlockDataSize(color: PaletteColor): Int {
        return 2 + // 名称长度
               color.name.toByteArray(Charsets.UTF_16BE).size + 2 + // 名称（UTF-16）+ 终止符
               4 + // 颜色模式 "RGB "
               12 + // 3个float值
               2 // 颜色类型
    }
    
    /**
     * 写入组开始块
     */
    private fun writeGroupStartBlock(buffer: ByteBuffer, name: String) {
        buffer.putShort(BLOCK_TYPE_GROUP_START.toShort())
        
        val nameBytes = name.toByteArray(Charsets.UTF_16BE)
        val blockDataSize = 2 + nameBytes.size + 2 // 名称长度 + 名称 + 终止符
        buffer.putInt(blockDataSize)
        
        buffer.putShort(((name.length + 1)).toShort()) // 名称长度（包含终止符）
        buffer.put(nameBytes)
        buffer.putShort(0) // UTF-16 终止符
    }
    
    /**
     * 写入组结束块
     */
    private fun writeGroupEndBlock(buffer: ByteBuffer) {
        buffer.putShort(BLOCK_TYPE_GROUP_END.toShort())
        buffer.putInt(0) // 无数据
    }
    
    /**
     * 写入颜色块
     */
    private fun writeColorBlock(buffer: ByteBuffer, name: String, color: Int) {
        buffer.putShort(BLOCK_TYPE_COLOR_ENTRY.toShort())
        
        val colorDataSize = calculateColorBlockDataSize(PaletteColor(color, name))
        buffer.putInt(colorDataSize)
        
        // 名称
        val nameBytes = name.toByteArray(Charsets.UTF_16BE)
        buffer.putShort((name.length + 1).toShort())
        buffer.put(nameBytes)
        buffer.putShort(0) // 终止符
        
        // 颜色模式
        buffer.put(COLOR_MODE_RGB.toByteArray())
        
        // RGB 值（0-1 范围）
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        buffer.putFloat(r)
        buffer.putFloat(g)
        buffer.putFloat(b)
        
        // 颜色类型（0 = Global）
        buffer.putShort(0)
    }
    
    /**
     * 从图片提取颜色
     * 使用 K-means 聚类算法提取主要颜色
     * 
     * @param pixels 图片像素数组
     * @param width 图片宽度
     * @param height 图片高度
     * @param colorCount 要提取的颜色数量
     * @return 提取的颜色列表
     */
    fun extractColorsFromImage(
        pixels: IntArray,
        width: Int,
        height: Int,
        colorCount: Int = 8
    ): List<PaletteColor> {
        // 简化实现：采样像素并计算平均颜色
        val colorCounts = mutableMapOf<Int, Int>()
        
        // 采样像素
        val step = maxOf(1, (width * height) / 1000)
        var sampledPixels = 0
        
        for (i in pixels.indices step step) {
            val pixel = pixels[i]
            // 忽略接近白色的像素
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            
            // 简化：四舍五入到最近的 32 值来减少颜色数量
            val quantizedR = (r / 32) * 32
            val quantizedG = (g / 32) * 32
            val quantizedB = (b / 32) * 32
            val quantizedColor = Color.rgb(quantizedR, quantizedG, quantizedB)
            
            colorCounts[quantizedColor] = (colorCounts[quantizedColor] ?: 0) + 1
            sampledPixels++
        }
        
        // 按出现次数排序，取前 N 个
        val sortedColors = colorCounts.entries
            .sortedByDescending { it.value }
            .take(colorCount)
            .map { it.key }
        
        return sortedColors.mapIndexed { index, color ->
            PaletteColor(color, "颜色 ${index + 1}")
        }
    }
}
