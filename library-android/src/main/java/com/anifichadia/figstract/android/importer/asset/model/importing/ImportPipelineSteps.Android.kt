package com.anifichadia.figstract.android.importer.asset.model.importing

import com.android.ide.common.vectordrawable.Svg2Vector
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figstract.util.FileManagement
import io.ktor.utils.io.core.toByteArray
import java.io.ByteArrayOutputStream

/**
 * Starting point referenced from:
 * https://medium.com/@bhojwaniravi/how-to-convert-multiple-svgs-to-vector-drawable-in-one-shot-8b5083417747
 * https://github.com/ravibhojwani86/Svg2VectorAndroid
 * https://android.googlesource.com/platform/tools/base/+/master/sdk-common/src/main/java/com/android/ide/common/vectordrawable/Svg2Vector.java
 */
val androidSvgToAvd = ImportPipeline.Step("androidSvgToAvd()") { instruction, input ->
    val tempOutputPath = FileManagement.stepCreateTempFile(
        stepName = "androidSvgToAvd",
        prefix = "${instruction.import.importTarget.outputName}_",
        suffix = ".svg",
    )
    tempOutputPath.toFile().writeBytes(input.data)

    val outputStream = ByteArrayOutputStream()
    Svg2Vector.parseSvgToXml(tempOutputPath, outputStream)

    input
        .copy(
            data = outputStream.toByteArray(),
            target = input.target.copy(
                format = "xml",
            ),
        )
        .single()
}

val androidVectorColorToPlaceholder = ImportPipeline.Step("androidVectorColorToPlaceholder()") { _, input ->
    val updatedFileContents = input.data
        .decodeToString()
        .replace(
            """Color="#(?!(FF00FF|00000000))([0-9A-F]{1,8})"""".toRegex(RegexOption.IGNORE_CASE),
            """Color="#FF00FF"""",
        )

    input
        .copy(
            data = updatedFileContents.toByteArray(),
        )
        .single()
}
