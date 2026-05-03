package com.jiafenbu.androidpaint.font

import android.content.Context
import android.graphics.Typeface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 字体管理器
 * 负责管理系统字体和自定义字体的加载
 *
 * 功能：
 * - 枚举系统内置字体
 * - 导入自定义字体（TTF/OTF）
 * - 管理自定义字体文件
 */
class FontManager(private val context: Context) {

    companion object {
        private const val FONTS_DIR = "fonts"

        /**
         * 系统内置字体列表
         */
        val SYSTEM_FONTS = listOf(
            SystemFont("默认", null),
            SystemFont("宋体", "sans-serif"),
            SystemFont("黑体", "sans-serif-black"),
            SystemFont("楷体", "casual"),
            SystemFont("等线", "monospace"),
            SystemFont("衬线", "serif"),
            SystemFont("无衬线", "sans-serif"),
            SystemFont("手写", "cursive"),
            SystemFont("等宽", "monospace")
        )
    }

    /**
     * 系统字体数据类
     *
     * @param displayName 显示名称
     * @param fontFamily Typeface 字体名称
     */
    data class SystemFont(
        val displayName: String,
        val fontFamily: String?
    ) {
        /**
         * 获取 Typeface 对象
         */
        fun toTypeface(style: Int = Typeface.NORMAL): Typeface {
            return if (fontFamily != null) {
                Typeface.create(fontFamily, style)
            } else {
                Typeface.create(Typeface.DEFAULT, style)
            }
        }
    }

    /**
     * 自定义字体数据类
     *
     * @param name 字体名称
     * @param filePath 文件路径
     * @param typeface Typeface 对象
     */
    data class CustomFont(
        val name: String,
        val filePath: String,
        val typeface: Typeface
    )

    /**
     * 自定义字体列表
     */
    private val _customFonts = mutableListOf<CustomFont>()
    val customFonts: List<CustomFont> get() = _customFonts.toList()

    /**
     * 字体目录
     */
    private val fontsDir: File by lazy {
        File(context.filesDir, FONTS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    init {
        // 加载已有的自定义字体
        loadCustomFonts()
    }

    /**
     * 加载自定义字体目录中的所有字体
     */
    private fun loadCustomFonts() {
        _customFonts.clear()
        fontsDir.listFiles()?.filter { it.extension in listOf("ttf", "otf") }?.forEach { file ->
            try {
                val typeface = Typeface.createFromFile(file)
                val font = CustomFont(
                    name = file.nameWithoutExtension,
                    filePath = file.absolutePath,
                    typeface = typeface
                )
                _customFonts.add(font)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 导入自定义字体文件
     *
     * @param inputStream 输入流
     * @param fileName 文件名
     * @return 导入是否成功
     */
    fun importFont(inputStream: InputStream, fileName: String): Boolean {
        return try {
            val outputFile = File(fontsDir, fileName)
            FileOutputStream(outputFile).use { fos ->
                inputStream.copyTo(fos)
            }

            // 加载新字体
            val typeface = Typeface.createFromFile(outputFile)
            val font = CustomFont(
                name = outputFile.nameWithoutExtension,
                filePath = outputFile.absolutePath,
                typeface = typeface
            )
            _customFonts.add(font)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 删除自定义字体
     *
     * @param filePath 字体文件路径
     * @return 删除是否成功
     */
    fun deleteFont(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                _customFonts.removeAll { it.filePath == filePath }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取所有可用字体（系统 + 自定义）
     */
    fun getAllFonts(): List<FontItem> {
        val fonts = mutableListOf<FontItem>()

        // 添加系统字体
        SYSTEM_FONTS.forEach { font ->
            fonts.add(FontItem(
                name = font.displayName,
                displayName = font.displayName,
                filePath = null,
                typeface = font.toTypeface()
            ))
        }

        // 添加自定义字体
        _customFonts.forEach { font ->
            fonts.add(FontItem(
                name = font.name,
                displayName = font.name,
                filePath = font.filePath,
                typeface = font.typeface
            ))
        }

        return fonts
    }

    /**
     * 根据名称获取字体
     *
     * @param name 字体名称
     * @param style 字体样式
     * @return Typeface 对象
     */
    fun getTypeface(name: String, style: Int = Typeface.NORMAL): Typeface {
        // 先查找系统字体
        SYSTEM_FONTS.find { it.displayName == name }?.let {
            return it.toTypeface(style)
        }

        // 再查找自定义字体
        _customFonts.find { it.name == name }?.let {
            return it.typeface
        }

        // 返回默认字体
        return Typeface.create(Typeface.DEFAULT, style)
    }

    /**
     * 根据名称和文件路径获取字体
     *
     * @param name 字体名称
     * @param filePath 文件路径（可选）
     * @param style 字体样式
     * @return Typeface 对象
     */
    fun getTypefaceByName(name: String, filePath: String?, style: Int = Typeface.NORMAL): Typeface {
        return if (!filePath.isNullOrEmpty()) {
            try {
                Typeface.createFromFile(filePath)
            } catch (e: Exception) {
                getTypeface(name, style)
            }
        } else {
            getTypeface(name, style)
        }
    }
}

/**
 * 字体项数据类
 */
data class FontItem(
    val name: String,
    val displayName: String,
    val filePath: String?,
    val typeface: Typeface
)
