package snd.komf.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import snd.komf.api.KomfCoreProviders
import snd.komf.api.KomfProviderSeriesId
import snd.komf.api.KomfProviders
import snd.komf.api.KomfServerLibraryId
import snd.komf.api.KomfServerSeriesId
import snd.komf.api.MediaServer
import snd.komf.api.UnknownKomfProvider
import snd.komf.api.metadata.KomfIdentifyRequest
import snd.komf.api.metadata.KomfMetadataJobResponse
import snd.komf.api.metadata.KomfMetadataSeriesSearchResult

class KomfMetadataClient(
    private val ktor: HttpClient,
    mediaServer: MediaServer
) {
    private val metadataApiPrefix = "/api/${mediaServer.name.lowercase()}/metadata"

    suspend fun getProviders(): List<String> {
        return ktor.get("/api/metadata/providers").body()
    }

    suspend fun searchSeries(
        name: String,
        libraryId: KomfServerLibraryId? = null,
        seriesId: KomfServerSeriesId? = null
    ): List<KomfMetadataSeriesSearchResult> {
        return ktor.get("$metadataApiPrefix/search") {
            parameter("name", name)
            libraryId?.let { parameter("libraryId", libraryId) }
            seriesId?.let { parameter("seriesId", seriesId) }
        }.body()
    }

    suspend fun getSeriesCover(
        libraryId: KomfServerLibraryId,
        provider: KomfProviders,
        providerSeriesId: KomfProviderSeriesId
    ): ByteArray? {
        return try {
            ktor.get("$metadataApiPrefix/series-cover") {
                parameter("libraryId", libraryId)
                parameter(
                    "provider", when (provider) {
                        is UnknownKomfProvider -> provider.name
                        else -> provider.toString()
                    }
                )
                parameter("providerSeriesId", providerSeriesId)
            }.body()

        } catch (exception: ClientRequestException) {
            if (exception.response.status == HttpStatusCode.NotFound) {
                null
            } else {
                throw exception
            }
        }
    }

    suspend fun identifySeries(request: KomfIdentifyRequest): KomfMetadataJobResponse {
        return ktor.post("$metadataApiPrefix/identify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun matchSeries(
        libraryId: KomfServerLibraryId,
        seriesId: KomfServerSeriesId
    ): KomfMetadataJobResponse {
        return ktor.post("$metadataApiPrefix/match/library/$libraryId/series/$seriesId").body()
    }

    suspend fun matchLibrary(libraryId: KomfServerLibraryId) {
        ktor.post("$metadataApiPrefix/match/library/$libraryId")
    }

    suspend fun resetSeries(
        libraryId: KomfServerLibraryId,
        seriesId: KomfServerSeriesId,
        removeComicInfo: Boolean = false
    ) {
        ktor.post("$metadataApiPrefix/reset/library/$libraryId/series/$seriesId") {
            parameter("removeComicInfo", removeComicInfo)
        }
    }

    suspend fun resetLibrary(
        libraryId: KomfServerLibraryId,
        removeComicInfo: Boolean = false
    ) {
        ktor.post("$metadataApiPrefix/reset/library/$libraryId") {
            parameter("removeComicInfo", removeComicInfo)
        }
    }
}