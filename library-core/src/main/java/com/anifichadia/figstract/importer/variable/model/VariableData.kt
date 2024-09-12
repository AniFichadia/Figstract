package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.figma.model.Mode
import com.anifichadia.figstract.figma.model.VariableCollection

class VariableData(
    val variableCollection: VariableCollection,
    val variablesByMode: List<VariablesByMode>,
) {
    data class VariablesByMode(
        val mode: Mode,
        val booleanVariables: Map<String, Boolean>?,
        val numberVariables: Map<String, Number>?,
        val stringVariables: Map<String, String>?,
        val colorVariables: Map<String, Color>?,
    ) {
        fun isEmpty(): Boolean = booleanVariables.isNullOrEmpty()
            && numberVariables.isNullOrEmpty()
            && stringVariables.isNullOrEmpty()
            && colorVariables.isNullOrEmpty()

        fun isNotEmpty() = !isEmpty()
    }
}
