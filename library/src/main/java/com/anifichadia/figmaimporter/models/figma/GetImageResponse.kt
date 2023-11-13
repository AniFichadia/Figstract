package com.anifichadia.figmaimporter.models.figma

import kotlinx.serialization.Serializable

@Serializable
data class GetImageResponse(
    val err: String? = null,
    val status: Int? = null,
    val images: Map<NodeId, ImageUrl>,
)
