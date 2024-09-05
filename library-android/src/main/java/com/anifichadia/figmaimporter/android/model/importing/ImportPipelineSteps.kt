package com.anifichadia.figmaimporter.android.model.importing

import com.android.ide.common.vectordrawable.Svg2Vector
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figmaimporter.util.FileManagement
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.delay
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
    val outputFile = tempOutputPath.toFile().also {
        it.deleteOnExit()
    }

    outputFile.writeBytes(input.data)

    // TODO: when writing to file, there's a delay flushing the file??? which causes the following steps to fail cause the file doesn't exist
    var attemptCount = 0
    val attemptLimit = 20
    val attemptDelay = 50L

    var failure: Throwable?
    do {
        attemptCount += 1
        delay(attemptDelay)

        try {
            val outputStream = ByteArrayOutputStream()
            Svg2Vector.parseSvgToXml(tempOutputPath, outputStream)

            return@Step input
                .copy(
                    data = outputStream.toByteArray(),
                    target = input.target.copy(
                        format = "xml",
                    ),
                )
                .single()
        } catch (e: Throwable) {
            failure = e
        }
    } while (attemptCount <= attemptLimit)

    // TODO logger this
    println("${instruction.export.nodeId} ${instruction.import.importTarget.outputName}: $failure")

    throw failure ?: error("whoops")
}

val androidVectorColorToPlaceholder = ImportPipeline.Step("androidVectorColorToPlaceholder()") { _, input ->
    // TODO: use this regex instead? Color=\"#(?!FF00FF|00000000).+
    val updatedFileContents = input.data
        .decodeToString()
        .replace("""Color="#000000"""", """Color="#FF00FF"""")

    input
        .copy(
            data = updatedFileContents.toByteArray(),
        )
        .single()
}
