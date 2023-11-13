package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.NodeId
import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO: build out full Node structure
@Polymorphic
@Serializable
sealed interface Node {
    val id: NodeId
    val name: String
    val visible: Boolean
    val rotation: Number?
    val componentPropertyReferences: Map<String, String>?

    interface Parent : Node {
        val children: List<Node>
    }

    interface Fillable : Node {
        val fills: List<Paint>
    }

    @Serializable
    @SerialName("DOCUMENT")
    data class Document(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,
    ) : Node, Parent

    @Serializable
    @SerialName("CANVAS")
    data class Canvas(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,

        val backgroundColor: Color,
        val flowStartingPoints: List<FlowStartingPoint> = emptyList(),
        val prototypeDevice: PrototypeDevice,

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node, Parent

    /** [Frame] and [Group] are identical */
    @Serializable
    @SerialName("FRAME")
    data class Frame(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,
        val locked: Boolean = false,

        override val fills: List<Paint> = emptyList(),
        val strokes: List<Paint> = emptyList(),
        val strokeWeight: Number? = null,
        val strokeAlign: StrokeAlign? = null,
        val strokeDashes: List<Number>? = null,
        val cornerRadius: Number? = null,
        val rectangleCornerRadiiNumber: List<Number> = emptyList(),
        val cornerSmoothing: Number? = null,

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node, Parent, Fillable

    /** [Frame] and [Group] are identical */
    @Serializable
    @SerialName("GROUP")
    data class Group(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,

        override val fills: List<Paint> = emptyList(),

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node, Parent, Fillable

    @Serializable
    @SerialName("SECTION")
    data class Section(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Parent, Fillable

    @Serializable
    @SerialName("VECTOR")
    data class Vector(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("BOOLEAN_OPERATION")
    data class BooleanOperation(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node, Parent

    @Serializable
    @SerialName("STAR")
    data class Star(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("LINE")
    data class Line(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("ELLIPSE")
    data class Ellipse(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("REGULAR_POLYGON")
    data class RegularPolygon(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("RECTANGLE")
    data class Rectangle(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("TABLE")
    data class Table(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("TABLE_CELL")
    data class TableCell(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("TEXT")
    data class Text(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,
    ) : Node

    @Serializable
    @SerialName("SLICE")
    data class Slice(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node

    @Serializable
    @SerialName("COMPONENT")
    data class Component(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node, Parent

    @Serializable
    @SerialName("COMPONENT_SET")
    data class ComponentSet(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,

        val exportSettings: List<ExportSetting> = emptyList(),
    ) : Node, Parent

    @Serializable
    @SerialName("INSTANCE")
    data class Instance(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val children: List<Node>,

        val exportSettings: List<ExportSetting> = emptyList(),

        val componentId: String,
    ) : Node, Parent

    @Serializable
    @SerialName("STICKY")
    data class Sticky(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("SHAPE_WITH_TEXT")
    data class ShapeWithText(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("CONNECTOR")
    data class Connector(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    @Serializable
    @SerialName("WASHI_TAPE")
    data class WashiTape(
        override val id: NodeId,
        override val name: String,
        override val visible: Boolean = true,
        override val rotation: Number? = null,
        override val componentPropertyReferences: Map<String, String>? = null,

        override val fills: List<Paint> = emptyList(),
    ) : Node, Fillable

    companion object {
        fun Node.traverseDepthFirst(action: (current: Node, parent: Parent?) -> Unit) {
            val stack = ArrayDeque<Pair<Node, Parent?>>()
            stack.addFirst(this to null)

            while (stack.isNotEmpty()) {
                val currentElement = stack.removeFirst()

                val currentNode = currentElement.first
                val currentParent = currentElement.second

                action.invoke(currentNode, currentParent)

                if (currentNode is Parent) {
                    for (index in currentNode.children.size - 1 downTo 0) {
                        stack.addFirst(currentNode.children[index] to currentNode)
                    }
                }
            }
        }

        fun Node.traverseBreadthFirst(action: (current: Node, parent: Parent?) -> Unit) {
            val queue = ArrayDeque<Pair<Node, Parent?>>()
            queue.addFirst(this to null)

            while (queue.isNotEmpty()) {
                val currentElement = queue.removeLast()

                val currentNode = currentElement.first
                val currentParent = currentElement.second

                action.invoke(currentNode, currentParent)

                if (currentNode is Parent) {
                    for (childNode in currentNode.children) {
                        queue.addFirst(childNode to currentNode)
                    }
                }
            }
        }
    }
}
