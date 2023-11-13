package com.anifichadia.figmaimporter.figma.api

import com.anifichadia.figmaimporter.apiclient.ApiResponse
import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.figma.model.GetFilesResponse
import com.anifichadia.figmaimporter.figma.model.GetImageResponse

interface FigmaApi {
    suspend fun getFile(
        key: FileKey,
    ): ApiResponse<GetFilesResponse>

    suspend fun getImages(
        key: FileKey,
        ids: List<String>,
        format: ExportSetting.Format,
        scale: Float,
    ): ApiResponse<GetImageResponse>
}
