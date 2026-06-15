package com.anifichadia.figstract.ios.importer.asset.model.importing.dsl

import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineStepRegistry.Companion.buildImportPipelineStepRegistry
import com.anifichadia.figstract.ios.importer.asset.model.importing.HEIC_LOSSY_QUALITY_PERCENT_DEFAULT
import com.anifichadia.figstract.ios.importer.asset.model.importing.convertToHeic

val IosImportPipelineStepRegistry = buildImportPipelineStepRegistry {
    "convertToHeic" withFactory { params ->
        val qualityPercent = params.valueOrDefault<Int>("qualityPercent") { HEIC_LOSSY_QUALITY_PERCENT_DEFAULT }
        convertToHeic(qualityPercent)
    }
}
