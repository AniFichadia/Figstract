package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.util.toHexString
import kotlinx.serialization.Serializable

@Serializable
data class Color(
    val r: Number,
    val g: Number,
    val b: Number,
    val a: Number,
) {
    fun toArgb(): Int {
        // Ensure values are within the expected range (0.0 to 1.0)
        val r = (r.coerceIn(0.0, 1.0) * 255).toInt()
        val g = (g.coerceIn(0.0, 1.0) * 255).toInt()
        val b = (b.coerceIn(0.0, 1.0) * 255).toInt()
        val a = (a.coerceIn(0.0, 1.0) * 255).toInt()

        // Combine into a single hex int (ARGB)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun toHexString(): String = this.toArgb().toHexString()

    override fun toString(): String {
        return toHexString()
    }
}
