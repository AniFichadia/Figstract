package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Polymorphic
@Serializable
sealed interface Paint {
    val visible: Boolean
    val opacity: Number

    @Serializable
    @SerialName("SOLID")
    data class Solid(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
        val color: Color,
    ) : Paint

    @Serializable
    @SerialName("GRADIENT_LINEAR")
    data class GradientLinear(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
        val blendMode: BlendMode,
        val gradientHandlePositions: List<Vector>,
        val gradientStops: List<ColorStop>,
    ) : Paint

    @Serializable
    @SerialName("GRADIENT_RADIAL")
    data class GradientRadial(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
        val blendMode: BlendMode,
        val gradientHandlePositions: List<Vector>,
        val gradientStops: List<ColorStop>,
    ) : Paint

    @Serializable
    @SerialName("GRADIENT_ANGULAR")
    data class GradientAngular(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
        val blendMode: BlendMode,
        val gradientHandlePositions: List<Vector>,
        val gradientStops: List<ColorStop>,
    ) : Paint

    @Serializable
    @SerialName("GRADIENT_DIAMOND")
    data class GradientDiamond(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
        val blendMode: BlendMode,
        val gradientHandlePositions: List<Vector>,
        val gradientStops: List<ColorStop>,
    ) : Paint

    @Serializable
    @SerialName("IMAGE")
    data class Image(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
        val scaleMode: ScaleMode,
//        val imageTransform: Transform,
        val scalingFactor: Number? = null,
        val rotation: Number? = null,
        val imageRef: String? = null,
        val filters: ImageFilters? = null,
        val gifRef: String? = null,
//        val boundVariables: Map<String, VariableAlias | VariableAlias[]>
    ) : Paint {
        enum class ScaleMode {
            FILL,
            FIT,
            TILE,
            STRETCH,
            ;
        }
    }

    @Serializable
    @SerialName("EMOJI")
    data class Emoji(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
    ) : Paint

    @Serializable
    @SerialName("VIDEO")
    data class Video(
        override val visible: Boolean = true,
        override val opacity: Number = 1.0,
    ) : Paint
}
