package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.model.GetFilesResponse
import com.anifichadia.figstract.importer.Lifecycle

class AssetFileHandler(
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

    companion object {
        /** Anything larger than this may cause the Figma API to fail */
        const val DEFAULT_ASSETS_PER_CHUNK = 10
    }
}
