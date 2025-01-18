package com.anifichadia.figstract.ios.assetcatalog

import com.anifichadia.figstract.type.replaceOrAdd
import com.anifichadia.figstract.util.FileLockRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AssetCatalog(
    parentDirectory: File,
    assetsFileName: String = DEFAULT_ASSETS_FILE_NAME,
) {
    private val assetCatalogRootDirectory = File(parentDirectory, "${assetsFileName}.xcassets").also {
        it.mkdirs()
    }

    inline fun contentBuilder(
        contentName: String,
        fileLockRegistry: FileLockRegistry,
        block: ContentBuilder.() -> Unit,
    ) = this.apply {
        ContentBuilder(contentName, fileLockRegistry).apply(block)
    }

    fun finalizeContents() {
        assetCatalogRootDirectory.mkdirs()
        writeAssetCatalogRootContent()
        ensureAssetCatalogSubdirectoriesHaveContentFiles()
    }

    private fun writeAssetCatalogRootContent() {
        File(assetCatalogRootDirectory, Content.FILE_NAME).writeText(
            assetCatalogJson.encodeToString(
                Content(
                    info = Content.Info.xcode,
                )
            )
        )
    }

    private fun ensureAssetCatalogSubdirectoriesHaveContentFiles(
        properties: Content.Properties = DEFAULT_CONTENT_PROPERTIES,
    ) {
        assetCatalogRootDirectory
            .walkTopDown()
            .filter { it.isDirectory }
            .forEach { subdirectory ->
                val hasContentFile = subdirectory
                    .listFiles { file -> file.name == Content.FILE_NAME }
                    ?.any()
                    ?: false

                if (!hasContentFile) {
                    writeAssetCatalogBlankContent(subdirectory, properties)
                }
            }
    }

    private fun writeAssetCatalogBlankContent(
        directory: File,
        properties: Content.Properties = DEFAULT_CONTENT_PROPERTIES,
    ) {
        File(directory, Content.FILE_NAME).writeText(
            assetCatalogJson.encodeToString(
                Content(
                    info = Content.Info.xcode,
                    properties = properties,
                )
            )
        )
    }

    inner class ContentBuilder(
        contentName: String,
        private val fileLockRegistry: FileLockRegistry,
    ) {
        private val contentDirectory = File(assetCatalogRootDirectory, contentName).also {
            it.mkdirs()
        }

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

        val DEFAULT_CONTENT_PROPERTIES = Content.Properties(providesNamespace = true)

        /**
         * Standard [Json] instance used for asset catalogs
         */
        private val assetCatalogJson = Json {
            prettyPrint = true
        }
    }
}
