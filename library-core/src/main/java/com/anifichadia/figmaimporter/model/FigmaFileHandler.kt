package com.anifichadia.figmaimporter.model

import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.GetFilesResponse

class FigmaFileHandler(
    val figmaFile: FileKey,
    val assetsPerChunk: Int = MAX_ASSETS_PER_CHUNK,
    val extractor: Extractor,
) {
    init {
        require(assetsPerChunk <= MAX_ASSETS_PER_CHUNK) { "assetsPerChunk '$assetsPerChunk' exceeds $MAX_ASSETS_PER_CHUNK" }
    }

    /**
     * Process a Figma [GetFilesResponse] to create [Instruction] on how to extract from the file
     */
    fun interface Extractor {
        fun extract(response: GetFilesResponse): List<Instruction>
    }

    companion object {
        /** Anything larger than this may cause the Figma API to fail */
        const val MAX_ASSETS_PER_CHUNK = 10
    }
}
