package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class BranchData(
    val key: String,
    val name: String,
)
