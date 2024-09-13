package com.anifichadia.figstract.figma.api

import com.anifichadia.figstract.apiclient.AbstractApiClient
import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.apiclient.AuthProvider
import com.anifichadia.figstract.apiclient.Endpoint
import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.model.ExportSetting
import com.anifichadia.figstract.figma.model.GetFilesResponse
import com.anifichadia.figstract.figma.model.GetImageResponse
import com.anifichadia.figstract.figma.model.GetLocalVariablesResponse
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod

class FigmaApiImpl(
    httpClient: HttpClient,
    endpoint: Endpoint = figmaEndpoint,
    authProvider: AuthProvider,
) : AbstractApiClient(
    httpClient = httpClient,
    endpoint = endpoint,
    basePath = null,
    authProvider = authProvider,
), FigmaApi {
    /**
     * GET /v1/files/:key
     * https://www.figma.com/developers/api#get-files-endpoint
     */
    override suspend fun getFile(
        key: FileKey,
    ) = apiRequest<GetFilesResponse>(
        method = HttpMethod.Get,
        path = "/v1/files/$key",
        authenticated = true,
    )

    /**
     * GET /v1/images/:key?ids=1:2,1:3,1:4
     * https://www.figma.com/developers/api#get-images-endpoint
     */
    override suspend fun getImages(
        key: FileKey,
        ids: List<String>,
        format: ExportSetting.Format,
        scale: Float,
        contentsOnly: Boolean?,
    ) = apiRequest<GetImageResponse>(
        method = HttpMethod.Get,
        path = "/v1/images/$key",
        authenticated = true,
        queryParams = mapOf(
            "ids" to ids.joinToString(separator = ","),
            "format" to format,
            "scale" to scale,
            "contents_only" to contentsOnly,
        ),
    )

    /**
     * GET /v1/files/:file_key/variables/local
     * https://www.figma.com/developers/api#get-local-variables-endpoint
     */
    override suspend fun getLocalVariables(
        key: FileKey,
    ): ApiResponse<GetLocalVariablesResponse> = apiRequest<GetLocalVariablesResponse>(
        method = HttpMethod.Get,
        path = "/v1/files/${key}/variables/local",
        authenticated = true,
    )
}

val figmaEndpoint = Endpoint(
    scheme = "https",
    host = "api.figma.com",
)
