package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.DelegatableOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetTokenStringGeneratorOptionGroup.Companion.createOption
import com.anifichadia.figstract.cli.core.assets.NodeTokenStringGenerator
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.model.TokenStringGenerator.Casing
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import java.io.File

abstract class AssetHandlerOptionGroup(protected val prefix: String) : DelegatableOptionGroup() {
    private val enabled by option("--${prefix}Enabled")
        .boolean()
        .default(false)

    private val figmaFile by option("--${prefix}FigmaFile")
    private val figmaFileBranchName by option("--${prefix}FigmaFileBranchName")
    private val figmaFileVersion by option("--${prefix}FigmaFileVersion")

    private val filters by AssetFilterOptionGroup(prefix)

    private val jsonPath by option("--${prefix}JsonPath")

    private val iosGroupByToken by createOption(
        prefix = "${prefix}IosGroupByToken",
        // Intentionally blank as a default value since it's tricky to create a nullable option
        defaultFormat = "",
        casing = Casing.UpperCamelCase,
    )

    private val instructionLimit by option("--${prefix}InstructionLimit").int()

    protected abstract val nameGenerators: AssetTokenStringGeneratorOptionGroup

    fun createHandler(
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
    ): AssetFileHandler? {
        if (enabled) {
            val figmaFile = this.figmaFile
            if (figmaFile != null) {
                return createHandlerInternal(
                    figmaFile = figmaFile,
                    figmaFileBranchName = figmaFileBranchName,
                    figmaFileVersion = figmaFileVersion,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    filters = filters,
                    jsonPath = jsonPath,
                    iosGroupByToken = iosGroupByToken.takeIf { it.format.isNotBlank() },
                    instructionLimit = instructionLimit,
                )
            } else {
                throw BadParameterValue("$prefix are enabled but figma file is not specified")
            }
        }
        return null
    }

    protected abstract fun createHandlerInternal(
        figmaFile: String,
        figmaFileBranchName: String?,
        figmaFileVersion: String?,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        jsonPath: String?,
        iosGroupByToken: NodeTokenStringGenerator?,
        instructionLimit: Int?,
    ): AssetFileHandler
}
