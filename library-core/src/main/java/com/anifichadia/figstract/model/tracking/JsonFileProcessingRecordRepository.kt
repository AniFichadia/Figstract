package com.anifichadia.figstract.model.tracking

import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.type.serializer.OffsetDateTimeSerializer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.io.File
import java.time.OffsetDateTime

class JsonFileProcessingRecordRepository(
    private val recordFile: File,
) : ProcessingRecordRepository {
    private val json = Json {
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(OffsetDateTimeSerializer())
        }
    }
    private val fileLock = Mutex(false)

    override suspend fun createRecord(figmaFile: FileKey, lastProcessed: OffsetDateTime) {
        val records = readAll().toMutableList()

        fileLock.withLock {
            val indexOfFirst = records.indexOfFirst { it.figmaFile == figmaFile }
            if (indexOfFirst >= 0) {
                records.removeAt(indexOfFirst)
                records.add(indexOfFirst, ProcessingRecord(figmaFile, lastProcessed))
            } else {
                records.add(ProcessingRecord(figmaFile, lastProcessed))
            }

            if (recordFile.exists()) {
                recordFile.delete()
            }
            recordFile.parentFile.mkdir()
            recordFile.writeText(json.encodeToString(records))
        }
    }

    override suspend fun readAll(): List<ProcessingRecord> {
        if (!recordFile.exists()) {
            return emptyList()
        }

        val fileContent = fileLock.withLock {
            recordFile.readText()
        }

        return json.decodeFromString<List<ProcessingRecord>>(fileContent)
    }
}
