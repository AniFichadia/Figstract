package com.anifichadia.figstract.importer.asset.model.exporting

import com.anifichadia.figstract.figma.model.ExportSetting

data class ExportConfig(
    val format: ExportSetting.Format,
    /** Must 0.01 and 4 */
    val scale: Float = SCALE_ORIGINAL,
    val contentsOnly: Boolean? = null,
) {
    companion object {
        const val SCALE_PLACEHOLDER: Float = Float.NaN

        const val SCALE_ORIGINAL: Float = 1f
        const val SCALE_MIN: Float = 0.01f
        const val SCALE_MAX: Float = 4f
    }
}
