package com.anifichadia.figstract.ios.importer.variable.model

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.importer.variable.model.ResolvedThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.ifNotNull
import com.anifichadia.figstract.util.sanitise
import com.anifichadia.figstract.util.sanitiseFileName
import com.anifichadia.figstract.util.toLowerCamelCase
import io.outfoxx.swiftpoet.BOOL
import io.outfoxx.swiftpoet.CodeBlock
import io.outfoxx.swiftpoet.DOUBLE
import io.outfoxx.swiftpoet.DeclaredTypeName
import io.outfoxx.swiftpoet.FileSpec
import io.outfoxx.swiftpoet.Modifier
import io.outfoxx.swiftpoet.PropertySpec
import io.outfoxx.swiftpoet.STRING
import io.outfoxx.swiftpoet.TypeName
import io.outfoxx.swiftpoet.TypeSpec
import java.io.File

@ExperimentalFigstractApi
class IosSwiftUiVariableDataWriter(
    private val outDirectory: File,
    private val modulePath: String,
) : VariableDataWriter {
    override suspend fun write(
        variableData: VariableData,
        resolvedThemeVariantMapping: ResolvedThemeVariantMapping,
    ) {
        val fileSpec = FileSpec.builder(
            moduleName = modulePath,
            fileName = variableData.variableCollection.name.sanitiseFileName().ToUpperCamelCase(),
        )
            .addType(createVariableCollectionType(variableData))
            .build()

        val resolvedOutputDirectory = outDirectory.fold(modulePath.split("."))
        resolvedOutputDirectory.mkdirs()
        fileSpec.writeTo(resolvedOutputDirectory)
    }

    private fun createVariableCollectionType(variableData: VariableData): TypeSpec {
        return TypeSpec.classBuilder(variableData.variableCollection.name.sanitiseFileName().ToUpperCamelCase())
            .addDoc("%L\n", variableData.variableCollection.name)
            .addTypes(variableData.variablesByMode.map { createModeType(it) })
            .build()
    }

    private fun createModeType(variablesByMode: VariableData.VariablesByMode): TypeSpec {
        return TypeSpec.classBuilder(variablesByMode.mode.name.sanitiseFileName().ToUpperCamelCase())
            .addDoc("%L\n", variablesByMode.mode.name)
            .ifNotNull(variablesByMode.booleanVariables) {
                addType(createVariablesType("Booleans", it, BOOL))
            }
            .ifNotNull(variablesByMode.numberVariables) {
                addType(createVariablesType("Numbers", it, DOUBLE))
            }
            .ifNotNull(variablesByMode.stringVariables) {
                addType(createVariablesType("Strings", it, STRING))
            }
            .ifNotNull(variablesByMode.colorVariables) {
                addType(
                    createVariablesType("Colors", it, swiftUiColorTypeName) { color ->
                        CodeBlock.builder()
                            .add("%T(red: %L, green: %L, blue: %L, opacity: %L)", swiftUiColorTypeName, color.r.toFloat(), color.g.toFloat(), color.b.toFloat(), color.a.toFloat())
                            .build()
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
        return TypeSpec.enumBuilder(name)
            .addDoc("%L\n", name)
            .addProperties(
                variables.entries.map { (variableName, value) ->
                    PropertySpec.builder(
                        name = variableName.sanitise().toLowerCamelCase(),
                        type = typeName,
                        modifiers = arrayOf(Modifier.STATIC),
                    )
                        .mutable(false)
                        .addDoc("%L\n", variableName)
                        .initializer(initializer(value))
                        .build()
                }
            )
            .build()
    }


    private companion object {
        private val swiftUiColorTypeName = DeclaredTypeName.typeName("SwiftUi.Color")
    }
}
