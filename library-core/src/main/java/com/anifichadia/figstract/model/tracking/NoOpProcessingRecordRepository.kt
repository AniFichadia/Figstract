package com.anifichadia.figstract.model.tracking

import com.anifichadia.figstract.figma.FileKey
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
