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
    )

    companion object {
        val tokens: List<Token<NodeContext>> = listOf(
            Token.Simple("canvas.id") { it.canvas.id },
            Token.Simple("canvas.name") { it.canvas.name },
            Token.Simple("node.id") { it.node.id },
            Token.Simple("node.name") { it.node.name },
            Token.Complex("""node.name.split "(?<splitOn>.+)" (?<indexOrLocation>(\d+)|first|last)""".toRegex()) { matchResult, context ->
                val splitOn = matchResult.groups["splitOn"]?.value ?: return@Complex null
                context.node.name.let { name ->
                    if (name.contains(splitOn)) {
                        name.split(splitOn).let { split ->
                            when (val indexOrLocation = matchResult.groups["indexOrLocation"]!!.value) {
                                "first" -> split.first()
                                "last" -> split.last()
                                else -> split[indexOrLocation.toInt()]
                            }
                        }
                    } else {
                        name
                    }
                }
            },
        )
    }
}
