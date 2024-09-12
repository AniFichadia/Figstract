package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.NodeId
import kotlinx.serialization.Serializable

@Serializable
data class FlowStartingPoint(
    val nodeId: NodeId,
    val name: String,
)
