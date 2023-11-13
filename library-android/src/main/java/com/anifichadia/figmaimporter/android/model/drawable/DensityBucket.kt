package com.anifichadia.figmaimporter.android.model.drawable

enum class DensityBucket(
    val suffix: String,
    /** [scale] is relative to [MDPI] since it's the 1x scale */
    val scale: Float,
) {
    LDPI("LDPI", 0.75f),
    MDPI("mdpi", 1f),
    HDPI("hdpi", 1.5f),
    XHDPI("xhdpi", 2f),
    XXHDPI("xxhdpi", 3f),
    XXXHDPI("xxxhdpi", 4f),
    ;

    fun scaleRelativeTo(other: DensityBucket): Float {
        return other.scale / this.scale
    }
}
