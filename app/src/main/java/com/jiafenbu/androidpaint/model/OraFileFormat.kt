package com.jiafenbu.androidpaint.model

import android.graphics.Bitmap
import com.jiafenbu.androidpaint.model.BlendMode
import com.jiafenbu.androidpaint.model.LayerType
import com.jiafenbu.androidpaint.model.TextLayerModel
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.min

/**
 * ORA (OpenRaster) 格式文件读写器
 * 
 * ORA 是开源绘画软件的通用格式，基于 ZIP 压缩的 XML + PNG 结构
 * 文件结构：
 * - mimetype: 固定内容 "image/openraster"
 * - mergedimage.png: 合并后的完整图像
 * - stack.xml: 图层堆栈描述
 * - data/layerN.png: 各图层PNG图像
 * - Thumbnails/thumbnail.png: 缩略图 (256x256)
 */
object OraFileFormat {
    
    private const val MIMETYPE = "image/openraster"
    private const val STACK_XML = "stack.xml"
    private const val MERGED_IMAGE = "mergedimage.png"
    private const val THUMBNAIL_PATH = "Thumbnails/thumbnail.png"
    private const val THUMBNAIL_SIZE = 256
    
    /**
     * ORA 项目数据结构
     */
    data class OraProjectData(
        val width: Int,
        val height: Int,
        val layers: List<OraLayerData>,
        val mergedImage: Bitmap?,
        val thumbnail: Bitmap?
    )
    
    /**
     * ORA 图层数据
     */
    data class OraLayerData(
        val name: String,
        val opacity: Float,
        val blendMode: BlendMode,
        val isVisible: Boolean,
        val imageData: Bitmap?,
        // 阶段7新增
        val layerType: LayerType = LayerType.NORMAL,
        val textLayerModel: TextLayerModel? = null
    )
    
