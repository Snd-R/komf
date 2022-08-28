package org.snd.metadata.providers.nautiljon

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.snd.infra.HttpClient
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.providers.nautiljon.model.SearchResult
import org.snd.metadata.providers.nautiljon.model.Series
import org.snd.metadata.providers.nautiljon.model.SeriesId
import org.snd.metadata.providers.nautiljon.model.Volume
import org.snd.metadata.providers.nautiljon.model.VolumeId

class NautiljonClient(
    private val client: HttpClient,
) {
    private val baseUrl: HttpUrl = "https://www.nautiljon.com/".toHttpUrl()
    private val parser = NautiljonParser()

    fun searchSeries(name: String): Collection<SearchResult> {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("mangas")
                .addQueryParameter("q", name)
                .build()
        ).build()

        return parser.parseSearchResults(client.execute(request))
    }

    fun getSeries(seriesId: SeriesId): Series {
        val seriesRequest = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("mangas/${seriesId.id}.html")
                .build()
        ).build()

        val seriesResponse = client.execute(seriesRequest)

        return parser.parseSeries(seriesResponse)
    }

    fun getBook(seriesId: SeriesId, bookId: VolumeId): Volume {
        val seriesRequest = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("mangas/${seriesId.id}/volume-${bookId.id}.html")
                .build()
        ).build()

        val response = client.execute(seriesRequest)

        return parser.parseVolume(response)
    }

    fun getSeriesThumbnail(series: Series): Thumbnail? {
        return series.imageUrl?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Thumbnail(bytes)
        }
    }

    fun getVolumeThumbnail(volume: Volume): Thumbnail? {
        return volume.imageUrl?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Thumbnail(bytes)
        }
    }
}
