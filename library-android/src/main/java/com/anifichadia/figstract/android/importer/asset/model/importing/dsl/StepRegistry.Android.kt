package com.anifichadia.figstract.android.importer.asset.model.importing.dsl

import com.anifichadia.figstract.android.importer.asset.model.importing.androidSvgToAvd
import com.anifichadia.figstract.android.importer.asset.model.importing.androidVectorColorToPlaceholder
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineStepRegistry.Companion.buildImportPipelineStepRegistry

val AndroidImportPipelineStepRegistry = buildImportPipelineStepRegistry {
    "androidSvgToAvd" withFactory { androidSvgToAvd }
    "androidVectorColorToPlaceholder" withFactory { androidVectorColorToPlaceholder }
}
