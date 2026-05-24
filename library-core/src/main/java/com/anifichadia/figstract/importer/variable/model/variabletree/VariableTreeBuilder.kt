package com.anifichadia.figstract.importer.variable.model.variabletree

import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.variableorganization.FullPathVariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variableorganization.rewriteWithFallback
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Converts raw [VariableData] into a [VariableGroup] tree that writers consume directly.
 *
 * Pipeline stages:
 * 1. Variant resolution — modes → [RawEntry.Single] or [RawEntry.LightDark]
 * 2. Path rewriting — [VariableOrganizationStrategy] produces [List<String>] segments
 * 3. Collision detection — same segments → fall back to [VariableOrganizationStrategy.Default]
 * 4. Tree construction and type bucketing — segments folded into [VariableGroup] hierarchy,
 *    leaves grouped into typed [VariableTypeBucket]s, empty buckets trimmed
 */
object VariableTreeBuilder {

    fun build(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
        organizationStrategy: VariableOrganizationStrategy,
    ): VariableGroup {
        val raw = resolveVariants(variableData, themeVariantMapping)
        val segmented = rewritePaths(raw, organizationStrategy)
        val withoutCollisions = resolveCollisions(segmented, organizationStrategy)
        return buildTree(name = variableData.variableCollection.name, entries = withoutCollisions)
    }

    // -------------------------------------------------------------------------
    // Stage 1: Variant resolution
    // -------------------------------------------------------------------------

    private sealed interface RawEntry {
        val figmaPath: String

        data class Single(
            override val figmaPath: String,
            val value: VariableValue,
        ) : RawEntry

        data class LightDark(
            override val figmaPath: String,
            val light: VariableValue,
            val dark: VariableValue,
        ) : RawEntry
    }

