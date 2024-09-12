package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.ImageUrl
import com.anifichadia.figstract.figma.NodeId
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
