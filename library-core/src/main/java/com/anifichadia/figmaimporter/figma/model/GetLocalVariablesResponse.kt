package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class GetLocalVariablesResponse(
    val err: String? = null,
    val status: Int? = null,
    val meta: Meta? = null,
) {
    @Serializable
    data class Meta(
        val variableCollections: Map<String, VariableCollection>,
        val variables: Map<String, Variable>,
    )

    companion object KnownErrors
}
