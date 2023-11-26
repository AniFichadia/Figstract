package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class FrameOffset(
    val node_id: String,
    val node_offset: Vector,
)
