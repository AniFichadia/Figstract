package com.anifichadia.figstract.importer.variable.model.variabletree

/**
 * A group of homogeneously-typed variables within a [VariableGroup]. Empty buckets should never exist.
 *
 * The hierarchy has three levels:
 * - [VariableTypeBucket] — top, for writers that don't care about variant or value type
 * - [Single] / [LightAndDark] — intermediate, for writers that care about variant only (e.g. XML)
 * - [Single.Colors] etc. — leaf, for writers that care about both (e.g. Compose, SwiftUI)
 */
sealed interface VariableTypeBucket {
    val name: String

    sealed interface Single<T : VariableValue> : VariableTypeBucket {
        val entries: List<VariableEntry<T>>

        data class Booleans(
            override val entries: List<VariableEntry<VariableValue.BooleanValue>>,
        ) : Single<VariableValue.BooleanValue> {
            override val name: String get() = "Booleans"

            init {
                require(entries.isNotEmpty()) { "Booleans bucket must not be empty" }
            }
        }

        data class Numbers(
            override val entries: List<VariableEntry<VariableValue.NumberValue>>,
        ) : Single<VariableValue.NumberValue> {
            override val name: String get() = "Numbers"

            init {
                require(entries.isNotEmpty()) { "Numbers bucket must not be empty" }
            }
        }

        data class Strings(
            override val entries: List<VariableEntry<VariableValue.StringValue>>,
        ) : Single<VariableValue.StringValue> {
            override val name: String get() = "Strings"

            init {
                require(entries.isNotEmpty()) { "Strings bucket must not be empty" }
            }
        }

        data class Colors(
            override val entries: List<VariableEntry<VariableValue.ColorValue>>,
        ) : Single<VariableValue.ColorValue> {
            override val name: String get() = "Colors"

            init {
                require(entries.isNotEmpty()) { "Colors bucket must not be empty" }
            }
        }
    }

    sealed interface LightAndDark<T : VariableValue> : VariableTypeBucket {
        val entries: List<LightDarkEntry<T>>

        data class Booleans(
            override val entries: List<LightDarkEntry<VariableValue.BooleanValue>>,
        ) : LightAndDark<VariableValue.BooleanValue> {
            override val name: String get() = "Booleans"

            init {
                require(entries.isNotEmpty()) { "LightDark.Booleans bucket must not be empty" }
            }
        }

        data class Numbers(
            override val entries: List<LightDarkEntry<VariableValue.NumberValue>>,
        ) : LightAndDark<VariableValue.NumberValue> {
            override val name: String get() = "Numbers"

            init {
                require(entries.isNotEmpty()) { "LightDark.Numbers bucket must not be empty" }
            }
        }

        data class Strings(
            override val entries: List<LightDarkEntry<VariableValue.StringValue>>,
        ) : LightAndDark<VariableValue.StringValue> {
            override val name: String get() = "Strings"

            init {
                require(entries.isNotEmpty()) { "LightDark.Strings bucket must not be empty" }
            }
        }

        data class Colors(
            override val entries: List<LightDarkEntry<VariableValue.ColorValue>>,
        ) : LightAndDark<VariableValue.ColorValue> {
            override val name: String get() = "Colors"

            init {
                require(entries.isNotEmpty()) { "LightDark.Colors bucket must not be empty" }
            }
        }
    }
}
