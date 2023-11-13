package com.anifichadia.figmaimporter.models.figma

enum class ExportFormat {
    JPG, PNG, SVG, PDF;

    val fileExtension: String = name.lowercase()

    override fun toString(): String { return name.lowercase() }
}