    /**
     * 保存项目为 ORA 格式
     * 
     * @param layerModels 图层模型列表
     * @param layerBitmaps 图层位图映射
     * @param canvasWidth 画布宽度
     * @param canvasHeight 画布高度
     * @param outputStream 输出流
     * @param mergedBitmap 合并后的完整图像（可选）
     */
    fun saveProject(
        layerModels: List<LayerModel>,
        layerBitmaps: Map<Long, Bitmap>,
        canvasWidth: Int,
        canvasHeight: Int,
        outputStream: OutputStream,
        mergedBitmap: Bitmap? = null
    ) {
        ZipOutputStream(outputStream).use { zipOut ->
            // 1. 首先写入 mimetype（必须在第一条，不压缩）
            val mimetypeEntry = ZipEntry("mimetype")
            mimetypeEntry.method = ZipEntry.STORED
            zipOut.putNextEntry(mimetypeEntry)
            zipOut.write(MIMETYPE.toByteArray(Charsets.US_ASCII))
            zipOut.closeEntry()
            
            // 2. 生成并写入 stack.xml
            val stackXml = generateStackXml(layerModels, layerBitmaps)
            val stackEntry = ZipEntry(STACK_XML)
            zipOut.putNextEntry(stackEntry)
            zipOut.write(stackXml.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
            
            // 3. 写入 data/ 目录下的各图层 PNG
            layerModels.forEachIndexed { index, layer ->
                val bitmap = layerBitmaps[layer.id]
                if (bitmap != null && !bitmap.isRecycled) {
                    val layerEntry = ZipEntry("data/layer$index.png")
                    zipOut.putNextEntry(layerEntry)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                    zipOut.closeEntry()
                }
            }
            
            // 4. 写入合并图像（如果提供）
            if (mergedBitmap != null && !mergedBitmap.isRecycled) {
                val mergedEntry = ZipEntry(MERGED_IMAGE)
                zipOut.putNextEntry(mergedEntry)
                mergedBitmap.compress(Bitmap.CompressFormat.PNG, 100, zipOut)
                zipOut.closeEntry()
            }
            
            // 5. 生成并写入缩略图
            val thumbnail = generateThumbnail(mergedBitmap ?: layerBitmaps.values.firstOrNull(), canvasWidth, canvasHeight)
            if (thumbnail != null && !thumbnail.isRecycled) {
                val thumbEntry = ZipEntry(THUMBNAIL_PATH)
                zipOut.putNextEntry(thumbEntry)
                thumbnail.compress(Bitmap.CompressFormat.PNG, 90, zipOut)
                zipOut.closeEntry()
                thumbnail.recycle()
            }
        }
    }
    
    /**
     * 从 ORA 文件加载项目
     * 
     * @param inputStream 输入流
     * @return ORA 项目数据
     */
    fun loadProject(inputStream: InputStream): OraProjectData {
        val layerDataList = mutableListOf<OraLayerData>()
        var width = 0
        var height = 0
        var mergedImage: Bitmap? = null
        var thumbnail: Bitmap? = null
        
        ZipInputStream(inputStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                when {
                    entry.name == STACK_XML -> {
                        // 解析 stack.xml
                        val xmlContent = zipIn.bufferedReader().readText()
                        val parsed = parseStackXml(xmlContent)
                        width = parsed.first
                        height = parsed.second
                        // 先收集图层信息，位图稍后从 data/ 目录读取
                        layerDataList.addAll(parsed.third)
                    }
                    entry.name == MERGED_IMAGE -> {
                        mergedImage = android.graphics.BitmapFactory.decodeStream(zipIn)
                    }
                    entry.name == THUMBNAIL_PATH -> {
                        thumbnail = android.graphics.BitmapFactory.decodeStream(zipIn)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        
        return OraProjectData(
            width = width,
            height = height,
            layers = layerDataList,
            mergedImage = mergedImage,
            thumbnail = thumbnail
        )
    }
    
    /**
     * 加载项目并同时读取图层位图
     * 这是完整加载，包含所有图层 PNG
     */
    fun loadProjectComplete(inputStream: InputStream): OraProjectDataWithBitmaps {
        val layerDataList = mutableListOf<OraLayerData>()
        var width = 0
        var height = 0
        var mergedImage: Bitmap? = null
        var thumbnail: Bitmap? = null
        val layerBitmaps = mutableMapOf<String, Bitmap>()
        
        // 先收集所有数据到内存
        val zipData = mutableMapOf<String, ByteArray>()
        
        ZipInputStream(inputStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val baos = ByteArrayOutputStream()
                zipIn.copyTo(baos)
                zipData[entry.name] = baos.toByteArray()
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        
        // 解析 stack.xml
        zipData[STACK_XML]?.let { xmlBytes ->
            val xmlContent = String(xmlBytes, Charsets.UTF_8)
            val parsed = parseStackXml(xmlContent)
            width = parsed.first
            height = parsed.second
            layerDataList.addAll(parsed.third)
        }
        
        // 加载合并图像
        zipData[MERGED_IMAGE]?.let { bytes ->
            mergedImage = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        
        // 加载缩略图
        zipData[THUMBNAIL_PATH]?.let { bytes ->
            thumbnail = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        
        // 加载各图层位图
        layerDataList.forEachIndexed { index, layerData ->
            val key = "data/layer$index.png"
            zipData[key]?.let { bytes ->
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                layerBitmaps[key] = bitmap
            }
        }
        
        return OraProjectDataWithBitmaps(
            width = width,
            height = height,
            layers = layerDataList,
            layerBitmaps = layerBitmaps,
            mergedImage = mergedImage,
            thumbnail = thumbnail
        )
    }
    
    /**
     * 完整的 ORA 项目数据（含位图）
     */
    data class OraProjectDataWithBitmaps(
        val width: Int,
        val height: Int,
        val layers: List<OraLayerData>,
        val layerBitmaps: Map<String, Bitmap>,
        val mergedImage: Bitmap?,
        val thumbnail: Bitmap?
    )
    
    /**
     * 生成 stack.xml 内容
     */
    private fun generateStackXml(
        layerModels: List<LayerModel>,
        layerBitmaps: Map<Long, Bitmap>
    ): String {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        doc.xmlVersion = "1.0"
        
        // 创建根元素 <image>
        val imageElement = doc.createElement("image").apply {
            setAttribute("w", layerModels.firstOrNull()?.let { 
                layerBitmaps[it.id]?.width?.toString() 
            } ?: "1080")
            setAttribute("h", layerModels.firstOrNull()?.let { 
                layerBitmaps[it.id]?.height?.toString() 
            } ?: "1920")
        }
        doc.appendChild(imageElement)
        
        // 创建 <stack> 元素
        val stackElement = doc.createElement("stack")
        imageElement.appendChild(stackElement)
        
        // 按倒序添加图层（ORA 中最上面的图层在 XML 最前面）
        layerModels.reversed().forEachIndexed { xmlIndex, layer ->
            val layerElement = doc.createElement("layer").apply {
                setAttribute("name", layer.name)
                setAttribute("src", "data/layer$xmlIndex.png")
                setAttribute("opacity", layer.opacity.toString())
                setAttribute("composite-op", toSvgCompositeOp(layer.blendMode))
                setAttribute("visibility", if (layer.isVisible) "visible" else "hidden")
                // 阶段7新增：保存图层类型
                setAttribute("layer-type", layer.layerType.name)
                // 如果是文字图层，保存文字属性 JSON
                if (layer.layerType == LayerType.TEXT && layer.textLayerModel != null) {
                    setAttribute("text-props", layer.textLayerModel!!.toJson())
                }
            }
            stackElement.appendChild(layerElement)
        }
        
        // 转换为字符串
        val transformer = TransformerFactory.newInstance().newTransformer()
        val sw = ByteArrayOutputStream()
        transformer.transform(DOMSource(doc), StreamResult(sw))
        return sw.toString()
    }
    
    /**
     * 解析 stack.xml 内容
     * @return Triple(宽度, 高度, 图层数据列表)
     */
    private fun parseStackXml(xmlContent: String): Triple<Int, Int, List<OraLayerData>> {
        val layers = mutableListOf<OraLayerData>()
        var width = 1080
        var height = 1920
        
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                xmlContent.byteInputStream(Charsets.UTF_8)
            )
            doc.documentElement.normalize()
            
            // 解析 image 属性
            val imageElement = doc.documentElement
            width = imageElement.getAttribute("w").toIntOrNull() ?: 1080
            height = imageElement.getAttribute("h").toIntOrNull() ?: 1920
            
            // 查找 stack 元素
            val stackList = doc.getElementsByTagName("stack")
            if (stackList.length > 0) {
                val stackElement = stackList.item(0) as Element
                val layerList = stackElement.getElementsByTagName("layer")
                
                for (i in 0 until layerList.length) {
                    val layerElement = layerList.item(i) as Element
                    val name = layerElement.getAttribute("name")
                    val opacity = layerElement.getAttribute("opacity").toFloatOrNull() ?: 1f
                    val compositeOp = layerElement.getAttribute("composite-op")
                    val visibility = layerElement.getAttribute("visibility")
                    val src = layerElement.getAttribute("src")
                    
                    // 阶段7新增：解析图层类型
                    val layerTypeStr = layerElement.getAttribute("layer-type")
                    val layerType = try {
                        if (layerTypeStr.isNotEmpty()) LayerType.valueOf(layerTypeStr) else LayerType.NORMAL
                    } catch (e: Exception) {
                        LayerType.NORMAL
                    }
                    
                    // 解析文字图层属性
                    val textPropsStr = layerElement.getAttribute("text-props")
                    val textLayerModel = try {
                        if (textPropsStr.isNotEmpty()) TextLayerModel.fromJson(textPropsStr) else null
                    } catch (e: Exception) {
                        null
                    }
                    
                    layers.add(OraLayerData(
                        name = name,
                        opacity = opacity,
                        blendMode = fromSvgCompositeOp(compositeOp),
                        isVisible = visibility != "hidden",
                        imageData = null, // 位图稍后单独加载
                        layerType = layerType,
                        textLayerModel = textLayerModel
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return Triple(width, height, layers)
    }
    
    /**
     * 生成缩略图
     */
    private fun generateThumbnail(source: Bitmap?, canvasWidth: Int, canvasHeight: Int): Bitmap? {
        if (source == null || source.isRecycled) return null
        
        // 计算缩放比例，保持宽高比
        val scale = min(
            THUMBNAIL_SIZE.toFloat() / canvasWidth,
            THUMBNAIL_SIZE.toFloat() / canvasHeight
        )
        
        val thumbWidth = (canvasWidth * scale).toInt()
        val thumbHeight = (canvasHeight * scale).toInt()
        
        return Bitmap.createScaledBitmap(source, thumbWidth, thumbHeight, true)
    }
    
    /**
     * 将 BlendMode 转换为 SVG composite-op 字符串
     */
    fun toSvgCompositeOp(blendMode: BlendMode): String = when (blendMode) {
        BlendMode.NORMAL -> "svg:src-over"
        BlendMode.MULTIPLY -> "svg:multiply"
        BlendMode.SCREEN -> "svg:screen"
        BlendMode.OVERLAY -> "svg:overlay"
        BlendMode.SOFT_LIGHT -> "svg:soft-light"
        BlendMode.HARD_LIGHT -> "svg:hard-light"
        BlendMode.COLOR_DODGE -> "svg:color-dodge"
        BlendMode.COLOR_BURN -> "svg:color-burn"
        BlendMode.DARKEN -> "svg:darken"
        BlendMode.LIGHTEN -> "svg:lighten"
        BlendMode.DIFFERENCE -> "svg:difference"
        BlendMode.HUE -> "svg:hue"
        BlendMode.SATURATION -> "svg:saturation"
        BlendMode.COLOR -> "svg:color"
        BlendMode.LUMINOSITY -> "svg:luminosity"
        BlendMode.SRC_OVER -> "svg:src-over"
    }
    
    /**
     * 从 SVG composite-op 字符串转换为 BlendMode
     */
    fun fromSvgCompositeOp(op: String): BlendMode = when (op) {
        "svg:multiply" -> BlendMode.MULTIPLY
        "svg:screen" -> BlendMode.SCREEN
        "svg:overlay" -> BlendMode.OVERLAY
        "svg:soft-light" -> BlendMode.SOFT_LIGHT
        "svg:hard-light" -> BlendMode.HARD_LIGHT
        "svg:color-dodge" -> BlendMode.COLOR_DODGE
        "svg:color-burn" -> BlendMode.COLOR_BURN
        "svg:darken" -> BlendMode.DARKEN
        "svg:lighten" -> BlendMode.LIGHTEN
        "svg:difference" -> BlendMode.DIFFERENCE
        "svg:hue" -> BlendMode.HUE
        "svg:saturation" -> BlendMode.SATURATION
        "svg:color" -> BlendMode.COLOR
        "svg:luminosity" -> BlendMode.LUMINOSITY
        "svg:src-over", "svg:plus", "" -> BlendMode.NORMAL
        else -> BlendMode.NORMAL
    }
}
