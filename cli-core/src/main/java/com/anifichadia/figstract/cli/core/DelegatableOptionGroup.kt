package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.core.GroupableOption
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

open class DelegatableOptionGroup : OptionGroup() {
    internal val options: MutableList<GroupableOption> = mutableListOf()

    override fun registerOption(option: GroupableOption) {
        super.registerOption(option)
        options += option
    }
}

operator fun <T : DelegatableOptionGroup> T.provideDelegate(
    thisRef: OptionGroup,
    prop: KProperty<*>,
): ReadOnlyProperty<OptionGroup, T> {
    this.options.forEach { thisRef.registerOption(it) }
    return ReadOnlyProperty { _, _ -> this@provideDelegate }
}
