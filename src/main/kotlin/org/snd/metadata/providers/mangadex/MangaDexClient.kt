package org.snd.metadata.providers.mangadex

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.snd.common.http.HttpClient
import org.snd.metadata.model.Image
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArt
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArtId
import org.snd.metadata.providers.mangadex.model.MangaDexManga
import org.snd.metadata.providers.mangadex.model.MangaDexMangaId
import org.snd.metadata.providers.mangadex.model.MangaDexPagedResponse
import org.snd.metadata.providers.mangadex.model.MangaDexResponse

val filesUrl: HttpUrl = "https://uploads.mangadex.org/".toHttpUrl()
private val apiUrl: HttpUrl = "https://api.mangadex.org".toHttpUrl()

class MangaDexClient(
    private val client: HttpClient,
    private val moshi: Moshi
) {

    fun searchSeries(
        title: String,
        limit: Int = 5,
        offset: Int = 0,
    ): MangaDexPagedResponse<List<MangaDexManga>> {
        val request = Request.Builder().url(
            apiUrl.newBuilder().addPathSegments("manga")
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("includes[]", "artist")
                .addQueryParameter("includes[]", "author")
                .addQueryParameter("includes[]", "cover_art")
                .addQueryParameter("order[relevance]", "desc")

                .addQueryParameter("title", title)
                .build()
        ).build()

        val response = client.execute(request)

        return moshi.adapter<MangaDexPagedResponse<List<MangaDexManga>>>().fromJson(response)
            ?: throw RuntimeException()
    }

    fun getSeries(mangaId: MangaDexMangaId): MangaDexManga {
        val request = Request.Builder().url(
            apiUrl.newBuilder().addPathSegments("manga/${mangaId.id}")
                .addQueryParameter("includes[]", "artist")
                .addQueryParameter("includes[]", "author")
                .addQueryParameter("includes[]", "cover_art")
                .build()
        ).build()

        val response = client.execute(request)

        return moshi.adapter<MangaDexResponse<MangaDexManga>>().fromJson(response)?.data
            ?: throw RuntimeException()
    }

    fun getSeriesCovers(
        mangaId: MangaDexMangaId,
        limit: Int = 100,
        offset: Int = 0
    ): MangaDexPagedResponse<List<MangaDexCoverArt>> {
        val request = Request.Builder().url(
            apiUrl.newBuilder().addPathSegments("cover")
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("offset", offset.toString())
                .addQueryParameter("manga[]", mangaId.id)
                .build()
        ).build()

        val response = client.execute(request)

        return moshi.adapter<MangaDexPagedResponse<List<MangaDexCoverArt>>>().fromJson(response)
            ?: throw RuntimeException()
    }

    fun getCover(coverId: MangaDexCoverArtId): MangaDexCoverArt {
        val request = Request.Builder()
            .url(apiUrl.newBuilder().addPathSegments("cover/${coverId.id}").build())
            .build()
        val response = client.execute(request)

        return moshi.adapter<MangaDexPagedResponse<MangaDexCoverArt>>().fromJson(response)?.data
            ?: throw RuntimeException()
    }

    fun getCover(mangaId: MangaDexMangaId, fileName: String): Image {
        val request = Request.Builder()
            .url(
                filesUrl.newBuilder()
                    .addPathSegments("covers/${mangaId.id}/$fileName.512.jpg")
                    .build()
            )
            .build()

        return Image(client.executeWithByteResponse(request))
    }
}
