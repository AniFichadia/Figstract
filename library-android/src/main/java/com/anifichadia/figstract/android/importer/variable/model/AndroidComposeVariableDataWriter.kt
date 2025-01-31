package com.anifichadia.figstract.android.importer.variable.model

import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.importer.variable.model.ResolvedThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.ifNotNull
import com.anifichadia.figstract.util.sanitise
import com.anifichadia.figstract.util.sanitiseFileName
import com.anifichadia.figstract.util.toLowerCamelCase
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.withIndent
import java.io.File

class AndroidComposeVariableDataWriter(
    private val outDirectory: File,
    private val packageName: String,
    private val colorAsHex: Boolean = true,
) : VariableDataWriter {
    init {
        require(!outDirectory.exists() || outDirectory.isDirectory)
    }

    override suspend fun write(
        variableData: VariableData,
        resolvedThemeVariantMapping: ResolvedThemeVariantMapping,
    ) {
        val fileSpec = FileSpec.builder(
            packageName = packageName,
            fileName = variableData.variableCollection.name.sanitiseFileName().ToUpperCamelCase(),
        )
            .addType(createVariableCollectionType(variableData))
            .build()

        outDirectory.mkdirs()
        fileSpec.writeTo(outDirectory)
    }

    private fun createVariableCollectionType(variableData: VariableData): TypeSpec {
        return TypeSpec.objectBuilder(variableData.variableCollection.name.sanitiseFileName().ToUpperCamelCase())
            .addKdoc("%L", variableData.variableCollection.name)
            .addTypes(variableData.variablesByMode.map { createModeType(it) })
            .build()
    }

    private fun createModeType(variablesByMode: VariableData.VariablesByMode): TypeSpec {
        return TypeSpec.objectBuilder(variablesByMode.mode.name.sanitiseFileName().ToUpperCamelCase())
            .addKdoc("%L", variablesByMode.mode.name)
            .ifNotNull(variablesByMode.booleanVariables) {
                addType(createVariablesType("Booleans", it, BOOLEAN))
            }
            .ifNotNull(variablesByMode.numberVariables) {
                addType(createVariablesType("Numbers", it, DOUBLE))
            }
            .ifNotNull(variablesByMode.stringVariables) {
                addType(createVariablesType("Strings", it, STRING))
            }
            .ifNotNull(variablesByMode.colorVariables) {
                addType(
                    createVariablesType("Colors", it, composeColorClassName) { color ->
                        if (colorAsHex) {
                            CodeBlock.of("%T(%L)", composeColorClassName, color.toHexString())
                        } else {
                            CodeBlock.builder()
                                .add("%T(", composeColorClassName)
                                .addStatement("")
                                .indent()
                                .withIndent {
                                    this
                                        .addStatement("red = %L,", color.r.toFloat())
                                        .addStatement("green = %L,", color.g.toFloat())
                                        .addStatement("blue = %L,", color.b.toFloat())
                                        .addStatement("alpha = %L,", color.a.toFloat())
                                }
                                .unindent()
                                .add(")")
                                .build()
                        }
                    }
                )
            }
            .build()
    }

    private fun <T> createVariablesType(
        name: String,
        variables: Map<String, T>,
        typeName: TypeName,
        initializer: (value: T) -> CodeBlock = { CodeBlock.of("%L", it) },
    ): TypeSpec {
        return TypeSpec.objectBuilder(name)
            .addKdoc("%L", name)
            .addProperties(
                variables.entries.map { (variableName, value) ->
                    PropertySpec.builder(
                        name = variableName.sanitise().toLowerCamelCase(),
                        type = typeName,
                    )
                        .addKdoc("%L", variableName)
                        .initializer(initializer(value))
                        .build()
                }
            )
            .build()
    }


    private companion object {
        private val composeColorClassName = ClassName("androidx.compose.ui.graphics", "Color")
    }
}
