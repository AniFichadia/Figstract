package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.ImageUrl
import com.anifichadia.figmaimporter.figma.NodeId
import kotlinx.serialization.Serializable

@Serializable
data class GetImageResponse(
    val err: String? = null,
    val status: Int? = null,
    val images: Map<NodeId, ImageUrl?>,
) {
    companion object KnownErrors {
        val tooManyImages = Error(400, "Document images too large or too many images")
    }
}
