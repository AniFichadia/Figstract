package com.anifichadia.figstract.importer.asset.model.importing.dsl

import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.passThrough
import com.anifichadia.figstract.importer.asset.model.importing.PNG_LOSSY_QUALITY_PERCENT_DEFAULT
import com.anifichadia.figstract.importer.asset.model.importing.WEBP_LOSSY_QUALITY_PERCENT_DEFAULT
import com.anifichadia.figstract.importer.asset.model.importing.convertToPngLossless
import com.anifichadia.figstract.importer.asset.model.importing.convertToPngLossy
import com.anifichadia.figstract.importer.asset.model.importing.convertToWebPLossless
import com.anifichadia.figstract.importer.asset.model.importing.convertToWebPLossy
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineStepRegistry.Companion.buildImportPipelineStepRegistry
import com.anifichadia.figstract.importer.asset.model.importing.pathElementsAppend
import com.anifichadia.figstract.importer.asset.model.importing.rename
import com.anifichadia.figstract.importer.asset.model.importing.renamePrefix
import com.anifichadia.figstract.importer.asset.model.importing.renameSuffix
import com.anifichadia.figstract.importer.asset.model.importing.scale
import com.anifichadia.figstract.importer.asset.model.importing.scaleToHeight
import com.anifichadia.figstract.importer.asset.model.importing.scaleToSize
import com.anifichadia.figstract.importer.asset.model.importing.scaleToWidth
import java.io.File

val CoreImportPipelineStepRegistry = buildImportPipelineStepRegistry {
    "passThrough" withFactory { passThrough() }

    // Scaling
    "scale" withFactory { params ->
        scale(params.value<Float>("scale"))
    }
    "scaleToSize" withFactory { params ->
        scaleToSize(
            width = params.value<Int>("width"),
            height = params.value<Int>("height"),
        )
    }
    "scaleToWidth" withFactory { params ->
        scaleToWidth(params.value<Int>("width"))
    }
    "scaleToHeight" withFactory { params ->
        scaleToHeight(params.value<Int>("height"))
    }

    // Naming
    "rename" withFactory { params ->
        rename(params.value<String>("name"))
    }
    "renameSuffix" withFactory { params ->
        renameSuffix(params.value<String>("suffix"))
    }
    "renamePrefix" withFactory { params ->
        renamePrefix(params.value<String>("prefix"))
    }

    // Path
    "pathElementsAppend" withFactory { params ->
        val elements = params.value<String>("pathElements")
            .split(",")
            .map { it.trim() }

        pathElementsAppend(elements)
    }

    // PNG
    "convertToPngLossless" withFactory { convertToPngLossless }
    "convertToPngLossy" withFactory { params ->
        convertToPngLossy(params.valueOrDefault<Int>("qualityPercent") { PNG_LOSSY_QUALITY_PERCENT_DEFAULT })
    }

    // WebP
    "convertToWebPLossless" withFactory { convertToWebPLossless }
    "convertToWebPLossy" withFactory { params ->
        convertToWebPLossy(params.valueOrDefault<Int>("qualityPercent") { WEBP_LOSSY_QUALITY_PERCENT_DEFAULT })
    }
}

fun destinationStepRegistry(
    baseDirectory: File = File("."),
): ImportPipelineStepRegistry = buildImportPipelineStepRegistry {
    "destinationNone" withFactory {
        Destination.None
    }

    "destinationDirectory" withFactory { params ->
        val path = params.value<String>("path")
        val resolved = baseDirectory.resolve(path)
        Destination.directoryDestination(resolved)
    }
}
