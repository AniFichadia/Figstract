package com.anifichadia.figstract.model.tracking

import com.anifichadia.figstract.figma.FileKey
import java.time.OffsetDateTime

interface ProcessingRecordRepository {
    suspend fun createRecord(
        figmaFile: FileKey,
        name: String?,
        lastProcessed: OffsetDateTime,
    )

    suspend fun readRecord(
        figmaFile: FileKey,
        name: String?,
    ): ProcessingRecord? = readAll().firstOrNull { it.figmaFile == figmaFile && it.name == name }

    suspend fun readAll(): List<ProcessingRecord>

    suspend fun updateRecord(
        figmaFile: FileKey,
        name: String?,
        lastProcessed: OffsetDateTime,
    ) = createRecord(figmaFile, name, lastProcessed)
}
