package com.anifichadia.figstract.android.importer.variable.model

import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.importer.variable.model.ResolvedThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.ifC
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
                when (resolvedThemeVariantMapping) {
                    is ResolvedThemeVariantMapping.LightAndDark -> {
                        if (variableData.booleansProvided) {
                            addType(
                                generateType(
                                    mappings = resolvedThemeVariantMapping.booleans,
                                    generatedTypeClassName = ClassName("", booleansClassName),
                                    propertyType = BOOLEAN,
                                )
                            )
                        }
                        if (variableData.numbersProvided) {
                            addType(
                                generateType(
                                    mappings = resolvedThemeVariantMapping.numbers,
                                    generatedTypeClassName = ClassName("", numbersClassName),
                                    propertyType = DOUBLE,
                                )
                            )
                        }
                        if (variableData.stringsProvided) {
                            addType(
                                generateType(
                                    mappings = resolvedThemeVariantMapping.strings,
                                    generatedTypeClassName = ClassName("", stringsClassName),
                                    propertyType = STRING,
                                )
                            )
                        }
                        if (variableData.colorsProvided) {
                            addType(
                                generateType(
                                    mappings = resolvedThemeVariantMapping.colors,
                                    generatedTypeClassName = ClassName("", colorsClassName),
                                    propertyType = composeColorClassName,
                                    initializer = ::colorInitializer,
                                )
                            )
                        }
                    }

                    is ResolvedThemeVariantMapping.None -> {
                        addTypes(
                            variableData.variablesByMode.map {
                                createModeType(it)
                            }
                        )
                    }
                }
            }
            .build()
    }

    private fun <T> generateType(
        mappings: Map<String, ResolvedThemeVariantMapping.LightAndDark.Value<T>>,
        generatedTypeClassName: ClassName,
        propertyType: TypeName,
        initializer: (value: T) -> CodeBlock = { CodeBlock.of("%L", it) },
    ): TypeSpec {
        val names = mappings.keys.associateWith { it.sanitise().toLowerCamelCase() }
        val properties = names
            .map { (_, safeColorName) ->
                PropertySpec.builder(safeColorName, propertyType)
                    .initializer(safeColorName)
                    .build()
            }

        return TypeSpec.classBuilder(generatedTypeClassName)
            .ifC(mappings.isNotEmpty()) {
                addModifiers(KModifier.DATA)
            }
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
                    .addProperty(
                        generateVariantProperty(
                            isLight = true,
                            typeName = generatedTypeClassName,
                            valueNames = names,
                            mappings = mappings,
                            initializer = initializer,
                        )
                    )
                    .addProperty(
                        generateVariantProperty(
                            isLight = false,
                            typeName = generatedTypeClassName,
                            valueNames = names,
                            mappings = mappings,
                            initializer = initializer,
                        )
                    )
                    .build()
            )
            .build()
    }

    private fun createModeType(
        variablesByMode: VariableData.VariablesByMode,
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
            .ifNotNull(variablesByMode.colorVariables) {
                addType(createVariablesType(colorsClassName, it, composeColorClassName, ::colorInitializer))
            }
            .build()
    }

    private fun <T> generateVariantProperty(
        isLight: Boolean,
        typeName: TypeName,
        valueNames: Map<String, String>,
        mappings: Map<String, ResolvedThemeVariantMapping.LightAndDark.Value<T>>,
        initializer: (value: T) -> CodeBlock = { CodeBlock.of("%L", it) },
    ): PropertySpec {
        return PropertySpec.builder(
            name = if (isLight) {
                "light"
            } else {
                "dark"
            },
            type = typeName,
        )
            .initializer(
                CodeBlock.builder()
                    .add("%T(", typeName)
                    .withIndent {
                        addStatement("")
                        valueNames.forEach { (valueName, safeValueName) ->
                            add("%L = ", safeValueName)
                            val value = mappings[valueName]!!.resolve(isLight = isLight)
                            val initializerCode = initializer(value)
                            add(initializerCode)
                            addStatement(",")
                        }
                    }
                    .add(")")
                    .build()
            )
            .build()
    }

    private fun colorInitializer(color: Color) = if (colorAsHex) {
        CodeBlock.of("%T(%L)", composeColorClassName, color.toHexString())
    } else {
        CodeBlock.builder()
            .add("%T(", composeColorClassName)
            .withIndent {
                addStatement("")
                addStatement("red = %L,", color.r.toFloat())
                addStatement("green = %L,", color.g.toFloat())
                addStatement("blue = %L,", color.b.toFloat())
                addStatement("alpha = %L,", color.a.toFloat())
            }
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
