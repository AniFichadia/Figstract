package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class ExportSetting(
    val suffix: String?,
    val format: Format,
    val constraint: Constraint,
) {
    enum class Format {
        JPG,
        PNG,
        SVG,
        PDF,
        ;

        val fileExtension: String = name.lowercase()

        override fun toString(): String = name.lowercase()

        companion object {
            fun fromFileExtensionOrNull(value: String?): Format? {
                return if (value != null) {
                    Format.entries.firstOrNull {
                        it.fileExtension.equals(value, ignoreCase = true)
                    }
                } else {
                    null
                }
            }
        }
    }
}
