package com.anifichadia.figstract.ios.assetcatalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Content(
    val info: Info,
    val images: List<Image>? = null,
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
