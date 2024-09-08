package com.anifichadia.figmaimporter.importer.variable.model

import com.anifichadia.figmaimporter.figma.model.Mode
import com.anifichadia.figmaimporter.figma.model.VariableCollection
import com.anifichadia.figmaimporter.model.IncludeOrExcludeFilter

data class VariableFileFilter(
    val variableCollectionFilter: VariableCollectionNameFilter,
    val modeNameFilter: ModeNameFilter,
    val variableTypeFilter: VariableTypeFilter,
)

class VariableCollectionNameFilter(
    override val include: List<Regex>,
    override val exclude: List<Regex>,
) : IncludeOrExcludeFilter<VariableCollection>() {
    override val getFilterableProperty: (VariableCollection) -> String = { it.name }
}

class ModeNameFilter(
    override val include: List<Regex>,
    override val exclude: List<Regex>,
) : IncludeOrExcludeFilter<Mode>() {
    override val getFilterableProperty: (Mode) -> String = { it.name }
}

data class VariableTypeFilter(
    val includeBooleans: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeStrings: Boolean = true,
    val includeColors: Boolean = true,
)
