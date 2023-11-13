package com.anifichadia.figmaimporter.models.importer.exporting

import com.anifichadia.figmaimporter.models.figma.ExportFormat

data class ExportConfig(
    val format: ExportFormat,
    /** Must 0.01 and 4 */
    val scale: Float,
) {
    companion object {
        const val SCALE_PLACEHOLDER: Float = Float.NaN

        val svgIcon = ExportConfig(
            format = ExportFormat.SVG,
            scale = 1f,
        )

        //region Android
        /** This is a template and should not be used directly. The [scale] has been set to an impossible value. */
        private val androidImage = ExportConfig(
            format = ExportFormat.PNG,
            scale = SCALE_PLACEHOLDER,
        )
        val androidImageLdpi = androidImage.copy(
            scale = 0.75f,
        )
        val androidImageMdpi = androidImage.copy(
            scale = 1f,
        )
        val androidImageHdpi = androidImage.copy(
            scale = 1.5f,
        )
        val androidImageXHdpi = androidImage.copy(
            scale = 2f,
        )
        val androidImageXxHdpi = androidImage.copy(
            scale = 3f,
        )
        val androidImageXxxHdpi = androidImage.copy(
            scale = 4f,
        )
        //endregion

        //region iOS
        val iosIcon = ExportConfig(
            format = ExportFormat.PDF,
            scale = 1f,
        )
        //endregion
    }
}
