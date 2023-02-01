package org.snd.metadata.providers.mangaupdates

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.infra.HttpClient
import org.snd.infra.MEDIA_TYPE_JSON
import org.snd.metadata.model.Image
import org.snd.metadata.providers.mangaupdates.model.SearchResultPage
import org.snd.metadata.providers.mangaupdates.model.Series
import org.snd.metadata.providers.mangaupdates.model.SeriesType

class MangaUpdatesClient(
    private val client: HttpClient,
    private val moshi: Moshi
) {
    private val baseUrl: HttpUrl = "https://api.mangaupdates.com/v1".toHttpUrl()

    fun searchSeries(
        name: String,
        types: Collection<SeriesType>,
        page: Int = 1,
        perPage: Int = 5,
    ): SearchResultPage {
        val payload = moshi.adapter<Map<String, *>>().toJson(
            mapOf(
                "search" to name,
                "page" to page,
                "perpage" to perPage,
                "type" to types.map { it.value }
            )
        )
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("series/search")
                .build()
        )
            .post(payload.toRequestBody(MEDIA_TYPE_JSON))
            .build()

        val response = client.execute(request)

        return moshi.adapter<SearchResultPage>().lenient().fromJson(response) ?: throw RuntimeException()
    }

    fun getSeries(seriesId: Long): Series {
        val seriesRequest = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("series/$seriesId")
                .build()
        ).build()

        val response = client.execute(seriesRequest)

        return moshi.adapter<Series>().lenient().fromJson(response) ?: throw RuntimeException()
    }

    fun getThumbnail(series: Series): Image? {
        return series.image?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Image(bytes)
        }
    }
}
