package com.anifichadia.figstract.ios.importer.variable.model.writer

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variabletree.LightDarkEntry
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableEntry
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableGroup
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableTypeBucket
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableValue
import com.anifichadia.figstract.importer.variable.model.writer.VariableDataWriter
import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.util.ToUpperCamelCase
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
        themeVariantMapping: ThemeVariantMapping,
        organizationStrategy: VariableOrganizationStrategy,
        collectionName: String,
        root: VariableGroup,
    ) {
        val fileSpec = FileSpec.builder(
            moduleName = modulePath,
            fileName = collectionName.sanitiseFileName().ToUpperCamelCase(),
        )
            .addType(createGroupType(root, isRoot = true))
            .build()

        val resolvedOutputDirectory = outDirectory.fold(modulePath.split("."))
        resolvedOutputDirectory.mkdirs()
        fileSpec.writeTo(resolvedOutputDirectory)
    }

    private fun createGroupType(group: VariableGroup, isRoot: Boolean = false): TypeSpec {
        val typeName = if (isRoot) {
            group.name.sanitiseFileName().ToUpperCamelCase()
        } else {
            group.name.sanitise().ToUpperCamelCase()
        }

        return TypeSpec.enumBuilder(typeName)
            .addDoc("%L\n", group.name)
            .apply {
                group.children.forEach { child -> addType(createGroupType(child)) }
                group.buckets.forEach { bucket -> addType(createBucketType(bucket)) }
            }
            .build()
    }

    private fun createBucketType(bucket: VariableTypeBucket): TypeSpec {
        return when (bucket) {
            is VariableTypeBucket.Single.Booleans -> createSingleType(bucket.name, bucket.entries, BOOL)

            is VariableTypeBucket.Single.Numbers -> createSingleType(bucket.name, bucket.entries, DOUBLE)

            is VariableTypeBucket.Single.Strings -> createSingleType(bucket.name, bucket.entries, STRING)

            is VariableTypeBucket.Single.Colors -> createSingleType(bucket.name, bucket.entries, colorTypeName)

            is VariableTypeBucket.LightAndDark.Booleans -> createLightDarkType(bucket.name, bucket.entries, BOOL)

            is VariableTypeBucket.LightAndDark.Numbers -> createLightDarkType(bucket.name, bucket.entries, DOUBLE)

            is VariableTypeBucket.LightAndDark.Strings -> createLightDarkType(bucket.name, bucket.entries, STRING)

            is VariableTypeBucket.LightAndDark.Colors -> createLightDarkType(bucket.name, bucket.entries, colorTypeName)
        }
    }

    private fun <V : VariableValue> createSingleType(
        name: String,
        entries: List<VariableEntry<V>>,
        typeName: TypeName,
    ): TypeSpec {
        return TypeSpec.enumBuilder(name)
            .addDoc("%L\n", name)
            .addProperties(
                entries.map { entry ->
                    PropertySpec.builder(
                        name = entry.name.sanitise().toLowerCamelCase(),
                        type = typeName,
                        modifiers = arrayOf(Modifier.STATIC),
                    )
                        .mutable(false)
                        .addDoc("%L\n", entry.figmaPath)
                        .initializer(entry.value.initializer())
                        .build()
                }
            )
            .build()
    }

    private fun <V : VariableValue> createLightDarkType(
        name: String,
        entries: List<LightDarkEntry<V>>,
        typeName: TypeName,
    ): TypeSpec {
        val structTypeName = DeclaredTypeName.typeName(".$name")
        val safeNames = entries.associateWith { it.name.sanitise().toLowerCamelCase() }

        val instanceProperties = safeNames.values.mapIndexed { i, safeName ->
            PropertySpec.builder(safeName, typeName)
                .addDoc("%L\n", entries[i].figmaPath)
                .mutable(false)
                .build()
        }

        return TypeSpec.structBuilder(name)
            .addDoc("%L\n", name)
            .addProperties(instanceProperties)
            .addProperty(
                buildVariantProperty(
                    variantName = "light",
                    structTypeName = structTypeName,
                    safeNames = safeNames,
                    entries = entries,
                ) { it.light.value },
            )
            .addProperty(
                buildVariantProperty(
                    variantName = "dark",
                    structTypeName = structTypeName,
                    safeNames = safeNames,
                    entries = entries
                ) { it.dark.value },
            )
            .build()
    }

    private fun <V : VariableValue> buildVariantProperty(
        variantName: String,
        structTypeName: DeclaredTypeName,
        safeNames: Map<LightDarkEntry<V>, String>,
        entries: List<LightDarkEntry<V>>,
        valueSelector: (LightDarkEntry<V>) -> V,
    ): PropertySpec {
        val initializer = CodeBlock.builder()
            .add("%T(\n", structTypeName)
            .indent()
            .apply {
                entries.forEachIndexed { index, entry ->
                    add("%L: %L", safeNames.getValue(entry), valueSelector(entry).initializer())

                    if (index != entries.lastIndex) {
                        add(",\n")
                    }
                }
            }
            .unindent()
            .add("\n)")
            .build()

        return PropertySpec.builder(variantName, structTypeName)
            .addModifiers(Modifier.STATIC)
            .mutable(false)
            .initializer(initializer)
            .build()
    }

    private fun VariableValue.initializer(): CodeBlock = when (this) {
        is VariableValue.BooleanValue -> CodeBlock.of("%L", this.value)
        is VariableValue.NumberValue -> CodeBlock.of("%L", this.value)
        is VariableValue.StringValue -> CodeBlock.of("%S", value)
        is VariableValue.ColorValue -> colorInitializer(value)
    }

    private fun colorInitializer(color: Color): CodeBlock {
        return CodeBlock.builder()
            .add(
                "%T(red: %L, green: %L, blue: %L, opacity: %L)",
                colorTypeName,
                color.r.toFloat(),
                color.g.toFloat(),
                color.b.toFloat(),
                color.a.toFloat(),
            )
            .build()
    }

    private companion object {
        private val colorTypeName = DeclaredTypeName.typeName("SwiftUi.Color")
    }
}
