package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.model.ValueOrVariableAlias.BooleanOrVariableAlias
import com.anifichadia.figstract.figma.model.ValueOrVariableAlias.ColorOrVariableAlias
import com.anifichadia.figstract.figma.model.ValueOrVariableAlias.NumberOrVariableAlias
import com.anifichadia.figstract.figma.model.ValueOrVariableAlias.StringOrVariableAlias
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Polymorphic
@JsonClassDiscriminator("resolvedType")
sealed interface Variable {
    val id: String
    val name: String
    val key: String
    val variableCollectionId: String
    val resolvedType: ResolvedType

    @Serializable
    @SerialName("BOOLEAN")
    data class BooleanVariable(
        override val id: String,
        override val name: String,
        override val key: String,
        override val variableCollectionId: String,
        override val resolvedType: ResolvedType,
        val valuesByMode: Map<String, BooleanOrVariableAlias>,
    ) : Variable

    @Serializable
    @SerialName("FLOAT")
    data class NumberVariable(
        override val id: String,
        override val name: String,
        override val key: String,
        override val variableCollectionId: String,
        override val resolvedType: ResolvedType,
        val valuesByMode: Map<String, NumberOrVariableAlias>,
    ) : Variable

    @Serializable
    @SerialName("STRING")
    data class StringVariable(
        override val id: String,
        override val name: String,
        override val key: String,
        override val variableCollectionId: String,
        override val resolvedType: ResolvedType,
        val valuesByMode: Map<String, StringOrVariableAlias>,
    ) : Variable

    @Serializable
    @SerialName("COLOR")
    data class ColorVariable(
        override val id: String,
        override val name: String,
        override val key: String,
        override val variableCollectionId: String,
        override val resolvedType: ResolvedType,
        val valuesByMode: Map<String, ColorOrVariableAlias>,
    ) : Variable
}

sealed class ValueOrVariableAlias<T>(
    val value: T?,
    val variableAlias: VariableAlias?,
) {
    abstract class Serializer<ValueT, OutT>(
        name: String,
    ) : KSerializer<OutT> where OutT : ValueOrVariableAlias<ValueT> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(name)

        protected abstract fun encode(encoder: Encoder, value: ValueT)

        protected abstract fun decode(decoder: Decoder, element: JsonElement): ValueT?

        protected abstract fun create(
            value: ValueT?,
            variableAlias: VariableAlias?,
        ): OutT

        override fun serialize(encoder: Encoder, value: OutT) {
            val actualValue = value.value
            if (actualValue != null) {
                encode(encoder, actualValue)
            } else {
                val variableAlias = value.variableAlias
                if (variableAlias != null) {
                    encoder.encodeSerializableValue(VariableAlias.serializer(), variableAlias)
                } else {
                    error("Either value or variableAlias is null")
                }
            }
        }

        override fun deserialize(decoder: Decoder): OutT {
            val input = decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by JSON")
            val element = input.decodeJsonElement()

            val decodedValue = decode(decoder, element)
            return if (decodedValue != null) {
                create(
                    value = decodedValue,
                    variableAlias = null,
                )
            } else {
                create(
                    value = null,
                    variableAlias = input.json.decodeFromJsonElement(
                        VariableAlias.serializer(),
                        element,
                    ),
                )
            }
        }
    }

    @Serializable(with = BooleanOrVariableAlias.Serializer::class)
    class BooleanOrVariableAlias(
        value: Boolean?,
        variableAlias: VariableAlias?,
    ) : ValueOrVariableAlias<Boolean>(value, variableAlias) {
        object Serializer :
            ValueOrVariableAlias.Serializer<Boolean, BooleanOrVariableAlias>("BooleanOrVariableAlias") {
            override fun encode(encoder: Encoder, value: Boolean) {
                encoder.encodeBoolean(value)
            }

            override fun decode(decoder: Decoder, element: JsonElement): Boolean? {
                return if (element is JsonPrimitive) {
                    element.boolean
                } else {
                    null
                }
            }

            override fun create(
                value: Boolean?,
                variableAlias: VariableAlias?,
            ) = BooleanOrVariableAlias(value, variableAlias)
        }
    }

    @Serializable(with = NumberOrVariableAlias.Serializer::class)
    class NumberOrVariableAlias(
        value: Number?,
        variableAlias: VariableAlias?,
    ) : ValueOrVariableAlias<Number>(value, variableAlias) {
        object Serializer :
            ValueOrVariableAlias.Serializer<Number, NumberOrVariableAlias>("NumberOrVariableAlias") {
            override fun encode(encoder: Encoder, value: Number) {
                encoder.encodeDouble(value)
            }

            override fun decode(decoder: Decoder, element: JsonElement): Number? {
                return if (element is JsonPrimitive) {
                    element.double
                } else {
                    null
                }
            }

            override fun create(
                value: Number?,
                variableAlias: VariableAlias?,
            ) = NumberOrVariableAlias(value, variableAlias)
        }
    }

    @Serializable(with = StringOrVariableAlias.Serializer::class)
    class StringOrVariableAlias(
        value: String?,
        variableAlias: VariableAlias?,
    ) : ValueOrVariableAlias<String>(value, variableAlias) {
        object Serializer :
            ValueOrVariableAlias.Serializer<String, StringOrVariableAlias>("StringOrVariableAlias") {
            override fun encode(encoder: Encoder, value: String) {
                encoder.encodeString(value)
            }

            override fun decode(decoder: Decoder, element: JsonElement): String? {
                return if (element is JsonPrimitive) {
                    element.toString()
                } else {
                    null
                }
            }

            override fun create(
                value: String?,
                variableAlias: VariableAlias?,
            ) = StringOrVariableAlias(value, variableAlias)
        }
    }

    @Serializable(with = ColorOrVariableAlias.Serializer::class)
    class ColorOrVariableAlias(
        value: Color?,
        variableAlias: VariableAlias?,
    ) : ValueOrVariableAlias<Color>(value, variableAlias) {
        object Serializer :
            ValueOrVariableAlias.Serializer<Color, ColorOrVariableAlias>("ColorOrVariableAlias") {
            override fun encode(encoder: Encoder, value: Color) {
                encoder.encodeSerializableValue(Color.serializer(), value)
            }

            override fun decode(decoder: Decoder, element: JsonElement): Color? {
                return try {
                    Color.serializer().deserialize(decoder)
                } catch (_: Throwable) {
                    null
                }
            }

            override fun create(
                value: Color?,
                variableAlias: VariableAlias?,
            ) = ColorOrVariableAlias(value, variableAlias)
        }
    }
}
