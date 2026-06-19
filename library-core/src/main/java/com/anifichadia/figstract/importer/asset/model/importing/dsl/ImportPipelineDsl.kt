package com.anifichadia.figstract.importer.asset.model.importing.dsl

import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.or
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then

/**
 * Parses a text-based pipeline DSL into an [ImportPipeline.Step] chain.
 *
 * ## Format
 *
 * Each non-blank, non-comment line is a pipeline expression. Multiple top-level lines are
 * sequenced with `then` — equivalent to separating them with `->`.
 *
 * ### Leaf steps
 *
 * ```
 * stepName()
 * stepName(param=value)
 * stepName(param1=value1, param2=value2)
 * ```
 *
 * ### Sequential composition (`->` / `then`)
 *
 * Outputs from the left step are fed into the right step.
 *
 * ```
 * scale(scale=2.0) -> convertToWebPLossy(qualityPercent=80) -> rename(name=out)
 * ```
 *
 * ### Parallel fan-out (`and`)
 *
 * Runs all branches against the same input concurrently; collects all outputs.
 * Each branch is a full pipeline chain separated by commas.
 *
 * ```
 * and(convertToWebPLossy(qualityPercent=75), convertToPngLossless())
 * ```
 *
 * Branches can themselves be chains:
 * ```
 * and(
 *   convertToWebPLossy(qualityPercent=75) -> renameSuffix(suffix=_web),
 *   convertToPngLossless() -> renameSuffix(suffix=_fallback)
 * )
 * ```
 *
 * ### First-non-empty fallback (`or`)
 *
 * Tries each branch in order; returns the first non-empty output.
 *
 * ```
 * or(convertToWebPLossy(qualityPercent=75), convertToPngLossless())
 * ```
 *
 * ### Nesting
 *
 * Combinators can be nested and combined with `->`:
 *
 * ```
 * scale(scale=2.0) -> and(
 *   convertToWebPLossy(qualityPercent=75) -> renameSuffix(suffix=_web),
 *   or(convertToWebPLossy(qualityPercent=50), convertToPngLossless()) -> renameSuffix(suffix=_fallback)
 * )
 * ```
 *
 * ### Comments and blank lines
 *
 * Lines starting with `#` (or containing an inline `#` outside a quoted string) are stripped.
 * Blank lines are ignored.
 */
class ImportPipelineDsl(
    private val registry: ImportPipelineStepRegistry,
) {
    /**
     * Parses [dsl] into a single [ImportPipeline.Step].
     *
     * @throws ImportPipelineDslException if any step name is unknown, a parameter is invalid, or the DSL is
     * structurally malformed.
     */
    fun parse(dsl: String): ImportPipeline.Step {
        val nodes = try {
            ImportPipelineDslParser.parse(dsl)
        } catch (e: ImportPipelineDslException) {
            throw e
        } catch (e: Exception) {
            throw ImportPipelineDslException("Failed to parse pipeline DSL: ${e.message}", e)
        }

        if (nodes.isEmpty()) {
            throw ImportPipelineDslException("Pipeline DSL is empty, at least one step is required")
        }

        // Multiple top-level lines are sequenced with then
        return nodes
            .map { node -> evaluate(node) }
            .reduce { acc, step -> acc then step }
    }

    private fun evaluate(node: PipelineNode): ImportPipeline.Step = when (node) {
        is PipelineNode.Leaf -> registry.resolve(node.expression)

        is PipelineNode.Then -> node.steps
            .map { evaluate(it) }
            .reduce { acc, step -> acc then step }

        is PipelineNode.And -> node.branches
            .map { evaluate(it) }
            .reduce { acc, step -> acc and step }

        is PipelineNode.Or -> node.branches
            .map { evaluate(it) }
            .reduce { acc, step -> acc or step }
    }
}
