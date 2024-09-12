package com.anifichadia.figstract.model

abstract class IncludeOrExcludeFilter<T> {
    protected abstract val include: Set<Regex>
    protected abstract val exclude: Set<Regex>
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
