package com.anifichadia.figmaimporter.models.figma

import arrow.optics.optics
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@optics
@Polymorphic
@Serializable
sealed interface Paint {
    val visible: Boolean

    @optics
    @Serializable
    @SerialName("SOLID")
    data class Solid(
        override val visible: Boolean = true,
    ) : Paint {
        companion object
    }

    @optics
    @Serializable
    @SerialName("GRADIENT_LINEAR")
    data class GradientLinear(
        override val visible: Boolean = true,
    ) : Paint {
        companion object
    }

    @optics
    @Serializable
    @SerialName("GRADIENT_RADIAL")
    data class GradientRadial(
        override val visible: Boolean = true,
    ) : Paint {
        companion object
    }

    @optics
    @Serializable
    @SerialName("GRADIENT_ANGULAR")
    data class GradientAngular(
        override val visible: Boolean = true,
    ) : Paint {
        companion object
    }

    @optics
    @Serializable
    @SerialName("GRADIENT_DIAMOND")
    data class GradientDiamond(
        override val visible: Boolean = true,
    ) : Paint {
        companion object
    }

    @optics
    @Serializable
    @SerialName("IMAGE")
    data class Image(
        override val visible: Boolean = true,
        val imageRef: String,
    ) : Paint {
        companion object
    }

    @optics
    @Serializable
    @SerialName("EMOJI")
    data class Emoji(
        override val visible: Boolean = true,
    ) : Paint {
        companion object
    }

    @optics
    @Serializable
    @SerialName("VIDEO")
    data class Video(
        override val visible: Boolean = true,
    ) : Paint {
        companion object
    }

    companion object
}
