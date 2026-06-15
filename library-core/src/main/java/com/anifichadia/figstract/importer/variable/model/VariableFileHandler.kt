package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.writer.VariableDataWriter

data class VariableFileHandler(
    val figmaFile: FileKey,
    val figmaFileBranchName: String?,
    val figmaFileVersion: String?,
    val filter: VariableFileFilter,
    val renamingMap: VariableRenamingMap,
    val themeVariantMappings: Map<String, ThemeVariantMapping>,
    val variableOrganizationStrategy: VariableOrganizationStrategy,
    val writers: List<VariableDataWriter>,
) {
    fun withResolvedBranchKey(branchKey: FileKey) = copy(
        figmaFile = branchKey,
        figmaFileBranchName = null,
    )
}
