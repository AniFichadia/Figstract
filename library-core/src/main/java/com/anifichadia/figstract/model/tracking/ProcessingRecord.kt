package com.anifichadia.figstract.model.tracking

import com.anifichadia.figstract.figma.FileKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class ProcessingRecord(
    val figmaFile: FileKey,
    @Contextual
    val lastProcessed: OffsetDateTime,
)
