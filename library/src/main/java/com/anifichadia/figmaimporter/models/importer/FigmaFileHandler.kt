package com.anifichadia.figmaimporter.models.importer

import com.anifichadia.figmaimporter.models.figma.FileKey
import com.anifichadia.figmaimporter.models.figma.GetFilesResponse

class FigmaFileHandler(
    val figmaFile: FileKey,
    val extractor: Extractor,
) {
    fun interface Extractor {
        fun extract(response: GetFilesResponse): List<Instruction>
    }
}
