package com.anifichadia.figstract.importer.asset.model.importing.dsl

import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.or
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then

/**
 * AST node produced by [ImportPipelineDslParser] before evaluation against a
 * [ImportPipelineStepRegistry].
 *
 * The tree structure mirrors the combinator hierarchy of [ImportPipeline.Step]:
 */
sealed interface PipelineNode {
    /** A single step call resolved through the registry. */
    data class Leaf(val expression: StepExpression) : PipelineNode

    /** @see [ImportPipeline.Step.then] */
    data class Then(val steps: List<PipelineNode>) : PipelineNode {
        init {
            require(steps.size >= 2) { "Then requires at least 2 steps" }
        }
    }

    /** @see [ImportPipeline.Step.and] */
    data class And(val branches: List<PipelineNode>) : PipelineNode {
        init {
            require(branches.size >= 2) { "and() requires at least 2 branches" }
        }
    }

    /** @see [ImportPipeline.Step.or] */
    data class Or(val branches: List<PipelineNode>) : PipelineNode {
        init {
            require(branches.size >= 2) { "or() requires at least 2 branches" }
        }
    }
}
