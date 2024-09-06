package com.anifichadia.figmaimporter.android.importer.asset.model.drawable

enum class DensityBucket(
    val suffix: String,
    /** [scale] relative to [MDPI]. [MDPI] represents a 1x scale */
    val scale: Float,
) {
    LDPI("ldpi", 0.75f),
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
