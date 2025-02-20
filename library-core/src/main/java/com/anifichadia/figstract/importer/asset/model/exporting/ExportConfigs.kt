package com.anifichadia.figstract.importer.asset.model.exporting

import com.anifichadia.figstract.figma.model.ExportSetting

val svg = ExportConfig(
    format = ExportSetting.Format.SVG,
    useAbsoluteBounds = true,
)
val pngUnscaled = ExportConfig(
    format = ExportSetting.Format.PNG,
)
