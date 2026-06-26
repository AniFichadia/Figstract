package com.anifichadia.figstract.cli.core.assets.model

import com.anifichadia.figstract.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.importer.asset.model.AssetFilter
import com.anifichadia.figstract.importer.asset.model.AssetRenamingMap
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.importer.asset.model.importing.ArtworkOutputFormat
import com.anifichadia.figstract.model.TokenStringGenerator
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Polymorphic
@Serializable
sealed interface AssetConfig {
    val fileDefinition: FigmaFileDefinition
    val enabled: Boolean
    val outDirectory: String?

    val assetFilter: AssetFilter

    val renamingMap: AssetRenamingMap

    val jsonPath: String?

    val instructionLimit: Int?


    sealed interface Convention : AssetConfig {
        val namingFormats: NamingFormats

        val platformOptions: PlatformOptions

        val iosGroupByTokenNamingFormat: String?
    }

    @Serializable
    @SerialName("Artwork")
    data class Artwork(
        override val fileDefinition: FigmaFileDefinition,
        override val enabled: Boolean,
        override val outDirectory: String? = null,

        override val assetFilter: AssetFilter = AssetFilter.Empty,

        override val renamingMap: AssetRenamingMap = AssetRenamingMap.Empty,
        override val namingFormats: NamingFormats = NamingFormats(
            androidFormat = """{canvas.name}_{node.name}""",
            iosFormat = """{canvas.name}{node.name}""",
            webFormat = """{canvas.name}_{node.name}""",
        ),

        override val jsonPath: String? = null,

        override val platformOptions: PlatformOptions = PlatformOptions(),

        override val iosGroupByTokenNamingFormat: String? = null,

        override val instructionLimit: Int? = null,

        val createUncropped: Boolean = true,
        val createCropped: Boolean = false,

        val androidOutputDensityBuckets: List<DensityBucket> = DensityBucket.defaults,

        val iosOutputScales: List<Scale> = Scale.defaults,
        val iosOutputFormat: ArtworkOutputFormat = ArtworkOutputFormat.Default,
    ) : Convention

    @Serializable
    @SerialName("Icon")
    data class Icon(
        override val fileDefinition: FigmaFileDefinition,
        override val enabled: Boolean,
        override val outDirectory: String? = null,

        override val assetFilter: AssetFilter = AssetFilter.Empty,

        override val renamingMap: AssetRenamingMap = AssetRenamingMap.Empty,
        override val namingFormats: NamingFormats = NamingFormats(
            androidFormat = """ic_{node.name}""",
            iosFormat = """{node.name}""",
            webFormat = """{node.name}""",
        ),

        override val jsonPath: String? = null,

        override val platformOptions: PlatformOptions = PlatformOptions(),

        override val iosGroupByTokenNamingFormat: String? = null,

        override val instructionLimit: Int? = null,
    ) : Convention

    @Serializable
    @SerialName("Custom")
    data class Custom(
        override val fileDefinition: FigmaFileDefinition,
        override val enabled: Boolean,
        override val outDirectory: String? = null,

        val pipelineDefinition: String,

        override val jsonPath: String,
        val exportConfig: ExportConfig,

        override val assetFilter: AssetFilter = AssetFilter.Empty,

        override val renamingMap: AssetRenamingMap = AssetRenamingMap.Empty,

        val namingFormat: String,
        val namingCasing: TokenStringGenerator.Casing,

        override val instructionLimit: Int? = null,
    ) : AssetConfig
}
