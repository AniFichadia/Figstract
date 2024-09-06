package com.anifichadia.figmaimporter.ios.figma.model

import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.importer.asset.model.exporting.ExportConfig.Companion.SCALE_PLACEHOLDER
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.Scale

val iosIcon = ExportConfig(
    format = ExportSetting.Format.PDF,
)

/** This is a template and should not be used directly. The [ExportConfig.scale] has been set to an impossible value. */
private val iosImage = ExportConfig(
    format = ExportSetting.Format.PNG,
    scale = SCALE_PLACEHOLDER,
)
val ios1xImage = iosImage.copy(
    scale = Scale.`1x`.scale,
)
val ios2xImage = iosImage.copy(
    scale = Scale.`2x`.scale,
)
val ios3xImage = iosImage.copy(
    scale = Scale.`3x`.scale,
)
