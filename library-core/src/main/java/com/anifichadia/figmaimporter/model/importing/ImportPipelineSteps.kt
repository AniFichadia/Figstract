package com.anifichadia.figmaimporter.model.importing

import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.passthrough
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.sksamuel.scrimage.ImmutableImage
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
        passthrough()
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
    val data = input.data
    val format = input.target.format

    val loader = ImmutableImage.loader()
    val image = loader.fromBytes(data)
    val resized = transform(image)

    val imageFormat =
        ExportSetting.Format.fromFileExtensionOrNull(format) ?: instruction.export.config.format

    when (imageFormat) {
        ExportSetting.Format.JPG -> {
            val resizedBytes = resized.bytes(JpegWriter.Default)
            input
                .copy(
                    data = resizedBytes,
                    target = input.target.copy(
                        format = imageFormat.fileExtension,
                    ),
                )
                .single()
        }

        ExportSetting.Format.PNG -> {
            val resizedBytes = resized.bytes(PngWriter.NoCompression)
            input
                .copy(
                    data = resizedBytes,
                    target = input.target.copy(
                        format = imageFormat.fileExtension,
                    ),
                )
                .single()
        }

        else -> {
            throw IllegalStateException("Format $imageFormat is unsupported")
        }
    }
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

//region WebP conversion
val convertToWebPLossless = convertToWebP("convertToWebPLossless()", WebpWriter.MAX_LOSSLESS_COMPRESSION)

/**
 * @param qualityPercent Must be between 0 and 100
 */
fun convertToWebPLossy(qualityPercent: Int = 75): ImportPipeline.Step {
    require(qualityPercent in (0..100)) { "qualityPercent must be between 0 and 100" }

    return convertToWebP(
        "convertToWebPLossy(qualityPercent: $qualityPercent)",
        WebpWriter.DEFAULT.withQ(qualityPercent)
    )
}

private fun convertToWebP(description: String, writer: WebpWriter) = ImportPipeline.Step(description) { _, input ->
    val image = ImmutableImage.loader().fromBytes(input.data)
    val convertedImageBytes = image.bytes(writer)

    input
        .copy(
            data = convertedImageBytes,
            target = input.target.copy(
                format = "webp",
            ),
        )
        .single()
}
//endregion
