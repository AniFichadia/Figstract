package com.anifichadia.figmaimporter.android.model.importing

import com.anifichadia.figmaimporter.android.model.drawable.DensityBucket
import com.anifichadia.figmaimporter.importer.asset.model.importing.Destination
import com.anifichadia.figmaimporter.importer.asset.model.importing.Destination.Companion.directoryDestination
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.importer.asset.model.importing.convertToWebPLossy
import com.anifichadia.figmaimporter.importer.asset.model.importing.pathElementsAppend
import com.anifichadia.figmaimporter.importer.asset.model.importing.scale
import java.io.File

/**
 * Scales from [sourceDensity], converts to webp, and organises outputs into density bucketted folders.
 *
 * Note: Make sure the destination is set to [Destination.None]
 *
 * @param densityBuckets Allows configuring the output [DensityBucket]s. By default, this uses all [DensityBucket]s
 * except [DensityBucket.LDPI] since it's less commonly used
 */
fun androidImageScaleAndStoreInDensityBuckets(
    imageDirectory: File,
    sourceDensity: DensityBucket,
    densityBuckets: List<DensityBucket> = DensityBucket.entries.filter { it != DensityBucket.LDPI },
): ImportPipeline.Step {
    return densityBuckets
        .map { targetDensity -> androidImageScaleAndStoreInDensityBucket(imageDirectory, sourceDensity, targetDensity) }
        .and()
}

fun androidImageScaleAndStoreInDensityBucket(
    imageDirectory: File,
    sourceDensity: DensityBucket,
    targetDensity: DensityBucket,
): ImportPipeline.Step {
    return scale(sourceDensity.scaleRelativeTo(targetDensity)) then
        convertToWebPLossy() then
        androidCreateDensityBucketPath(targetDensity) then
        directoryDestination(imageDirectory)
}

fun androidCreateDensityBucketPath(
    density: DensityBucket,
): ImportPipeline.Step {
    return pathElementsAppend("drawable-${density.suffix}")
}
