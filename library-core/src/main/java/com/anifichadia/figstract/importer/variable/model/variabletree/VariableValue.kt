package com.anifichadia.figstract.importer.variable.model.variabletree

import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.model.Color

sealed interface VariableValue {
    @JvmInline value class BooleanValue(val value: Boolean) : VariableValue
    @JvmInline value class NumberValue(val value: Number) : VariableValue
    @JvmInline value class StringValue(val value: String) : VariableValue
    @JvmInline value class ColorValue(val value: Color) : VariableValue
}
