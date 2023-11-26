package com.anifichadia.figmaimporter.model.tracking

import com.anifichadia.figmaimporter.figma.FileKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ProcessingRecord(
    val figmaFile: FileKey,
    @Contextual
    val lastProcessed: OffsetDateTime,
)
