package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.android.importer.asset.model.importing.dsl.AndroidImportPipelineStepRegistry
import com.anifichadia.figstract.importer.asset.model.importing.dsl.CoreImportPipelineStepRegistry
import com.anifichadia.figstract.ios.importer.asset.model.importing.dsl.IosImportPipelineStepRegistry

val CombinedStepRegistry = CoreImportPipelineStepRegistry +
    AndroidImportPipelineStepRegistry +
    IosImportPipelineStepRegistry
