package com.anifichadia.figstract.figma.api

import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.model.ExportSetting
import com.anifichadia.figstract.figma.model.GetFilesResponse
import com.anifichadia.figstract.figma.model.GetImageResponse
import com.anifichadia.figstract.figma.model.GetLocalVariablesResponse

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
