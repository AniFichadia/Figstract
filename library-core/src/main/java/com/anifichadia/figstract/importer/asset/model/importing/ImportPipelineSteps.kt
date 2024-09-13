package com.anifichadia.figstract.importer.asset.model.importing

import com.anifichadia.figstract.figma.model.ExportSetting
import com.anifichadia.figstract.figma.model.ExportSetting.Format.JPG
import com.anifichadia.figstract.figma.model.ExportSetting.Format.PDF
import com.anifichadia.figstract.figma.model.ExportSetting.Format.PNG
import com.anifichadia.figstract.figma.model.ExportSetting.Format.SVG
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.passThrough
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.webp.WebpWriter

//region Transforms

//region Scaling
/** @see [transform] */
fun scale(scale: Float): ImportPipeline.Step {
    return if (scale != 1f) {
        transform("scale(scale: $scale)") { it.scale(scale.toDouble()) }
    } else {
        passThrough()
    }
}

/** @see [transform] */
fun scaleToSize(width: Int, height: Int) =
    transform("scaleToSize(width: $width, height: $height)") { it.scaleTo(width, height) }

/** @see [transform] */
fun scaleToWidth(width: Int) = transform("scaleToWidth(width: $width)") { it.scaleToWidth(width) }

/** @see [transform] */
fun scaleToHeight(height: Int) = transform("scaleToHeight(height: $height)") { it.scaleToHeight(height) }
//endregion

/**
 * Note: this only currently supports JPEG and PNGs
 */
private fun transform(
    description: String,
    transform: (original: ImmutableImage) -> ImmutableImage,
) = ImportPipeline.Step(description) { instruction, input ->
    val format = input.target.format
    val imageFormat = ExportSetting.Format.fromFileExtensionOrNull(format) ?: instruction.export.config.format

    val writer = when (imageFormat) {
        JPG -> JpegWriter.Default
        PNG -> PngWriter.NoCompression
        SVG,
        PDF,
        -> throw IllegalStateException("Format $imageFormat is unsupported")
    }

    val data = input.data
    val image = ImmutableImage.loader().fromBytes(data)
    val transformedImage = transform(image)
    val transformedImageBytes = transformedImage.bytes(writer)

    input
        .copy(
            data = transformedImageBytes,
            target = input.target.copy(
                format = imageFormat.fileExtension,
            ),
        )
        .single()
}
//endregion

//region Naming
fun rename(name: String) = ImportPipeline.Step("rename(name: $name)") { _, input ->
    input
        .copy(
            target = input.target.copy(
                outputName = name,
            )
        )
        .single()
}

fun rename(block: (String) -> String) = ImportPipeline.Step("rename(block: $block)") { instruction, input ->
    val resolveOutputName = resolveOutputName(instruction, input)

    input
        .copy(
            target = input.target.copy(
                outputName = block(resolveOutputName),
            )
        )
        .single()
}

fun renameSuffix(suffix: String) = ImportPipeline.Step("renameSuffix(suffix: $suffix)") { instruction, input ->
    val resolveOutputName = resolveOutputName(instruction, input)

    input
        .copy(
            target = input.target.copy(
                outputName = "$resolveOutputName$suffix",
            )
        )
        .single()
}

fun renamePrefix(prefix: String) = ImportPipeline.Step("renamePrefix(prefix: $prefix)") { instruction, input ->
    val resolveOutputName = resolveOutputName(instruction, input)

    input
        .copy(
            target = input.target.copy(
                outputName = "$prefix$resolveOutputName",
            )
        )
        .single()
}
//endregion

//region Path
fun pathElementsAppend(vararg pathElement: String) = pathElementsAppend(pathElement.toList())

fun pathElementsAppend(pathElements: List<String>) =
    ImportPipeline.Step("pathElementsAppend(pathElements: $pathElements)") { _, input ->
        input
            .copy(
                target = input.target.copy(
                    pathElements = (input.target.pathElements + pathElements),
                )
            )
            .single()
    }
//endregion

//region PNG compression
val convertToPngLossless = convertToFormat("convertToPngLossless()", "png", PngWriter.NoCompression)

/**
 * @param qualityPercent Must be between 0 and 100
 */
private fun convertToPngLossy(qualityPercent: Int = 75): ImportPipeline.Step {
    require(qualityPercent in (0..100)) { "qualityPercent must be between 0 and 100" }

    return convertToFormat(
        "convertToPngLossy(qualityPercent: $qualityPercent)",
        "png",
        PngWriterWithCompression(qualityPercent),
    )
}
//endregion

//region WebP conversion
val convertToWebPLossless = convertToWebP("convertToWebPLossless()", WebpWriter.MAX_LOSSLESS_COMPRESSION)

/**
 * @param qualityPercent Must be between 0 and 100
 */
fun convertToWebPLossy(qualityPercent: Int = 75): ImportPipeline.Step {
    require(qualityPercent in (0..100)) { "qualityPercent must be between 0 and 100" }

    return convertToWebP(
        "convertToWebPLossy(qualityPercent: $qualityPercent)",
        WebpWriter.DEFAULT.withQ(qualityPercent).withMultiThread(),
    )
}

private fun convertToWebP(description: String, writer: WebpWriter) =
    convertToFormat(description, "webp", writer)

private fun convertToFormat(description: String, format: String?, writer: ImageWriter) =
    ImportPipeline.Step(description) { _, input ->
        val image = ImmutableImage.loader().fromBytes(input.data)
        val convertedImageBytes = image.bytes(writer)

        input
            .copy(
                data = convertedImageBytes,
                target = input.target.copy(
                    format = format ?: input.target.format,
                ),
            )
            .single()
    }
//endregion
