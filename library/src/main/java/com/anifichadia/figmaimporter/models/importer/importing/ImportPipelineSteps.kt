package com.anifichadia.figmaimporter.models.importer.importing

import com.android.ide.common.vectordrawable.Svg2Vector
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readFully
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Starting point referenced from:
 * https://medium.com/@bhojwaniravi/how-to-convert-multiple-svgs-to-vector-drawable-in-one-shot-8b5083417747
 * https://github.com/ravibhojwani86/Svg2VectorAndroid
 * https://android.googlesource.com/platform/tools/base/+/master/sdk-common/src/main/java/com/android/ide/common/vectordrawable/Svg2Vector.java
 */
val svgToAvd = ImportPipelineStep { instruction, (channel, format) ->
    val tempDirectoryPath = Paths.get("", "temp", "avdConversion").also {
        val tempDirectory = it.toAbsolutePath().toFile()
        tempDirectory.mkdirs()
        tempDirectory.deleteOnExit()
    }
    val tempOutputPath = Files.createTempFile(tempDirectoryPath, "${instruction.import.outputName}_", ".svg")
    val outputFile = tempOutputPath.toFile().also {
        it.deleteOnExit()
    }

    channel.copyAndClose(outputFile.writeChannel())

    // TODO: when writing to file, there's a delay flushing the file??? which causes the following steps to fail cause the file doesn't exist
    var attemptCount = 0
    var attemptLimit = 10
    val attemptDelay = 10L

    var failure: Throwable?

    do {
        attemptCount += 1
        delay(attemptDelay)

        try {
            val outputStream = ByteArrayOutputStream()
            Svg2Vector.parseSvgToXml(tempOutputPath, outputStream)

            return@ImportPipelineStep ImportPipelineStep.Output(ByteReadChannel(outputStream.toByteArray()), "xml")
        } catch (e: Throwable) {
            failure = e
        }
    } while (attemptCount <= attemptLimit)

    println("${instruction.export.nodeId} ${instruction.import.outputName}: $failure")

    throw failure ?: error("whoops")
}

val androidVectorColorToPlaceholder = ImportPipelineStep { _, (channel, format) ->
    val bytes = ByteArray(channel.availableForRead)
    channel.readFully(bytes)

    // TODO: use this regex instead? Color=\"#(?!FF00FF|00000000).+
    val updatedFileContents = bytes
        .decodeToString()
        .replace("""android:fillColor="#000000"""", """android:fillColor="#FF00FF"""")

    ImportPipelineStep.Output(ByteReadChannel(updatedFileContents.toByteArray()), format)
}
