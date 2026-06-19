package com.anifichadia.figstract.importer.asset.model.importing.dsl

/**
 * Recursive-descent parser for the Figstract pipeline DSL.
 *
 * ## Grammar (informal)
 *
 * ```
 * pipeline   ::= chain ( NEWLINE chain )*
 * chain      ::= node ( '->' node )*
 * node       ::= combinator | leaf
 * combinator ::= ('and' | 'or') '(' arglist ')'
 * arglist    ::= chain ( ',' chain )*
 * leaf       ::= IDENT ( '(' params ')' )?
 * ```
 *
 * Combinator argument lists are split on commas at paren-depth 0, so step params like
 * `scaleToSize(width=100, height=200)` are never split in the middle.
 *
 * @see PipelineNode for the AST produced.
 */
internal object ImportPipelineDslParser {
    private val COMBINATORS = setOf("and", "or")

    /**
     * Parses [text] into a list of top-level [PipelineNode]s (one per non-blank, non-comment line
     * after stripping comments and normalising whitespace).
     *
     * The caller is responsible for reducing the list into a single node (e.g. via [PipelineNode.Then]).
     */
    fun parse(text: String): List<PipelineNode> {
        return text
            .lines()
            .map { stripComment(it).trim() }
            .filter { it.isNotBlank() }
            .map { line -> parseChain(line) }
    }

    //region Grammar rules
    /** pipeline-chain: node ( '->' node )* */
    private fun parseChain(text: String): PipelineNode {
        val parts = splitOnArrow(text).map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) throw ImportPipelineDslException("Empty pipeline chain in: '$text'")
        if (parts.size == 1) return parseNode(parts[0])
        return PipelineNode.Then(parts.map { parseNode(it) })
    }

    /** node: combinator | leaf */
    private fun parseNode(text: String): PipelineNode {
        val trimmed = text.trim()
        val name = trimmed.substringBefore("(").trim()
        return if (name in COMBINATORS) {
            parseCombinator(trimmed, name)
        } else {
            PipelineNode.Leaf(StepExpression.parse(trimmed))
        }
    }

    /** combinator: ('and'|'or') '(' arglist ')' */
    private fun parseCombinator(text: String, name: String): PipelineNode {
        val argsText = extractCombinatorArgs(text, name)
        val branches = splitOnTopLevelCommas(argsText)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { parseChain(it) }

        if (branches.size < 2) {
            throw ImportPipelineDslException(
                "'$name(...)' requires at least 2 comma-separated branches, got ${branches.size}"
            )
        }

        return when (name) {
            "and" -> PipelineNode.And(branches)
            "or" -> PipelineNode.Or(branches)
            else -> throw ImportPipelineDslException("Unknown combinator '$name'")
        }
    }
    //endregion

    //region Splitting helpers — all paren-depth-aware
    /**
     * Splits [text] on `->` sequences that are not inside parentheses.
     *
     * `scaleToSize(width=100, height=200) -> rename(name=out)` splits into two parts;
     * `and(a() -> b(), c())` is not split because `->` is inside the outer parens.
     */
    private fun splitOnArrow(text: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        var i = 0

        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '(' -> {
                    depth++; current.append(ch); i++
                }

                ch == ')' -> {
                    depth--; current.append(ch); i++
                }

                depth == 0 && text.startsWith("->", i) -> {
                    parts += current.toString()
                    current.clear()
                    i += 2
                }

                else -> {
                    current.append(ch); i++
                }
            }
        }
        parts += current.toString()
        return parts
    }

    /**
     * Splits [text] on commas that are at paren-depth 0.
     *
     * `a(), scaleToSize(width=100, height=200), b()` splits into three parts.
     */
    private fun splitOnTopLevelCommas(text: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0

        for (ch in text) {
            when {
                ch == '(' -> {
                    depth++; current.append(ch)
                }

                ch == ')' -> {
                    depth--; current.append(ch)
                }

                ch == ',' && depth == 0 -> {
                    parts += current.toString(); current.clear()
                }

                else -> current.append(ch)
            }
        }
        parts += current.toString()
        return parts
    }

    /**
     * Extracts the argument string from inside `name(...)`, validating balanced parens.
     *
     * For `and(a(), b())` with name `and`, returns `a(), b()`.
     */
    private fun extractCombinatorArgs(text: String, name: String): String {
        val prefix = "$name("
        if (!text.startsWith(prefix)) {
            throw ImportPipelineDslException("Expected '$name(...)' but got: '$text'")
        }
        if (!text.endsWith(")")) {
            throw ImportPipelineDslException("Unclosed '(' in: '$text'")
        }
        return text.removePrefix(prefix).removeSuffix(")")
    }

    /** Strips a `#`-prefixed comment from a line, respecting quoted strings. */
    private fun stripComment(line: String): String {
        var inQuote = false
        for (i in line.indices) {
            val ch = line[i]
            if (ch == '"') inQuote = !inQuote
            if (!inQuote && ch == '#') return line.substring(0, i)
        }
        return line
    }
    //endregion
}
