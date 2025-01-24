package com.anifichadia.figstract.ios.assetcatalog

import com.anifichadia.figstract.util.toHexString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Content(
    val info: Info,
    val images: List<Image>? = null,
    val colors: List<Color>? = null,
    val properties: Properties? = null,
) {
    @Serializable
    data class Info(
        val author: String,
        val version: Int,
    ) {
        companion object {
            val xcode = Info("xcode", 1)
        }
    }

    @Serializable
    data class Image(
        val idiom: Idiom,
        val scale: Scale,
        val filename: String,
    )

    /**
     * https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/Named_Color.html
     */
    @Serializable
    data class Color(
        val color: ColorValue,
        val appearances: List<Appearance>?,
        val idiom: Idiom,
    ) {
        @Serializable
        data class ColorValue(
            val components: Components,
            @SerialName("color-space")
            val colorSpace: ColorSpace = ColorSpace.default,
        ) {
            @Serializable(with = ColorSpace.Serializer::class)
            enum class ColorSpace(private val value: String) {
                SRGB("srgb"),
                DisplayP3("display-p3"),
                ;

                class Serializer : KSerializer<ColorSpace> {
                    override val descriptor: SerialDescriptor =
                        PrimitiveSerialDescriptor("ColorSpaceSerializer", PrimitiveKind.STRING)

                    override fun deserialize(decoder: Decoder): ColorSpace {
                        val decoded = decoder.decodeString()
                        return entries.first { it.value == decoded }
                    }

                    override fun serialize(encoder: Encoder, value: ColorSpace) {
                        encoder.encodeString(value.value)
                    }
                }

                companion object {
                    val default = SRGB
                }
            }

            @Serializable
            data class Components(
                val red: String,
                val green: String,
                val blue: String,
                val alpha: String,
            ) {
                constructor(
                    red: Float,
                    green: Float,
                    blue: Float,
                    alpha: Float,
                ) : this(
                    red = red.toColorHexString(),
                    green = green.toColorHexString(),
                    blue = blue.toColorHexString(),
                    alpha = alpha.toString(),
                )
            }

            companion object {
                private fun Float.toColorHexString(): String = this.toHexString(2)
            }
        }

        @Serializable
        data class Appearance(
            val appearance: String,
            val value: String,
        )
    }

    /**
     * https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/ImageSetType.html#//apple_ref/doc/uid/TP40015170-CH25-SW2
     */
    enum class Idiom {
        appLauncher,
        companionSettings,
        `ios-marketing`,
        iphone,
        ipad,
        mac,
        notificationCenter,
        quickLook,
        tv,
        universal,
        watch,
        `watch-marketing`,
        ;

        companion object {
            val default = universal
        }
    }

    @Serializable
    data class Properties(
        @SerialName("provides-namespace") val providesNamespace: Boolean,
    )

    companion object {
        const val FILE_NAME = "Contents.json"
    }
}
