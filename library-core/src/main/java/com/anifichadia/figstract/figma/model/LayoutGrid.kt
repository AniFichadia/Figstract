package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class LayoutGrid(
    val pattern: Pattern,
    val sectionSize: Number,
    val visible: Boolean,
    val color: Color,
    val alignment: Alignment,
    val gutterSize: Number,
    val offset: Number,
    val count: Number,
) {
    enum class Pattern {
        COLUMNS,
        ROWS,
        GRID,
        ;
    }

    enum class Alignment {
        MIN,
        STRETCH,
        CENTER,
        ;
    }
}
