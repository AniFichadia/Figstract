package com.anifichadia.figstract.android.importer.variable.model.writer

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
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.ifC
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
        require(!outDirectory.exists() || outDirectory.isDirectory) {
            "outDirectory must be a directory: $outDirectory"
        }
    }

    override suspend fun write(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
        organizationStrategy: VariableOrganizationStrategy,
        collectionName: String,
        root: VariableGroup,
    ) {
        val fileSpec = FileSpec.builder(
            packageName = packageName,
            fileName = collectionName.sanitiseFileName().ToUpperCamelCase(),
        )
            .addType(createGroupType(root, isRoot = true))
            .build()

        outDirectory.mkdirs()
        fileSpec.writeTo(outDirectory)
    }

    private fun createGroupType(group: VariableGroup, isRoot: Boolean = false): TypeSpec {
        val typeName = if (isRoot) {
            group.name.sanitiseFileName().ToUpperCamelCase()
        } else {
            group.name.sanitise().ToUpperCamelCase()
        }

        return TypeSpec.objectBuilder(typeName)
            .addKdoc("%L", group.name)
            .apply {
                group.children.forEach { child -> addType(createGroupType(child)) }
                group.buckets.forEach { bucket -> addType(createBucketType(bucket)) }
            }
            .build()
    }

    private fun createBucketType(bucket: VariableTypeBucket): TypeSpec {
        return when (bucket) {
            is VariableTypeBucket.Single.Booleans ->
                createSingleType(bucket.name, bucket.entries, BOOLEAN)

            is VariableTypeBucket.Single.Numbers ->
                createSingleType(bucket.name, bucket.entries, DOUBLE)

            is VariableTypeBucket.Single.Strings ->
                createSingleType(bucket.name, bucket.entries, STRING)

            is VariableTypeBucket.Single.Colors ->
                createSingleType(bucket.name, bucket.entries, composeColorClassName)

            is VariableTypeBucket.LightAndDark.Booleans ->
                createLightDarkType(bucket.name, bucket.entries, BOOLEAN)

            is VariableTypeBucket.LightAndDark.Numbers ->
                createLightDarkType(bucket.name, bucket.entries, DOUBLE)

            is VariableTypeBucket.LightAndDark.Strings ->
                createLightDarkType(bucket.name, bucket.entries, STRING)

            is VariableTypeBucket.LightAndDark.Colors ->
                createLightDarkType(bucket.name, bucket.entries, composeColorClassName)
        }
    }

    private fun <V : VariableValue> createSingleType(
        name: String,
        entries: List<VariableEntry<V>>,
        typeName: TypeName,
    ): TypeSpec {
        return TypeSpec.objectBuilder(name)
            .addKdoc("%L", name)
            .addProperties(
                entries.map { entry ->
                    PropertySpec.builder(entry.name.sanitise().toLowerCamelCase(), typeName)
                        .addKdoc("%L", entry.figmaPath)
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
        val className = ClassName("", name)
        val safeNames = entries.associateWith { it.name.sanitise().toLowerCamelCase() }
        val properties = safeNames.values.mapIndexed { i, safeName ->
            PropertySpec.builder(safeName, typeName)
                .addKdoc("%L\n", entries[i].figmaPath)
                .initializer(safeName)
                .build()
        }

        return TypeSpec.classBuilder(className)
            .addKdoc("%L", name)
            .ifC(entries.isNotEmpty()) { addModifiers(KModifier.DATA) }
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(properties.map { ParameterSpec.builder(it.name, it.type).build() })
                    .build()
            )
            .addProperties(properties)
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addProperty(
                        buildVariantProperty(
                            variantName = "light",
                            className = className,
                            safeNames = safeNames,
                            entries = entries
                        ) { it.light.value },
                    )
                    .addProperty(
                        buildVariantProperty(
                            variantName = "dark",
                            className = className,
                            safeNames = safeNames,
                            entries = entries
                        ) { it.dark.value },
                    )
                    .build()
            )
            .build()
    }

    private fun <V : VariableValue> buildVariantProperty(
        variantName: String,
        className: ClassName,
        safeNames: Map<LightDarkEntry<V>, String>,
        entries: List<LightDarkEntry<V>>,
        valueSelector: (LightDarkEntry<V>) -> V,
    ): PropertySpec {
        return PropertySpec.builder(variantName, className)
            .initializer(
                CodeBlock.builder()
                    .add("%T(", className)
                    .withIndent {
                        addStatement("")
                        entries.forEach { entry ->
                            add("%L = ", safeNames[entry]!!)
                            add(valueSelector(entry).initializer())
                            addStatement(",")
                        }
                    }
                    .add(")")
                    .build()
            )
            .build()
    }

    private fun VariableValue.initializer(): CodeBlock = when (this) {
        is VariableValue.BooleanValue -> CodeBlock.of("%L", value)
        is VariableValue.NumberValue -> CodeBlock.of("%L", value)
        is VariableValue.StringValue -> CodeBlock.of("%S", value)
        is VariableValue.ColorValue -> colorInitializer(value)
    }

    private fun colorInitializer(color: Color): CodeBlock {
        return if (colorAsHex) {
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
    }

    private companion object {
        private val composeColorClassName = ClassName("androidx.compose.ui.graphics", "Color")
    }
}
