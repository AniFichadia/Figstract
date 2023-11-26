package com.anifichadia.figmaimporter.model.tracking

import com.anifichadia.figmaimporter.figma.FileKey
import java.time.OffsetDateTime

object NoOpProcessingRecordRepository : ProcessingRecordRepository {
    override suspend fun createRecord(
        figmaFile: FileKey,
        lastProcessed: OffsetDateTime,
    ) {/* No-op */
    }

    override suspend fun readAll(): List<ProcessingRecord> {
        return emptyList()
    }
}
