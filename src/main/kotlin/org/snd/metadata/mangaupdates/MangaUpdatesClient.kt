package org.snd.metadata.mangaupdates

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.snd.infra.HttpClient
import org.snd.metadata.mangaupdates.model.SearchResult
import org.snd.metadata.mangaupdates.model.Series
import org.snd.metadata.model.Thumbnail

class MangaUpdatesClient(
    private val client: HttpClient,
) {
    private val baseUrl: HttpUrl = "https://www.mangaupdates.com".toHttpUrl()
    private val parser = MangaUpdatesParser()

    fun searchSeries(name: String): Collection<SearchResult> {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("series.html")
                .addQueryParameter("search", name)
                .build()
        ).build()

        return parser.parseSeriesSearch(client.execute(request))
    }

    fun getSeries(seriesId: String): Series {
        val seriesRequest = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("series/$seriesId")
                .build()
        ).build()

        val seriesResponse = client.execute(seriesRequest)

        return parser.parseSeries(seriesId, seriesResponse)
    }

    fun getThumbnail(series: Series): Thumbnail? {
        return series.image?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Thumbnail(bytes)
        }
    }
}
