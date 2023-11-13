package com.anifichadia.figmaimporter.model.exporting

import com.anifichadia.figmaimporter.figma.model.ExportSetting

data class ExportConfig(
    val format: ExportSetting.Format,
    /** Must 0.01 and 4 */
    val scale: Float = UNSCALED,
) {
    companion object {
        const val SCALE_PLACEHOLDER: Float = Float.NaN

        const val UNSCALED: Float = 1f
    }
}
