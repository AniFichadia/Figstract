package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.util.sanitiseFileName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JsonVariableDataWriter(private val outDirectory: File) : VariableDataWriter {
    init {
        require(!outDirectory.exists() || outDirectory.isDirectory)
    }

    override suspend fun write(variableData: VariableData) {
        val data = variableData.variablesByMode.associate {
            val modeOutput = ModeOutput(
                booleanVariables = it.booleanVariables,
                numberVariables = it.numberVariables,
                stringVariables = it.stringVariables,
                colorVariables = it.colorVariables,
            )

            it.mode.name to modeOutput
        }

        val outputFile = outDirectory.fold("${variableData.variableCollection.name.sanitiseFileName()}.json")
        outputFile.parentFile.mkdirs()

        outputFile.writeText(json.encodeToString(data))
    }

    @Serializable
    data class ModeOutput(
        val booleanVariables: Map<String, Boolean>?,
        val numberVariables: Map<String, Number>?,
        val stringVariables: Map<String, String>?,
        val colorVariables: Map<String, Color>?,
    )

    companion object {
        private val json = Json {
            prettyPrint = true
        }
    }
}
