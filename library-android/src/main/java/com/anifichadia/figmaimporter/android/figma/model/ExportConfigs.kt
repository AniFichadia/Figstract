package com.anifichadia.figmaimporter.android.figma.model

import com.anifichadia.figmaimporter.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.importer.asset.model.exporting.ExportConfig.Companion.SCALE_PLACEHOLDER

/** This is a template and should not be used directly. The [ExportConfig.scale] has been set to an impossible value. */
private val androidImage = ExportConfig(
    format = ExportSetting.Format.PNG,
    scale = SCALE_PLACEHOLDER,
)
val androidImageLdpi = androidImage.copy(
    scale = DensityBucket.LDPI.scale,
)
val androidImageMdpi = androidImage.copy(
    scale = DensityBucket.MDPI.scale,
)
val androidImageHdpi = androidImage.copy(
    scale = DensityBucket.HDPI.scale,
)
val androidImageXHdpi = androidImage.copy(
    scale = DensityBucket.XHDPI.scale,
)
val androidImageXxHdpi = androidImage.copy(
    scale = DensityBucket.XXHDPI.scale,
)
val androidImageXxxHdpi = androidImage.copy(
    scale = DensityBucket.XXXHDPI.scale,
)
