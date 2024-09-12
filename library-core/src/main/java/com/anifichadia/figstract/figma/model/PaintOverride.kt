package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
class PaintOverride(
    val fills: List<Paint> = emptyList(),
    val inheritFillStyleId: String,
)