    private fun resolveVariants(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
    ): Map<String, RawEntry> {
        return when (themeVariantMapping) {
            is ThemeVariantMapping.None -> {
                variableData.variablesByMode
                    .flatMap { mode ->
                        mode.allVariables().map { (name, value) ->
                            name to RawEntry.Single(figmaPath = name, value = value)
                        }
                    }
                    .toMap()
            }

            is ThemeVariantMapping.LightAndDark -> {
                val lightMode = variableData.variablesByMode
                    .find { it.mode.name == themeVariantMapping.lightThemeModeName }
                val darkMode = variableData.variablesByMode
                    .find { it.mode.name == themeVariantMapping.darkThemeModeName }

                if (lightMode == null || darkMode == null) {
                    logger.warn {
                        "ThemeVariantMapping.LightAndDark could not find modes " +
                            "\"${themeVariantMapping.lightThemeModeName}\" / \"${themeVariantMapping.darkThemeModeName}\" " +
                            "in collection \"${variableData.variableCollection.name}\" — falling back to Single"
                    }
                    return resolveVariants(variableData, ThemeVariantMapping.None)
                }

                val lightVars = lightMode.allVariables()
                val darkVars = darkMode.allVariables()

                (lightVars.keys + darkVars.keys).associateWith { name ->
                    val light = lightVars[name]
                    val dark = darkVars[name]
                    when {
                        light != null && dark != null ->
                            RawEntry.LightDark(figmaPath = name, light = light, dark = dark)
                        light != null -> {
                            logger.warn { "Variable \"$name\" has no dark value — falling back to light value for dark" }
                            RawEntry.LightDark(figmaPath = name, light = light, dark = light)
                        }
                        dark != null -> {
                            logger.warn { "Variable \"$name\" has no light value — falling back to light (dark) value" }
                            RawEntry.LightDark(figmaPath = name, light = dark, dark = dark)
                        }
                        else -> error("Unreachable")
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Stage 2: Path rewriting
    // -------------------------------------------------------------------------

    private data class SegmentedEntry(
        val segments: List<String>,
        val raw: RawEntry,
    )

    private fun rewritePaths(
        entries: Map<String, RawEntry>,
        organizationStrategy: VariableOrganizationStrategy,
    ): List<SegmentedEntry> {
        return entries.map { (figmaPath, raw) ->
            val segments = organizationStrategy.rewriteWithFallback(figmaPath)
            SegmentedEntry(segments = segments, raw = raw)
        }
    }

    // -------------------------------------------------------------------------
    // Stage 3: Collision detection
    // -------------------------------------------------------------------------

    private fun resolveCollisions(
        entries: List<SegmentedEntry>,
        organizationStrategy: VariableOrganizationStrategy,
    ): List<SegmentedEntry> {
        if (organizationStrategy is FullPathVariableOrganizationStrategy) return entries

        val seen = mutableMapOf<List<String>, SegmentedEntry>()
        val result = mutableListOf<SegmentedEntry>()
        val collisions = mutableSetOf<List<String>>()

        for (segmented in entries) {
            val existing = seen[segmented.segments]
            if (existing != null) {
                collisions += segmented.segments

                if (result.removeIf { it.segments == existing.segments }) {
                    result += existing.copy(segments = VariableOrganizationStrategy.Default.rewrite(existing.raw.figmaPath))
                }
                result += segmented.copy(segments = VariableOrganizationStrategy.Default.rewrite(segmented.raw.figmaPath))

                logger.warn {
                    "Naming collision: \"${segmented.raw.figmaPath}\" and \"${existing.raw.figmaPath}\" " +
                        "both produced segments ${segmented.segments} — falling back to Default for both"
                }
            } else if (segmented.segments !in collisions) {
                seen[segmented.segments] = segmented
                result += segmented
            } else {
                result += segmented.copy(
                    segments = VariableOrganizationStrategy.Default.rewrite(segmented.raw.figmaPath)
                )
            }
        }

        return result
    }

    // -------------------------------------------------------------------------
    // Stage 4 + 5: Tree construction and type bucketing
    // -------------------------------------------------------------------------

    private fun buildTree(name: String, entries: List<SegmentedEntry>): VariableGroup {
        val leaves = entries.filter { it.segments.size == 1 }
        val buckets = buildBuckets(leaves)

        val childGroups = entries
            .filter { it.segments.size > 1 }
            .groupBy { it.segments.first() }
            .map { (groupName, groupEntries) ->
                buildTree(
                    name = groupName,
                    entries = groupEntries.map { it.copy(segments = it.segments.drop(1)) },
                )
            }

        return VariableGroup(name = name, children = childGroups, buckets = buckets)
    }

    private fun buildBuckets(leaves: List<SegmentedEntry>): List<VariableTypeBucket> {
        val singles = leaves.filterIsInstance<SegmentedEntry>().filter { it.raw is RawEntry.Single }
        val lightDarks = leaves.filter { it.raw is RawEntry.LightDark }

        return buildList {
            // Single buckets — group by value type
            singles
                .groupBy { (it.raw as RawEntry.Single).value::class }
                .forEach { (_, segmented) ->
                    val leafName = { s: SegmentedEntry -> s.segments.first() }
                    when (val sample = (segmented.first().raw as RawEntry.Single).value) {
                        is VariableValue.BooleanValue -> add(
                            VariableTypeBucket.Single.Booleans(segmented.map { s ->
                                VariableEntry(
                                    leafName(s),
                                    s.raw.figmaPath,
                                    (s.raw as RawEntry.Single).value as VariableValue.BooleanValue
                                )
                            })
                        )

                        is VariableValue.NumberValue -> add(
                            VariableTypeBucket.Single.Numbers(segmented.map { s ->
                                VariableEntry(
                                    leafName(s),
                                    s.raw.figmaPath,
                                    (s.raw as RawEntry.Single).value as VariableValue.NumberValue
                                )
                            })
                        )

                        is VariableValue.StringValue -> add(
                            VariableTypeBucket.Single.Strings(segmented.map { s ->
                                VariableEntry(
                                    leafName(s),
                                    s.raw.figmaPath,
                                    (s.raw as RawEntry.Single).value as VariableValue.StringValue
                                )
                            })
                        )

                        is VariableValue.ColorValue -> add(
                            VariableTypeBucket.Single.Colors(segmented.map { s ->
                                VariableEntry(
                                    leafName(s),
                                    s.raw.figmaPath,
                                    (s.raw as RawEntry.Single).value as VariableValue.ColorValue
                                )
                            })
                        )
                    }
                }

            // LightDark buckets — group by light value type
            lightDarks
                .groupBy { ((it.raw as RawEntry.LightDark).light)::class }
                .forEach { (_, segmented) ->
                    val leafName = { s: SegmentedEntry -> s.segments.first() }
                    when ((segmented.first().raw as RawEntry.LightDark).light) {
                        is VariableValue.BooleanValue -> add(
                            VariableTypeBucket.LightAndDark.Booleans(segmented.map { s ->
                                val raw = s.raw as RawEntry.LightDark
                                LightDarkEntry(
                                    light = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.light as VariableValue.BooleanValue
                                    ),
                                    dark = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.dark as VariableValue.BooleanValue
                                    ),
                                )
                            })
                        )

                        is VariableValue.NumberValue -> add(
                            VariableTypeBucket.LightAndDark.Numbers(segmented.map { s ->
                                val raw = s.raw as RawEntry.LightDark
                                LightDarkEntry(
                                    light = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.light as VariableValue.NumberValue
                                    ),
                                    dark = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.dark as VariableValue.NumberValue
                                    ),
                                )
                            })
                        )

                        is VariableValue.StringValue -> add(
                            VariableTypeBucket.LightAndDark.Strings(segmented.map { s ->
                                val raw = s.raw as RawEntry.LightDark
                                LightDarkEntry(
                                    light = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.light as VariableValue.StringValue
                                    ),
                                    dark = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.dark as VariableValue.StringValue
                                    ),
                                )
                            })
                        )

                        is VariableValue.ColorValue -> add(
                            VariableTypeBucket.LightAndDark.Colors(segmented.map { s ->
                                val raw = s.raw as RawEntry.LightDark
                                LightDarkEntry(
                                    light = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.light as VariableValue.ColorValue
                                    ),
                                    dark = VariableEntry(
                                        leafName(s),
                                        raw.figmaPath,
                                        raw.dark as VariableValue.ColorValue
                                    ),
                                )
                            })
                        )
                    }
                }
        }
    }

    private fun VariableData.VariablesByMode.allVariables(): Map<String, VariableValue> {
        return buildMap {
            booleanVariables?.forEach { (k, v) -> put(k, VariableValue.BooleanValue(v)) }
            numberVariables?.forEach { (k, v) -> put(k, VariableValue.NumberValue(v)) }
            stringVariables?.forEach { (k, v) -> put(k, VariableValue.StringValue(v)) }
            colorVariables?.forEach { (k, v) -> put(k, VariableValue.ColorValue(v)) }
        }
    }
}
