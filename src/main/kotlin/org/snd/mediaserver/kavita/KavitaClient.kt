package org.snd.mediaserver.kavita

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.infra.HttpClient
import org.snd.infra.MEDIA_TYPE_JSON
import org.snd.mediaserver.kavita.model.KavitaChapter
import org.snd.mediaserver.kavita.model.KavitaChapterId
import org.snd.mediaserver.kavita.model.KavitaChapterMetadata
import org.snd.mediaserver.kavita.model.KavitaCoverUploadRequest
import org.snd.mediaserver.kavita.model.KavitaLibrary
import org.snd.mediaserver.kavita.model.KavitaLibraryId
import org.snd.mediaserver.kavita.model.KavitaPage
import org.snd.mediaserver.kavita.model.KavitaSearch
import org.snd.mediaserver.kavita.model.KavitaSeries
import org.snd.mediaserver.kavita.model.KavitaSeriesId
import org.snd.mediaserver.kavita.model.KavitaSeriesMetadata
import org.snd.mediaserver.kavita.model.KavitaSeriesMetadataUpdate
import org.snd.mediaserver.kavita.model.KavitaSeriesUpdate
import org.snd.mediaserver.kavita.model.KavitaVolume
import org.snd.mediaserver.kavita.model.KavitaVolumeId
import org.snd.metadata.model.Image
import java.util.*

class KavitaClient(
    private val client: HttpClient,
    private val moshi: Moshi,
    private val baseUrl: HttpUrl,
) {
    fun getSeries(seriesId: KavitaSeriesId): KavitaSeries {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/${seriesId.id}")
                    .build()
            ).build()

        return parseJson(client.execute(request))
    }

    fun getSeries(libraryId: KavitaLibraryId, page: Int): KavitaPage<KavitaSeries> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series")
                    .addQueryParameter("libraryId", libraryId.id.toString())
                    .addQueryParameter("pageNumber", page.toString())
                    .addQueryParameter("pageSize", "500")
                    .build()
            )
            .post("{}".toRequestBody(MEDIA_TYPE_JSON))
            .build()

        val response = client.execute(request)
        val series = parseJson<Collection<KavitaSeries>>(response)
        return KavitaPage(series, page)
    }

    fun getSeriesCover(seriesId: KavitaSeriesId): ByteArray {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/image/series-cover")
                    .addQueryParameter("seriesId", seriesId.id.toString())
                    .build()
            )
            .build()

        return client.executeWithByteResponse(request)
    }

    fun updateSeries(seriesUpdate: KavitaSeriesUpdate) {
        val postBody = toJson(seriesUpdate)
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/update")
                    .build()
            )
            .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    fun updateSeriesMetadata(metadata: KavitaSeriesMetadataUpdate) {
        val postBody = toJson(metadata)
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/metadata")
                    .build()
            )
            .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }


    fun getSeriesMetadata(seriesId: KavitaSeriesId): KavitaSeriesMetadata {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/metadata")
                    .addQueryParameter("seriesId", seriesId.id.toString())
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun getVolumes(seriesId: KavitaSeriesId): Collection<KavitaVolume> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/volumes")
                    .addQueryParameter("seriesId", seriesId.id.toString())
                    .build()
            ).build()

        return parseJson(client.execute(request))
    }

    fun getVolume(volumeId: KavitaVolumeId): KavitaVolume {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/volume")
                    .addQueryParameter("volumeId", volumeId.id.toString())
                    .build()
            ).build()
        return parseJson(client.execute(request))
    }

    fun getChapter(chapterId: KavitaChapterId): KavitaChapter {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/chapter")
                    .addQueryParameter("chapterId", chapterId.id.toString())
                    .build()
            ).build()
        return parseJson(client.execute(request))
    }

    fun getChapterMetadata(chapterId: KavitaChapterId): KavitaChapterMetadata {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/chapter-metadata")
                    .addQueryParameter("chapterId", chapterId.id.toString())
                    .build()
            ).build()
        return parseJson(client.execute(request))

    }

    fun uploadSeriesCover(seriesId: KavitaSeriesId, cover: Image) {
        val base64Image = Base64.getEncoder().encodeToString(cover.image)
        val postBody = toJson(KavitaCoverUploadRequest(id = seriesId.id, url = base64Image))
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/upload/series")
                    .build()
            )
            .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    fun uploadBookCover(chapterId: KavitaChapterId, cover: Image) {
        val base64Image = Base64.getEncoder().encodeToString(cover.image)
        val postBody = toJson(KavitaCoverUploadRequest(id = chapterId.id, url = base64Image))
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/upload/chapter")
                    .build()
            )
            .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    fun getLibraries(): Collection<KavitaLibrary> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/library")
                    .build()
            ).build()

        return parseJson(client.execute(request))
    }

    fun search(query: String): KavitaSearch {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/search/search")
                    .addQueryParameter("queryString", query)
                    .build()
            ).build()

        return parseJson(client.execute(request))
    }

    fun scanSeries(seriesId: KavitaSeriesId) {
        val postBody = toJson(mapOf("seriesId" to seriesId.id))
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/series/scan")
                    .build()
            )
            .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    fun resetChapterLock(chapterId: KavitaChapterId) {
        val postBody = toJson(mapOf("id" to chapterId.id, "url" to ""))
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/upload/reset-chapter-lock")
                    .build()
            )
            .post(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    private inline fun <reified T> parseJson(json: String): T {
        return moshi.adapter<T>().lenient().fromJson(json) ?: throw RuntimeException()
    }

    private inline fun <reified T> toJson(value: T): String {
        return moshi.adapter<T>().lenient().serializeNulls().toJson(value)
    }
}
