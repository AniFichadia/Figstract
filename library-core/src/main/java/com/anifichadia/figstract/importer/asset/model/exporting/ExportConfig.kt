package com.anifichadia.figstract.importer.asset.model.exporting

import com.anifichadia.figstract.figma.model.ExportSetting
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig.Companion.SCALE_MAX
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig.Companion.SCALE_MIN

data class ExportConfig(
    val format: ExportSetting.Format,
    /** Must be in range [SCALE_MIN] and [SCALE_MAX] */
    val scale: Float? = SCALE_ORIGINAL,
    val contentsOnly: Boolean? = null,
    val useAbsoluteBounds: Boolean? = null,
) {
    init {
        require(scale == null || scale.isNaN() || scale in SCALE_MIN..SCALE_MAX) { "$scale must be null or between $SCALE_MIN and $SCALE_MAX" }
    }

    companion object {
        const val SCALE_PLACEHOLDER: Float = Float.NaN

        const val SCALE_ORIGINAL: Float = 1f
        const val SCALE_MIN: Float = 0.01f
        const val SCALE_MAX: Float = 4f
    }
}
