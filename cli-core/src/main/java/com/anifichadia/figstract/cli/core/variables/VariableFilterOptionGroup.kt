package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.cli.core.IncludeOrExcludeFilterOptionGroup
import com.anifichadia.figstract.cli.core.IncludeOrExcludeFilterOptionGroup.Companion.provideDelegate
import com.anifichadia.figstract.importer.variable.model.ModeNameFilter
import com.anifichadia.figstract.importer.variable.model.VariableCollectionNameFilter
import com.anifichadia.figstract.importer.variable.model.VariableFileFilter
import com.anifichadia.figstract.importer.variable.model.VariableTypeFilter
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.MultiUsageError
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean

class VariableFilterOptionGroup : OptionGroup() {
    private val variableCollection by IncludeOrExcludeFilterOptionGroup(null, "VariableCollection")
    private val mode by IncludeOrExcludeFilterOptionGroup(null, "Mode")

    private val includeBooleans by option("--includeTypeBoolean")
        .boolean()
        .default(true)
    private val includeNumbers by option("--includeTypeNumber")
        .boolean()
        .default(true)
    private val includeStrings by option("--includeTypeString")
        .boolean()
        .default(true)
    private val includeColors by option("--includeTypeColor")
        .boolean()
        .default(true)

    fun toVariableFilter(): VariableFileFilter {
        val errors = buildList {
            add(variableCollection.error())
            add(mode.error())
            if (!includeBooleans && !includeNumbers && !includeStrings && !includeColors) {
                add(BadParameterValue("No included types have been enabled"))
            }
        }.filterNotNull()
        if (errors.isNotEmpty()) throw MultiUsageError(errors)

        return VariableFileFilter(
            variableCollectionFilter = VariableCollectionNameFilter(
                include = variableCollection.includes,
                exclude = variableCollection.excludes,
            ),
            modeNameFilter = ModeNameFilter(
                include = mode.includes,
                exclude = mode.excludes,
            ),
            variableTypeFilter = VariableTypeFilter(
                includeBooleans = includeBooleans,
                includeNumbers = includeNumbers,
                includeStrings = includeStrings,
                includeColors = includeColors,
            ),
        )
    }
}
