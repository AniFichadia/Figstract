package com.anifichadia.figmaimporter.model

import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.GetFilesResponse
import kotlin.time.Duration
import kotlin.time.TimeSource

class FigmaFileHandler(
    val figmaFile: FileKey,
    val assetsPerChunk: Int = DEFAULT_ASSETS_PER_CHUNK,
    val lifecycle: Lifecycle = Lifecycle.NoOp,
    val extractor: Extractor,
) {
    /**
     * Process a Figma [GetFilesResponse] to create [Instruction] on how to extract from the file
     */
    fun interface Extractor {
        fun extract(response: GetFilesResponse, responseString: String): List<Instruction>
    }

    interface Lifecycle {
        suspend fun onStarted() {}

        suspend fun onFileRetrievalStarted() {}
        suspend fun onFileRetrievalFinished() {}

        suspend fun onExportStarted() {}
        suspend fun onExportFinished() {}

        suspend fun onImportStarted() {}
        suspend fun onImportFinished() {}

        suspend fun onFinished() {}

        object NoOp : Lifecycle

        class Combined : Lifecycle {
            private val lifecycles: List<Lifecycle>

            constructor(lifecycles: List<Lifecycle>) {
                this.lifecycles = lifecycles.filter { it != NoOp }
            }

            constructor(vararg lifecycles: Lifecycle) : this(lifecycles.toList())

            override suspend fun onStarted() {
                lifecycles.forEach { lifecycle -> lifecycle.onStarted() }
            }

            override suspend fun onFileRetrievalStarted() {
                lifecycles.forEach { lifecycle -> lifecycle.onFileRetrievalStarted() }
            }

            override suspend fun onFileRetrievalFinished() {
                lifecycles.forEach { lifecycle -> lifecycle.onFileRetrievalFinished() }
            }

            override suspend fun onExportStarted() {
                lifecycles.forEach { lifecycle -> lifecycle.onExportStarted() }
            }

            override suspend fun onExportFinished() {
                lifecycles.forEach { lifecycle -> lifecycle.onExportFinished() }
            }

            override suspend fun onImportStarted() {
                lifecycles.forEach { lifecycle -> lifecycle.onImportStarted() }
            }

            override suspend fun onImportFinished() {
                lifecycles.forEach { lifecycle -> lifecycle.onImportFinished() }
            }

            override suspend fun onFinished() {
                lifecycles.forEach { lifecycle -> lifecycle.onFinished() }
            }
        }

        class Timing : Lifecycle {
            private var fullLifecycleStartMark: TimeSource.Monotonic.ValueTimeMark? = null
            private var fullDuration: Duration? = null
            private var fileRetrievalStartMark: TimeSource.Monotonic.ValueTimeMark? = null
            private var fileRetrievalDuration: Duration? = null
            private var exportStartMark: TimeSource.Monotonic.ValueTimeMark? = null
            private var exportDuration: Duration? = null
            private var importStartMark: TimeSource.Monotonic.ValueTimeMark? = null
            private var importDuration: Duration? = null

            override suspend fun onStarted() {
                fullLifecycleStartMark = TimeSource.Monotonic.markNow()
            }

            override suspend fun onFileRetrievalStarted() {
                fileRetrievalStartMark = TimeSource.Monotonic.markNow()
            }

            override suspend fun onFileRetrievalFinished() {
                fileRetrievalDuration = fileRetrievalStartMark?.elapsedNow()
            }

            override suspend fun onExportStarted() {
                exportStartMark = TimeSource.Monotonic.markNow()
            }

            override suspend fun onExportFinished() {
                exportDuration = exportStartMark?.elapsedNow()
            }

            override suspend fun onImportStarted() {
                importStartMark = TimeSource.Monotonic.markNow()
            }

            override suspend fun onImportFinished() {
                importDuration = importStartMark?.elapsedNow()
            }

            override suspend fun onFinished() {
                fullDuration = fullLifecycleStartMark?.elapsedNow()
            }

            override fun toString(): String {
                fun StringBuilder.addTiming(
                    name: String,
                    startMark: TimeSource.Monotonic.ValueTimeMark?,
                    duration: Duration?,
                    last: Boolean = false,
                ) {
                    append(name)
                    append(" ")
                    if (duration != null) {
                        startMark!!
                        append("$duration ($startMark)")
                    } else {
                        append("None")
                    }

                    if (!last) {
                        appendLine()
                    }
                }

                return buildString {
                    addTiming("fileRetrieval", fileRetrievalStartMark, fileRetrievalDuration)
                    addTiming("export", exportStartMark, exportDuration)
                    addTiming("import", importStartMark, importDuration)
                    addTiming("full", fullLifecycleStartMark, fullDuration, last = true)
                }
            }
        }

        companion object {
            operator fun Lifecycle.plus(other: Lifecycle): Lifecycle {
                return resolve(this, other) { first, second ->
                    Combined(first, second)
                }
            }

            private inline fun resolve(
                first: Lifecycle,
                second: Lifecycle,
                neither: (first: Lifecycle, second: Lifecycle) -> Lifecycle,
            ): Lifecycle {
                return if (first == NoOp) {
                    second
                } else if (second == NoOp) {
                    first
                } else {
                    neither(first, second)
                }
            }
        }
    }

    companion object {
        /** Anything larger than this may cause the Figma API to fail */
        const val DEFAULT_ASSETS_PER_CHUNK = 10
    }
}
