package com.anifichadia.figstract.model.tracking

import com.anifichadia.figstract.figma.FileKey
import java.time.OffsetDateTime

interface ProcessingRecordRepository {
    suspend fun createRecord(figmaFile: FileKey, lastProcessed: OffsetDateTime)

    suspend fun readRecord(figmaFile: FileKey): ProcessingRecord? =
        readAll().firstOrNull { it.figmaFile == figmaFile }

    suspend fun readAll(): List<ProcessingRecord>

    suspend fun updateRecord(figmaFile: FileKey, lastProcessed: OffsetDateTime) =
        createRecord(figmaFile, lastProcessed)
}
