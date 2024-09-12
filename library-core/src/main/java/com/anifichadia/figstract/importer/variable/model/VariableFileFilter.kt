package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.model.Mode
import com.anifichadia.figstract.figma.model.VariableCollection
import com.anifichadia.figstract.model.IncludeOrExcludeFilter

data class VariableFileFilter(
    val variableCollectionFilter: VariableCollectionNameFilter,
    val modeNameFilter: ModeNameFilter,
    val variableTypeFilter: VariableTypeFilter,
)

class VariableCollectionNameFilter(
    override val include: Set<Regex>,
    override val exclude: Set<Regex>,
) : IncludeOrExcludeFilter<VariableCollection>() {
    override val getFilterableProperty: (VariableCollection) -> String = { it.name }
}

class ModeNameFilter(
    override val include: Set<Regex>,
    override val exclude: Set<Regex>,
) : IncludeOrExcludeFilter<Mode>() {
    override val getFilterableProperty: (Mode) -> String = { it.name }
}

data class VariableTypeFilter(
    val includeBooleans: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeStrings: Boolean = true,
    val includeColors: Boolean = true,
)
