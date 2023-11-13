package com.anifichadia.figmaimporter.models.figma

import com.anifichadia.figmaimporter.apiclient.ApiResponse
import com.anifichadia.figmaimporter.apiclient.Endpoint

interface FigmaApi {
    suspend fun getFile(
        key: FileKey,
    ): ApiResponse<GetFilesResponse>

    suspend fun getImages(
        key: FileKey,
        ids: List<String>,
        format: ExportFormat,
        scale: Float,
    ): ApiResponse<GetImageResponse>
}
