package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.util.sanitiseFileName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File

class JsonVariableDataWriter(
    private val outDirectory: File,
    private val colorAsHex: Boolean = true,
) : VariableDataWriter {
    init {
        require(!outDirectory.exists() || outDirectory.isDirectory)
    }

    override suspend fun write(variableData: VariableData) {
        val data = variableData.variablesByMode.associate {
            val modeOutput = ModeOutput(
                booleans = it.booleanVariables,
                numbers = it.numberVariables,
                strings = it.stringVariables,
                colors = it.colorVariables?.let { colors ->
                    colors.mapValues { (_, value) ->
                        if (colorAsHex) {
                            ModeOutput.ColorFormat(
                                color = null,
                                hexColor = value.toHexString(),
                            )
                        } else {
                            ModeOutput.ColorFormat(
                                color = value,
                                hexColor = null,
                            )
                        }
                    }
                },
            )

            it.mode.name to modeOutput
        }

        val outputFile = outDirectory.fold("${variableData.variableCollection.name.sanitiseFileName()}.json")
        outputFile.parentFile.mkdirs()

        outputFile.writeText(json.encodeToString(data))
    }

    @Serializable
    data class ModeOutput(
        val booleans: Map<String, Boolean>?,
        val numbers: Map<String, Number>?,
        val strings: Map<String, String>?,
        val colors: Map<String, ColorFormat>?,
    ) {
        @Serializable(with = ColorFormat.Serializer::class)
        data class ColorFormat(
            val color: Color?,
            val hexColor: String?,
        ) {
            init {
                require(color != null || hexColor != null)
            }

            class Serializer : KSerializer<ColorFormat> {
                override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ColorFormat")

                override fun deserialize(decoder: Decoder): ColorFormat = TODO()

                override fun serialize(encoder: Encoder, value: ColorFormat) {
                    val color = value.color
                    val hexColor = value.hexColor
                    if (color != null) {
                        Color.serializer().serialize(encoder, color)
                    } else if (hexColor != null) {
                        String.serializer().serialize(encoder, hexColor)
                    } else {
                        error("Cannot serialize ColorFormat")
                    }
                }
            }
        }
    }

    companion object {
        private val json = Json {
            prettyPrint = true
            explicitNulls = false
        }
    }
}
