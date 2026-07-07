package com.anifichadia.figstract.model.tracking

import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.type.noOp
import java.time.OffsetDateTime

object NoOpProcessingRecordRepository : ProcessingRecordRepository {
    override suspend fun createRecord(
        figmaFile: FileKey,
        name: String?,
        lastProcessed: OffsetDateTime,
    ) = noOp()

    override suspend fun readAll(): List<ProcessingRecord> {
        return emptyList()
    }
}
