package com.anifichadia.figstract.ios.assetcatalog

import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.type.replaceOrAdd
import com.anifichadia.figstract.util.FileLockRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/index.html
 * https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/FolderStructure.html#//apple_ref/doc/uid/TP40015170-CH33-SW1
 */
class AssetCatalog(
    parentDirectory: File,
    assetsFileName: String = DEFAULT_ASSETS_FILE_NAME,
) {
    private val assetCatalogRootDirectory = File(parentDirectory, "${assetsFileName}.xcassets").also {
        it.mkdirs()
        writeAssetCatalogRootContent(it)
    }

    inline fun contentBuilder(
        groups: List<String>,
        fileLockRegistry: FileLockRegistry,
        block: ContentBuilder.() -> Unit,
    ) = this.apply {
        ContentBuilder(
            groups = groups,
            fileLockRegistry = fileLockRegistry,
        ).apply(block)
    }

    enum class GroupName(val directoryName: String) {
        Colors("Colors"),
        Images("Images"),
        ;
    }

    inner class ContentBuilder(
        groups: List<String>,
        private val fileLockRegistry: FileLockRegistry,
    ) {
        private val contentDirectory: File = groups
            .fold(assetCatalogRootDirectory) { acc, s ->
                acc.fold(s).also { prepareNamespaceDirectory(it) }
            }

        // https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/ImageSetType.html#//apple_ref/doc/uid/TP40015170-CH25-SW1
        suspend fun addImage(
            name: String,
            extension: String,
            content: ByteArray,
            type: Type.Image,
            scale: Scale,
            idiom: Content.Idiom = Content.Idiom.default,
        ) {
            val directory = File(contentDirectory, "${name}.${type.directorySuffix}").also {
                it.mkdirs()
            }
            val fileName = "${name}${scale.asFileSuffix()}.${extension}"
            val file = File(directory, fileName)
            file.writeBytes(content)

            contentFileOperation(directory) { contentToUpdate ->
                contentToUpdate.copy(
                    images = (contentToUpdate.images ?: emptyList())
                        .replaceOrAdd(
                            predicate = { it.scale == scale },
                            replacement = {
                                Content.Image(
                                    idiom = idiom,
                                    scale = scale,
                                    filename = fileName,
                                )
                            },
                        )
                        // Ensures file is deterministically generated
                        .sortedBy { it.scale }
                )
            }
        }

        suspend fun addColor(
            name: String,
            red: Float,
            green: Float,
            blue: Float,
            alpha: Float,
            appearances: List<Content.Color.Appearance>?,
            idiom: Content.Idiom = Content.Idiom.default,
        ) {
            val directory = File(contentDirectory, "${name}.${Type.Theme.ColorSet.directorySuffix}").also {
                it.mkdirs()
            }

            val color = Content.Color(
                color = Content.Color.ColorValue(
                    components = Content.Color.ColorValue.Components(
                        red = red,
                        green = green,
                        blue = blue,
                        alpha = alpha,
                    ),
                ),
                appearances = appearances,
                idiom = idiom,
            )

            contentFileOperation(directory) { contentToUpdate ->
                contentToUpdate.copy(
                    colors = (contentToUpdate.colors ?: emptyList())
                        .replaceOrAdd(
                            predicate = { it == color },
                            replacement = { color },
                        )
                        // Ensures file is deterministically generated
                        .sortedBy { it.idiom },
                )
            }
        }

        private suspend fun contentFileOperation(
            directory: File,
            update: (Content) -> Content,
        ) {
            val contentsFile = File(directory, Content.FILE_NAME)
            fileLockRegistry.withLock(contentsFile) {
                val contentToUpdate = if (contentsFile.exists()) {
                    assetCatalogJson.decodeFromString<Content>(contentsFile.readText())
                } else {
                    Content(info = Content.Info.xcode)
                }

                val updatedContent = update(contentToUpdate)

                contentsFile.writeText(assetCatalogJson.encodeToString(updatedContent))
            }
        }
    }

    companion object {
        const val DEFAULT_ASSETS_FILE_NAME = "Assets"

        private val NAMESPACE_CONTENT_PROPERTIES = Content.Properties(providesNamespace = true)

        /**
         * Standard [Json] instance used for asset catalogs
         */
        private val assetCatalogJson = Json {
            prettyPrint = true
        }

        private fun writeAssetCatalogRootContent(directory: File) {
            val file = File(directory, Content.FILE_NAME)
            if (file.exists()) return

            file.writeText(
                assetCatalogJson.encodeToString(
                    Content(
                        info = Content.Info.xcode,
                    )
                )
            )
        }

        private fun prepareNamespaceDirectory(directory: File) {
            directory.mkdirs()
            writeContentsFileForNamespacedDirectory(directory)
        }

        private fun writeContentsFileForNamespacedDirectory(
            directory: File,
        ) {
            val file = File(directory, Content.FILE_NAME)
            if (file.exists()) return

            file.writeText(
                assetCatalogJson.encodeToString(
                    Content(
                        info = Content.Info.xcode,
                        properties = NAMESPACE_CONTENT_PROPERTIES,
                    )
                )
            )
        }
    }
}
