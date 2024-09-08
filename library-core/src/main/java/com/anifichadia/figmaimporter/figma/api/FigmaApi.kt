package com.anifichadia.figmaimporter.figma.api

import com.anifichadia.figmaimporter.apiclient.ApiResponse
import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.figma.model.GetFilesResponse
import com.anifichadia.figmaimporter.figma.model.GetImageResponse
import com.anifichadia.figmaimporter.figma.model.GetLocalVariablesResponse

interface FigmaApi {
    suspend fun getFile(
        key: FileKey,
    ): ApiResponse<GetFilesResponse>

    /**
     * @param contentsOnly if not provided is defaulted to true by figma
     */
    suspend fun getImages(
        key: FileKey,
        ids: List<String>,
        format: ExportSetting.Format,
        scale: Float,
        contentsOnly: Boolean? = null,
    ): ApiResponse<GetImageResponse>

    suspend fun getLocalVariables(
        key: FileKey,
    ): ApiResponse<GetLocalVariablesResponse>
}
