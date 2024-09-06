package com.anifichadia.figmaimporter.cli

import com.github.ajalt.clikt.core.MutuallyExclusiveGroupException
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class IncludeOrExcludeFilterOptionGroup(
    prefix: String? = null,
    suffix: String,
) : OptionGroup() {
    private val includeOptionName = name(prefix, true, suffix)
    private val includesOption = createOption(includeOptionName)
    private val _includes by includesOption
    val includes: List<Regex> get() = _includes.map { it.toRegex() }

    private val excludeOptionName = name(prefix, false, suffix)
    private val excludesOption = createOption(excludeOptionName)
    private val _excludes by excludesOption
    val excludes: List<Regex> get() = _excludes.map { it.toRegex() }

    fun error(): UsageError? {
        return if (_includes.isNotEmpty() && _excludes.isNotEmpty()) {
            MutuallyExclusiveGroupException(listOf(includeOptionName, excludeOptionName))
        } else {
            null
        }
    }

    companion object {
        fun name(prefix: String?, isInclude: Boolean, suffix: String): String {
            return buildString {
                if (prefix != null) {
                    append(prefix)
                    append("Filter")
                } else {
                    append("filter")
                }

                if (isInclude) {
                    append("Included")
                } else {
                    append("Excluded")
                }

                append(suffix.replaceFirstChar { it.uppercase() })
            }
        }

        fun ParameterHolder.createOption(name: String) = option("--$name").multiple().unique()

        operator fun IncludeOrExcludeFilterOptionGroup.provideDelegate(
            thisRef: OptionGroup,
            prop: KProperty<*>,
        ): ReadOnlyProperty<OptionGroup, IncludeOrExcludeFilterOptionGroup> {
            thisRef.registerOption(includesOption)
            thisRef.registerOption(excludesOption)
            return ReadOnlyProperty { _, _ -> this@provideDelegate }
        }
    }
}
