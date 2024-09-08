package com.anifichadia.figmaimporter.model

abstract class IncludeOrExcludeFilter<T> {
    protected abstract val include: List<Regex>
    protected abstract val exclude: List<Regex>
    protected abstract val getFilterableProperty: (T) -> String

    fun accept(value: T): Boolean {
        val filterableProperty = getFilterableProperty(value)

        if (include.isNotEmpty()) {
            return include.any { it.matches(filterableProperty) }
        }

        if (exclude.isNotEmpty()) {
            return !exclude.any { it.matches(filterableProperty) }
        }

        return true
    }
}
