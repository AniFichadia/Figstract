package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.model.TokenStringGenerator

class NodeTokenStringGenerator(
    override val format: String,
    override val casing: Casing,
) : TokenStringGenerator<NodeTokenStringGenerator.NodeContext>() {
    override val tokens: List<Token<NodeContext>> = NodeTokenStringGenerator.tokens

    data class NodeContext(
        val canvas: Node.Canvas,
        val node: Node,
        val parentNode: Node,
    )

    companion object {
        val tokens: List<Token<NodeContext>> = listOf(
            Token("canvas.id") { it.canvas.id },
            Token("canvas.name") { it.canvas.name },
            Token("node.id") { it.node.id },
            Token("node.name") { it.node.name },
            Token("parentNode.id") { it.parentNode.id },
            Token("parentNode.name") { it.parentNode.name },
            Token("parentNode.splitName") { context ->
                context.parentNode.name.let {
                    if (it.contains("/")) {
                        it.split("/").last()
                    } else {
                        it
                    }
                }
            },
        )
    }
}
