package org.snd.metadata.mangaupdates

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.snd.infra.HttpClient
import org.snd.metadata.mangaupdates.model.SearchResult
import org.snd.metadata.mangaupdates.model.Series
import org.snd.model.Thumbnail

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

    fun getSeries(seriesId: Int): Series {
        val seriesRequest = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("series.html")
                .addQueryParameter("id", seriesId.toString())
                .build()
        ).build()

        val seriesResponse = client.execute(seriesRequest)

        val categoriesRequest = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("ajax/show_categories.php")
                .addQueryParameter("s", seriesId.toString())
                .build()
        ).build()
        val categories = client.execute(categoriesRequest)

        return parser.parseSeries(seriesId, seriesResponse, categories)
    }

    fun getThumbnail(series: Series): Thumbnail? {
        return series.image?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Thumbnail(bytes)
        }
    }
}
