package com.anifichadia.figmaimporter.model.importing

import com.sksamuel.scrimage.AwtImage
import com.sksamuel.scrimage.metadata.ImageMetadata
import com.sksamuel.scrimage.nio.ImageWriter
import java.io.IOException
import java.io.OutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriteParam

/**
 * [com.sksamuel.scrimage.nio.PngWriter] does not have a quality param. This is a copy pasted version
 * with internal rework to set [ImageWriteParam.setCompressionQuality].
 */
internal class PngWriterWithCompression(private val qualityPercent: Int) : ImageWriter {
    @Throws(IOException::class)
    override fun write(image: AwtImage, metadata: ImageMetadata?, out: OutputStream?) {
        val type = ImageTypeSpecifier.createFromBufferedImageType(image.type)
        val writer = ImageIO.getImageWriters(type, "png").next()
        val param = writer.defaultWriteParam
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
            param.setCompressionQuality(qualityPercent / 100f)
        }
        val ios = ImageIO.createImageOutputStream(out)
        writer.setOutput(ios)
        writer.write(null, IIOImage(image.awt(), null, null), param)
        writer.dispose()
        ios.close()
    }
}
