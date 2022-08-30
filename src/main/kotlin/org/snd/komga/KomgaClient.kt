package org.snd.komga

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.EMPTY_REQUEST
import org.snd.infra.HttpClient
import org.snd.infra.MEDIA_TYPE_JSON
import org.snd.komga.model.dto.KomgaBook
import org.snd.komga.model.dto.KomgaBookId
import org.snd.komga.model.dto.KomgaBookMetadataUpdate
import org.snd.komga.model.dto.KomgaBookThumbnail
import org.snd.komga.model.dto.KomgaLibrary
import org.snd.komga.model.dto.KomgaLibraryId
import org.snd.komga.model.dto.KomgaSeries
import org.snd.komga.model.dto.KomgaSeriesId
import org.snd.komga.model.dto.KomgaSeriesMetadataUpdate
import org.snd.komga.model.dto.KomgaSeriesThumbnail
import org.snd.komga.model.dto.KomgaThumbnailId
import org.snd.komga.model.dto.Page
import org.snd.metadata.model.Thumbnail

class KomgaClient(
    private val client: HttpClient,
    private val moshi: Moshi,
    private val baseUrl: HttpUrl,
) {

    fun getSeries(seriesId: KomgaSeriesId): KomgaSeries {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun getSeries(libraryId: KomgaLibraryId, unpaged: Boolean, page: Int = 0): Page<KomgaSeries> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series")
                    .addQueryParameter("unpaged", unpaged.toString())
                    .addQueryParameter("library_id", libraryId.id)
                    .addQueryParameter("page", page.toString())
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun updateSeriesMetadata(seriesId: KomgaSeriesId, metadata: KomgaSeriesMetadataUpdate) {
        val postBody = toJson(metadata)
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}/metadata")
                    .build()
            )
            .patch(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    fun getSeriesThumbnails(seriesId: KomgaSeriesId): Collection<KomgaSeriesThumbnail> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}/thumbnails")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun uploadSeriesThumbnail(seriesId: KomgaSeriesId, thumbnail: Thumbnail, selected: Boolean = false): KomgaSeriesThumbnail {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "thumbnail", thumbnail.thumbnail.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}/thumbnails")
                    .addQueryParameter("selected", selected.toString())
                    .build()
            )
            .post(requestBody)
            .build()

        return parseJson(client.execute(request))
    }

    fun deleteSeriesThumbnail(seriesId: KomgaSeriesId, thumbnailId: KomgaThumbnailId) {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}/thumbnails/${thumbnailId.id}")
                    .build()
            ).delete()
            .build()

        client.execute(request)
    }

    fun searchSeries(name: String, page: Int, pageSize: Int): Page<KomgaSeries> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series")
                    .addQueryParameter("search", name)
                    .addQueryParameter("page", page.toString())
                    .addQueryParameter("pageSize", pageSize.toString())
                    .build()
            ).build()

        return parseJson(client.execute(request))
    }

    fun analyzeSeries(seriesId: KomgaSeriesId) {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}/analyze")
                    .build()
            ).post(EMPTY_REQUEST)
            .build()

        client.execute(request)
    }

    fun getBook(bookId: KomgaBookId): KomgaBook {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/books/${bookId.id}")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun getBooks(seriesId: KomgaSeriesId, unpaged: Boolean): Page<KomgaBook> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}/books")
                    .addQueryParameter("unpaged", unpaged.toString())
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun updateBookMetadata(bookId: KomgaBookId, metadata: KomgaBookMetadataUpdate) {
        val postBody = toJson(metadata)
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/books/${bookId.id}/metadata")
                    .build()
            )
            .patch(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.execute(request)
    }

    fun getBookThumbnails(bookId: KomgaBookId): Collection<KomgaBookThumbnail> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/books/${bookId.id}/thumbnails")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun uploadBookThumbnail(bookId: KomgaBookId, thumbnail: Thumbnail, selected: Boolean = false): KomgaBookThumbnail {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "thumbnail", thumbnail.thumbnail.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/books/${bookId.id}/thumbnails")
                    .addQueryParameter("selected", selected.toString())
                    .build()
            )
            .post(requestBody)
            .build()

        return parseJson(client.execute(request))
    }

    fun deleteBookThumbnail(bookId: KomgaBookId, thumbnailId: KomgaThumbnailId) {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/books/${bookId.id}/thumbnails/${thumbnailId.id}")
                    .build()
            ).delete()
            .build()

        client.execute(request)
    }

    fun getLibraries(): Collection<KomgaLibrary> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/libraries")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun getLibrary(libraryId: KomgaLibraryId): KomgaLibrary {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/libraries/${libraryId.id}")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    private inline fun <reified T : Any> parseJson(json: String): T {
        return moshi.adapter<T>().lenient().fromJson(json) ?: throw RuntimeException()
    }

    private inline fun <reified T : Any> toJson(value: T): String {
        return moshi.adapter<T>().lenient().serializeNulls().toJson(value)
    }
}
