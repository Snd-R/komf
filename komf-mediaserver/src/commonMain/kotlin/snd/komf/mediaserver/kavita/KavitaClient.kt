package snd.komf.mediaserver.kavita

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import snd.komf.mediaserver.kavita.model.KavitaChapter
import snd.komf.mediaserver.kavita.model.KavitaChapterId
import snd.komf.mediaserver.kavita.model.request.KavitaCoverUploadRequest
import snd.komf.mediaserver.kavita.model.KavitaLibrary
import snd.komf.mediaserver.kavita.model.KavitaLibraryId
import snd.komf.mediaserver.kavita.model.KavitaSeries
import snd.komf.mediaserver.kavita.model.KavitaSeriesDetails
import snd.komf.mediaserver.kavita.model.KavitaSeriesId
import snd.komf.mediaserver.kavita.model.KavitaSeriesMetadata
import snd.komf.mediaserver.kavita.model.request.KavitaSeriesMetadataUpdateRequest
import snd.komf.mediaserver.kavita.model.request.KavitaSeriesUpdateRequest
import snd.komf.mediaserver.kavita.model.KavitaVolume
import snd.komf.mediaserver.kavita.model.KavitaVolumeId
import snd.komf.mediaserver.kavita.model.request.KavitaChapterMetadataUpdateRequest
import snd.komf.model.Image
import java.util.*

class KavitaClient(
    private val ktor: HttpClient,
    private val json: Json,
    private val apiKey: String,
) {
    suspend fun getSeries(seriesId: KavitaSeriesId): KavitaSeries {
        return ktor.get("api/series/${seriesId.value}").body()
    }

    suspend fun getSeries(libraryId: KavitaLibraryId, page: Int): KavitaPage<KavitaSeries> {
        val response = ktor.post("api/series/v2") {
            parameter("pageNumber", page)
            parameter("pageSize", "500")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    putJsonArray("statements") {
                        add(
                            buildJsonObject {
                                put("field", 19)
                                put("value", libraryId.value.toString())
                                put("comparison", 0)
                            }
                        )
                    }

                }
            )
        }

        val pagination = response.headers["Pagination"]?.let { json.decodeFromString<KavitaPagination>(it) }
        return KavitaPage(
            content = response.body(),
            currentPage = pagination?.currentPage ?: page,
            itemsPerPage = pagination?.itemsPerPage,
            totalItems = pagination?.totalItems,
            totalPages = pagination?.totalPages
        )
    }

    suspend fun getSeriesCover(seriesId: KavitaSeriesId): Image {
        val response: HttpResponse = ktor.get("api/image/series-cover") {
            parameter("seriesId", seriesId.value)
            parameter("apiKey", apiKey)
        }
        val contentType = response.contentType()
        return Image(response.body<ByteArray>(), contentType?.toString())
    }

    suspend fun updateSeries(seriesUpdate: KavitaSeriesUpdateRequest) {
        ktor.post("api/series/update") {
            contentType(ContentType.Application.Json)
            setBody(seriesUpdate)
        }
    }

    suspend fun updateSeriesMetadata(metadata: KavitaSeriesMetadataUpdateRequest) {
        ktor.post("api/series/metadata") {
            contentType(ContentType.Application.Json)
            setBody(metadata)
        }
    }

    suspend fun updateChapterMetadata(metadata: KavitaChapterMetadataUpdateRequest) {
        ktor.post("api/chapter/update") {
            contentType(ContentType.Application.Json)
            setBody(metadata)
        }
    }


    suspend fun getSeriesMetadata(seriesId: KavitaSeriesId): KavitaSeriesMetadata {
        return ktor.get("api/series/metadata") {
            parameter("seriesId", seriesId.value)
        }.body()
    }

    suspend fun getSeriesDetails(seriesId: KavitaSeriesId): KavitaSeriesDetails {
        return ktor.get("api/series/series-detail") {
            parameter("seriesId", seriesId.value)
        }.body()
    }

    suspend fun getVolumes(seriesId: KavitaSeriesId): Collection<KavitaVolume> {
        return ktor.get("api/series/volumes") {
            parameter("seriesId", seriesId.value)
        }.body()
    }

    suspend fun getVolume(volumeId: KavitaVolumeId): KavitaVolume {
        val response = ktor.get("api/series/volume") {
            parameter("volumeId", volumeId.value)
        }
        if (response.status == HttpStatusCode.NoContent) throw snd.komf.mediaserver.kavita.KavitaResourceNotFoundException()

        return response.body()
    }

    suspend fun getChapter(chapterId: KavitaChapterId): KavitaChapter {
        val response = ktor.get("api/series/chapter") {
            parameter("chapterId", chapterId.value)
        }
        if (response.status == HttpStatusCode.NoContent) throw snd.komf.mediaserver.kavita.KavitaResourceNotFoundException()
        return response.body()
    }

    suspend fun getChapterCover(chapterId: KavitaChapterId): Image {
        val response = ktor.get("api/image/chapter-cover") {
            parameter("chapterId", chapterId.value)
            parameter("apiKey", apiKey)
        }

        val contentType = response.contentType()
        return Image(response.body(), contentType.toString())
    }

    suspend fun uploadSeriesCover(seriesId: KavitaSeriesId, cover: Image) {
        val base64Image = Base64.getEncoder().encodeToString(cover.bytes)
        ktor.post("api/upload/series") {
            contentType(ContentType.Application.Json)
            setBody(KavitaCoverUploadRequest(id = seriesId.value, url = base64Image, lockCover = false))
        }
    }

    suspend fun uploadVolumeCover(volumeId: KavitaVolumeId, cover: Image) {
        val base64Image = Base64.getEncoder().encodeToString(cover.bytes)
        ktor.post("api/upload/volume") {
            contentType(ContentType.Application.Json)
            setBody(KavitaCoverUploadRequest(id = volumeId.value, url = base64Image, lockCover = false))
        }
    }

    suspend fun getLibraries(): Collection<KavitaLibrary> {
        return ktor.get("api/library/libraries").body()
    }

    suspend fun scanSeries(seriesId: KavitaSeriesId) {
        ktor.post("api/series/scan") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("seriesId", seriesId.value) })

        }
    }

    suspend fun scanLibrary(libraryId: KavitaLibraryId) {
        ktor.post("api/library/scan") {
            parameter("libraryId", libraryId.value)
        }
    }

    suspend fun resetChapterLock(chapterId: KavitaChapterId) {
        ktor.post("api/upload/reset-chapter-lock") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("id", chapterId.value)
                put("url", "")
            })

        }
    }

}

data class KavitaPage<T>(
    val content: Collection<T>,
    val currentPage: Int,
    val itemsPerPage: Int?,
    val totalItems: Int?,
    val totalPages: Int?,
)

@Serializable
data class KavitaPagination(
    val currentPage: Int,
    val itemsPerPage: Int,
    val totalItems: Int,
    val totalPages: Int,
)