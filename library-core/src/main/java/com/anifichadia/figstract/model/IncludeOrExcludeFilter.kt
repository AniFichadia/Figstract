package com.anifichadia.figstract.model

abstract class IncludeOrExcludeFilter<T> {
    protected abstract val include: Set<Regex>
    protected abstract val exclude: Set<Regex>
    protected abstract val getFilterableProperty: (T) -> String

    fun accept(value: T): Boolean {
        if (include.isEmpty() && exclude.isEmpty()) return true

        val filterableProperty = getFilterableProperty(value)

        return if (include.isNotEmpty()) {
            include.any { it.matches(filterableProperty) }
        } else {
            !exclude.any { it.matches(filterableProperty) }
        }
    }
}
