package org.snd.metadata.providers.mal

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.snd.common.http.HttpClient
import org.snd.metadata.model.Image
import org.snd.metadata.providers.mal.model.SearchResults
import org.snd.metadata.providers.mal.model.Series


class MalClient(
    private val client: HttpClient,
    private val moshi: Moshi
) {
    private val baseUrl = "https://api.myanimelist.net".toHttpUrl()
    private val includeFields: Set<String> = setOf(
        "id",
        "title",
        "main_picture",
        "alternative_titles",
        "start_date",
        "end_date",
        "synopsis",
        "mean",
        "rank",
        "popularity",
        "num_list_users",
        "num_scoring_users",
        "nsfw",
        "genres",
        "created_at",
        "updated_at",
        "media_type",
        "status",
        "num_volumes",
        "num_chapters",
        "authors{first_name,last_name}",
        "pictures",
        "background",
        "serialization{name}",
        "main_picture",
        "pictures"
    )

    fun searchSeries(name: String): SearchResults {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("v2/manga")
                .addQueryParameter("fields", "alternative_titles,media_type")
                .addQueryParameter("q", name)
                .addQueryParameter("nsfw", "true")
                .build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getSeries(id: Int): Series {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("v2/manga/$id")
                .addQueryParameter("fields", includeFields.joinToString()).build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getThumbnail(series: Series): Image? {
        return series.mainPicture?.medium?.let {
            val request = Request.Builder().url(it.toHttpUrl()).build()
            val bytes = client.executeWithByteResponse(request)
            Image(bytes)
        }
    }

    private inline fun <reified T : Any> parseJson(json: String): T {
        return moshi.adapter<T>().lenient().fromJson(json) ?: throw RuntimeException()
    }
}
