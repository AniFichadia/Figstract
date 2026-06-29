package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.Conventions
import com.anifichadia.figstract.cli.core.DelegatableOptionGroup
import com.anifichadia.figstract.cli.core.assets.option.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.option.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.assets.option.AssetTokenStringGeneratorOptionGroup.Companion.createOption
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.AssetRenamingMap
import com.anifichadia.figstract.importer.asset.model.NodeTokenStringGenerator
import com.anifichadia.figstract.ios.ios
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
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

    private val renamingMap by option("--${prefix}RenamingMapFile")
        .help(
            """Path to a JSON file mapping old canvas/node names to new names. Format: 
            |{ 
            |   "canvases": { 
            |       "OldName": "NewName"
            |   }, 
            |   "nodes": { 
            |       "OldName": "NewName" 
            |   } 
            |}""".trimMargin(),
        )
        .file(
            mustExist = true,
            canBeFile = true,
            canBeDir = false,
        )
        .convert { file ->
            try {
                AssetRenamingMap.fromFile(file)
            } catch (e: Exception) {
                throw BadParameterValue("Failed to parse renaming map file '$file': ${e.message}")
            }
        }
        .default(AssetRenamingMap.Empty)

    private val jsonPath by option("--${prefix}JsonPath")

    private val iosGroupByToken by createOption(
        prefix = "${prefix}IosGroupByToken",
        // Intentionally blank as a default value since it's tricky to create a nullable option
        defaultFormat = "",
        casing = Conventions.Casing.ios,
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
                    figmaFileDefinition = FigmaFileDefinition(
                        fileKey = figmaFile,
                        branchName = figmaFileBranchName,
                        version = figmaFileVersion,
                    ),
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    filters = filters,
                    renamingMap = renamingMap,
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
        figmaFileDefinition: FigmaFileDefinition,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        renamingMap: AssetRenamingMap,
        jsonPath: String?,
        iosGroupByToken: NodeTokenStringGenerator?,
        instructionLimit: Int?,
    ): AssetFileHandler
}
