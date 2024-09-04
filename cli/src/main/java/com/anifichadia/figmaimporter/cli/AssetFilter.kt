package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.figma.model.Node

data class AssetFilter(
    private val include: Filter.Include,
    private val exclude: Filter.Exclude,
) {
    /** If the element should be included for processing */
    fun accept(node: Node): Boolean {
        if (include.canFilterNode(node)) {
            return include.accept(node)
        }

        if (exclude.canFilterNode(node)) {
            return exclude.accept(node)
        }

        return true
    }

    sealed class Filter(canvasNames: List<String>, nodeNames: List<String>, parentNodeNames: List<String>) {
        private val canvasNameMatchers: List<Regex> = canvasNames.map { it.toRegex() }
        private val nodeNameMatchers: List<Regex> = nodeNames.map { it.toRegex() }
        private val parentNodeNameMatchers: List<Regex> = parentNodeNames.map { it.toRegex() }

        fun accept(node: Node): Boolean {
            if (node is Node.Canvas) {
                return canvasNameMatchers.accepts(node.name)
            } else if (node is Node.Parent && parentNodeNameMatchers.accepts(node.name)) {
                return true
            }

            return nodeNameMatchers.accepts(node.name)
        }

        fun canFilterNode(node: Node): Boolean {
            if (canvasNameMatchers.isNotEmpty() && node is Node.Canvas) return true
            if (parentNodeNameMatchers.isNotEmpty() && node is Node.Parent) return true

            return nodeNameMatchers.isNotEmpty()
        }

        protected abstract fun List<Regex>.accepts(candidate: String): Boolean

        class Include(
            canvasNames: List<String>,
            nodeNames: List<String>,
            parentNodeNames: List<String>,
        ) : Filter(
            canvasNames = canvasNames,
            nodeNames = nodeNames,
            parentNodeNames = parentNodeNames,
        ) {
            override fun List<Regex>.accepts(candidate: String): Boolean {
                return this.isEmpty() || this.any { it.matches(candidate) }
            }
        }

        class Exclude(
            canvasNames: List<String>,
            nodeNames: List<String>,
            parentNodeNames: List<String>,
        ) : Filter(
            canvasNames = canvasNames,
            nodeNames = nodeNames,
            parentNodeNames = parentNodeNames,
        ) {
            override fun List<Regex>.accepts(candidate: String): Boolean {
                return this.isEmpty() || !this.any { it.matches(candidate) }
            }
        }
    }
}
