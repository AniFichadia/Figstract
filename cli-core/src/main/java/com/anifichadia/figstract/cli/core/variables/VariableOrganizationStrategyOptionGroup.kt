package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.importer.variable.model.variableorganization.CustomRegexVariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variableorganization.FullPathVariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variableorganization.LeafOnlyVariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variableorganization.StripRootVariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum

class VariableOrganizationStrategyOptionGroup : OptionGroup(
    name = "Variable Organization Strategy",
    help = "Controls how Figma variable paths are rewritten and structured in generated output",
) {
    private val strategy by option("--variableOrganizationStrategy")
        .enum<StrategyChoice>(ignoreCase = true)
        .default(StrategyChoice.Default)
        .help(
            """How Figma variable paths are rewritten before generating output. Also see --variableOrganizationStrategyNested
                |
                |  Default     - Full path as a single flat name.
                |                colour/Primary/Primary → colourPrimaryPrimary
                |
                |  FullPath    - Full path, optionally nested.
                |                nested=false: colour/Primary/Primary → colourPrimaryPrimary (flat)
                |                nested=true:  colour/Primary/Primary → colour > Primary > Primary
                |
                |  LeafOnly    - Only the last path segment is used.
                |                colour/Primary/Primary → Primary
                |
                |  StripRoot   - Drops the first segment, optionally nested.
                |                nested=false: colour/Primary/Primary → Primary/Primary (flat)
                |                nested=true:  colour/Primary/Primary → Primary > Primary
                |
                |  CustomRegex - User-supplied regex with replacement. Requires --variableOrganizationStrategyPattern and  --variableOrganizationStrategyReplacement.
                |                Optionally nested.
            """.trimMargin()

        )

    private val nested by option("--variableOrganizationStrategyNested")
        .boolean()
        .default(false)
        .help(
            """Whether to split the rewritten path on '${VariableOrganizationStrategy.DELIMITER}' to produce nested group hierarchy.
                |Applies to FullPath, StripRoot, and CustomRegex strategies. Has no effect on Default or LeafOnly.
            """.trimMargin()
        )

    private val pattern by option("--variableOrganizationStrategyPattern")
        .help(
            """Kotlin regex pattern applied to the raw Figma variable path.
                |Only used when --variableOrganizationStrategy=custom.
                |The result is split on '/' to produce path segments.
                |Example: ^colour/(.+)${'$'}
            """.trimMargin()
        )

    private val replacement by option("--variableOrganizationStrategyReplacement")
        .help(
            """Replacement string for --variableOrganizationStrategyPattern.
                |Supports backreferences: ${'$'}1, ${'$'}2, ${'$'}{name}.
                |Only used when --variableOrganizationStrategy=custom.
                |Example: ${'$'}1
            """.trimMargin()
        )

    fun toVariableOrganizationStrategy(): VariableOrganizationStrategy {
        if (strategy != StrategyChoice.Default && strategy != StrategyChoice.FullPath) {
            println("Warning: Variable Organization strategies are experimental and is subject to change")
        }

        return when (strategy) {
            StrategyChoice.Default -> VariableOrganizationStrategy.Default
            StrategyChoice.FullPath -> FullPathVariableOrganizationStrategy(nested = nested)
            StrategyChoice.LeafOnly -> LeafOnlyVariableOrganizationStrategy
            StrategyChoice.StripRoot -> StripRootVariableOrganizationStrategy(nested = nested)
            StrategyChoice.CustomRegex -> {
                val resolvedPattern = pattern
                    ?: throw BadParameterValue("--variableOrganizationStrategyPattern is required when --variableOrganizationStrategy=custom")
                val resolvedReplacement = replacement
                    ?: throw BadParameterValue("--variableOrganizationStrategyReplacement is required when --variableOrganizationStrategy=custom")
                val compiledPattern = try {
                    Regex(resolvedPattern)
                } catch (e: Exception) {
                    throw BadParameterValue("Invalid regex pattern \"$resolvedPattern\": ${e.message}")
                }
                CustomRegexVariableOrganizationStrategy(
                    pattern = compiledPattern,
                    replacement = resolvedReplacement,
                    nested = nested,
                )
            }
        }
    }

    private enum class StrategyChoice {
        Default,
        FullPath,
        LeafOnly,
        StripRoot,
        CustomRegex,
        ;
    }
}
