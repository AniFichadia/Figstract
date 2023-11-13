package com.anifichadia.figmaimporter.figma.api

import com.anifichadia.figmaimporter.apiclient.AbstractApiClient
import com.anifichadia.figmaimporter.apiclient.AuthProvider
import com.anifichadia.figmaimporter.apiclient.Endpoint
import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.figma.model.GetFilesResponse
import com.anifichadia.figmaimporter.figma.model.GetImageResponse
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
    ) = apiRequest<GetImageResponse>(
        method = HttpMethod.Get,
        path = "/v1/images/$key",
        authenticated = true,
        queryParams = mapOf(
            "ids" to ids.joinToString(separator = ","),
            "format" to format,
            "scale" to scale,
        )
    )
}

val figmaEndpoint = Endpoint(
    scheme = "https",
    host = "api.figma.com",
)
