package com.anifichadia.figstract.importer

import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.api.FigmaApi
import io.github.oshai.kotlinlogging.KLogger

suspend fun FigmaApi.getFileWithBranchName(
    key: FileKey,
    branchName: String,
    logger: KLogger,
): FileKey {
    logger.debug { "Resolving branch '$branchName' for $key" }

    val response = getFile(
        key = key,
        branchData = true,
    )

    val branches = response
        .successBodyOrThrow()
        .branches

    logger.debug { "Branches: $branches" }

    val branchKey = branches
        ?.firstOrNull { it.name == branchName }
        ?.key
        ?: error("Branch '$branchName' not found in $key")

    logger.debug { "Resolved branch '$branchName' to key: $branchKey" }

    return branchKey
}
