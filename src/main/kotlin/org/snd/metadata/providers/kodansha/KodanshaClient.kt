package org.snd.metadata.providers.kodansha

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.snd.infra.HttpClient
import org.snd.metadata.model.Image
import org.snd.metadata.providers.kodansha.model.KodanshaBook
import org.snd.metadata.providers.kodansha.model.KodanshaBookId
import org.snd.metadata.providers.kodansha.model.KodanshaBookListPage
import org.snd.metadata.providers.kodansha.model.KodanshaSearchResult
import org.snd.metadata.providers.kodansha.model.KodanshaSeries
import org.snd.metadata.providers.kodansha.model.KodanshaSeriesId

class KodanshaClient(
    private val client: HttpClient,
) {
    private val baseUrl: HttpUrl = "https://kodansha.us/".toHttpUrl()
    private val parser = KodanshaParser()

    fun searchSeries(name: String): Collection<KodanshaSearchResult> {
        val request = Request.Builder().url(
            baseUrl.newBuilder()
                .addQueryParameter("s", name)
                .addQueryParameter("filter_category[]", "manga")
                .build()
        ).build()

        return parser.parseSearchResults(client.execute(request))
    }

    fun getSeries(seriesId: KodanshaSeriesId): KodanshaSeries {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("series/${seriesId.id}")
                .build()
        ).build()

        return parser.parseSeries(client.execute(request))
    }

    fun getAllSeriesBooks(seriesId: KodanshaSeriesId, page: Int): KodanshaBookListPage {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("/manga/browse-volumes/page/$page/")
                .addQueryParameter("filter_series", seriesId.id)
                .build()
        ).build()

        return parser.parseBookListPage(client.execute(request))
    }

    fun getBook(bookId: KodanshaBookId): KodanshaBook {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("volume/${bookId.id}")
                .build()
        ).build()

        return parser.parseBook(client.execute(request))
    }

    fun getThumbnail(url: HttpUrl): Image {
        val request = Request.Builder().url(url).build()
        val bytes = client.executeWithByteResponse(request)

        return Image(bytes)
    }
}
