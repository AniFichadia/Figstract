package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.NodeId
import kotlinx.serialization.Serializable

@Serializable
data class FlowStartingPoint(
    val nodeId: NodeId,
    val name: String,
)
