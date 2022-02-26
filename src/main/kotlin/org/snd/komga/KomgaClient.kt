package org.snd.komga

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.infra.HttpClient
import org.snd.infra.MEDIA_TYPE_JSON
import org.snd.komga.model.*

class KomgaClient(
    private val client: HttpClient,
    private val moshi: Moshi,
    private val baseUrl: HttpUrl,
) {

    fun getSeries(seriesId: SeriesId): Series {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun getSeries(libraryId: LibraryId, unpaged: Boolean): Page<Series> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series")
                    .addQueryParameter("unpaged", unpaged.toString())
                    .addQueryParameter("libraryId", libraryId.id)
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun updateSeriesMetadata(seriesId: SeriesId, metadata: SeriesMetadataUpdate) {
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

    fun updateSeriesThumbnail(seriesId: SeriesId, thumbnail: ByteArray) {
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "thumbnail", thumbnail.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/series/${seriesId.id}/thumbnails")
                    .build()
            )
            .post(requestBody)
            .build()

        client.execute(request)

    }

    fun getBook(bookId: BookId) {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/books/${bookId.id}")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun getBooks(seriesId: SeriesId, unpaged: Boolean): Page<Book> {
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

    fun updateBookMetadata(bookId: BookId, metadata: BookMetadataUpdate) {
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

    fun updateBookMetadataAsync(bookId: BookId, metadata: BookMetadataUpdate) {
        val postBody = toJson(metadata)
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/books/${bookId.id}/metadata")
                    .build()
            )
            .patch(postBody.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        client.executeAsync(request)
    }

    fun getLibraries(): Collection<Library> {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("api/v1/libraries")
                    .build()
            )
            .build()

        return parseJson(client.execute(request))
    }

    fun getLibrary(libraryId: LibraryId): Library {
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
        return moshi.adapter<T>().lenient().toJson(value)
    }
}
