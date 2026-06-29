package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.android.importer.asset.model.importing.dsl.AndroidImportPipelineStepRegistry
import com.anifichadia.figstract.importer.asset.model.importing.dsl.CoreImportPipelineStepRegistry
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineStepRegistry
import com.anifichadia.figstract.importer.asset.model.importing.dsl.destinationStepRegistry
import com.anifichadia.figstract.ios.importer.asset.model.importing.dsl.IosImportPipelineStepRegistry
import com.anifichadia.figstract.ios.importer.asset.model.importing.dsl.iosAssetCatalogStepRegistry
import com.anifichadia.figstract.util.FileManagement.outDirectory
import java.io.File

val CombinedStepRegistry = CoreImportPipelineStepRegistry +
    AndroidImportPipelineStepRegistry +
    IosImportPipelineStepRegistry

fun combinedRegistries(outDirectory: File): ImportPipelineStepRegistry {
    return CombinedStepRegistry +
        destinationStepRegistry(outDirectory) +
        iosAssetCatalogStepRegistry(outDirectory)
}
