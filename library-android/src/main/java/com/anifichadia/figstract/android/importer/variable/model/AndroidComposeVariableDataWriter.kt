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
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
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
            .addType(createVariableCollectionType(variableData, resolvedThemeVariantMapping))
            .build()

        outDirectory.mkdirs()
        fileSpec.writeTo(outDirectory)
    }

    private fun createVariableCollectionType(
        variableData: VariableData,
        resolvedThemeVariantMapping: ResolvedThemeVariantMapping,
    ): TypeSpec {
        return TypeSpec.objectBuilder(variableData.variableCollection.name.sanitiseFileName().ToUpperCamelCase())
            .addKdoc("%L", variableData.variableCollection.name)
            .apply {
                // TODO: only add types if there's data mode data to use and there's no resolved colors
                addTypes(variableData.variablesByMode.map {
                    createModeType(
                        it,
                        resolvedThemeVariantMapping
                    )
                })

                if (resolvedThemeVariantMapping is ResolvedThemeVariantMapping.LightAndDark) {
                    val mappings = resolvedThemeVariantMapping.colors
                    val colorNames = mappings.keys.associateWith { it.sanitise().toLowerCamelCase() }

                    val properties = colorNames
                        .map { (_, safeColorName) ->
                            PropertySpec.builder(safeColorName, composeColorClassName)
                                .initializer(safeColorName)
                                .build()
                        }

                    addType(
                        TypeSpec.classBuilder(colorsClassName)
                            .addModifiers(KModifier.DATA)
                            .primaryConstructor(
                                FunSpec.constructorBuilder()
                                    .addParameters(
                                        properties.map { property ->
                                            ParameterSpec.builder(property.name, property.type).build()
                                        }
                                    )
                                    .build()
                            )
                            .addProperties(properties)
                            .addType(
                                TypeSpec.companionObjectBuilder()
                                    .addProperty(generateColorVariantProperty(true, colorNames, mappings))
                                    .addProperty(generateColorVariantProperty(false, colorNames, mappings))
                                    .build()
                            )
                            .build()
                    )
                }
            }
            .build()
    }

    private fun createModeType(
        variablesByMode: VariableData.VariablesByMode,
        resolvedThemeVariantMapping: ResolvedThemeVariantMapping,
    ): TypeSpec {
        return TypeSpec.objectBuilder(variablesByMode.mode.name.sanitiseFileName().ToUpperCamelCase())
            .addKdoc("%L", variablesByMode.mode.name)
            .ifNotNull(variablesByMode.booleanVariables) {
                addType(createVariablesType(booleansClassName, it, BOOLEAN))
            }
            .ifNotNull(variablesByMode.numberVariables) {
                addType(createVariablesType(numbersClassName, it, DOUBLE))
            }
            .ifNotNull(variablesByMode.stringVariables) {
                addType(createVariablesType(stringsClassName, it, STRING))
            }
            .apply {
                val colorVariables = variablesByMode.colorVariables
                if (colorVariables != null && resolvedThemeVariantMapping is ResolvedThemeVariantMapping.None) {
                    addType(
                        createVariablesType(colorsClassName, colorVariables, composeColorClassName) { color ->
                            colorInitializer(color)
                        }
                    )
                }
            }
            .build()
    }

    private fun generateColorVariantProperty(
        isLight: Boolean,
        colorNames: Map<String, String>,
        mappings: Map<String, ResolvedThemeVariantMapping.LightAndDark.Value<Color>>,
    ): PropertySpec {
        val className = ClassName("", colorsClassName)
        return PropertySpec.builder(
            name = if (isLight) {
                "light"
            } else {
                "dark"
            },
            type = className,
        )
            .initializer(
                CodeBlock.builder()
                    .add("%T(", className)
                    .apply {
                        colorNames.forEach { (colorName, safeColorName) ->
                            add("\n")
                            add("%L = ", safeColorName)
                            val color = mappings[colorName]!!.resolve(isLight = isLight)
                            add(colorInitializer(color))
                            add(",")
                        }
                    }
                    .add("\n)")
                    .build()
            )
            .build()
    }

    private fun colorInitializer(color: Color) = if (colorAsHex) {
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
        private val booleansClassName = "Booleans"
        private val numbersClassName = "Numbers"
        private val stringsClassName = "Strings"
        private val colorsClassName = "Colors"
    }
}
