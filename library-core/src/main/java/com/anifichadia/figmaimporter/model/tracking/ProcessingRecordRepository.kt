package com.anifichadia.figmaimporter.model.tracking

import com.anifichadia.figmaimporter.figma.FileKey
import java.time.OffsetDateTime

interface ProcessingRecordRepository {
    suspend fun createRecord(figmaFile: FileKey, lastProcessed: OffsetDateTime)

    suspend fun readRecord(figmaFile: FileKey): ProcessingRecord? =
        readAll().firstOrNull { it.figmaFile == figmaFile }

    suspend fun readAll(): List<ProcessingRecord>

    suspend fun updateRecord(figmaFile: FileKey, lastProcessed: OffsetDateTime) =
        createRecord(figmaFile, lastProcessed)
}
