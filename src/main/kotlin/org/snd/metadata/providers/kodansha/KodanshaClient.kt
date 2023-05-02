package org.snd.metadata.providers.kodansha

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.snd.common.http.HttpClient
import org.snd.metadata.model.Image
import org.snd.metadata.providers.kodansha.model.KodanshaBook
import org.snd.metadata.providers.kodansha.model.KodanshaBookId
import org.snd.metadata.providers.kodansha.model.KodanshaResponse
import org.snd.metadata.providers.kodansha.model.KodanshaSearchResult
import org.snd.metadata.providers.kodansha.model.KodanshaSeries
import org.snd.metadata.providers.kodansha.model.KodanshaSeriesId

const val kodanshaBaseUrl = "https://kodansha.us/"

class KodanshaClient(
    private val client: HttpClient,
    private val moshi: Moshi,
) {
    private val apiUrl = "https://api.kodansha.us".toHttpUrl()

    fun search(name: String): KodanshaResponse<List<KodanshaSearchResult>> {
        val request = Request.Builder().url(
            apiUrl.newBuilder()
                .addPathSegments("search/V3")
                .addQueryParameter("query", name)
                .build()
        ).build()

        val response = client.execute(request)
        return parseJson(response)
    }

    fun getSeries(seriesId: KodanshaSeriesId): KodanshaSeries {
        val request = Request.Builder().url(
            apiUrl.newBuilder().addPathSegments("series/V2/${seriesId.id}")
                .build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getAllSeriesBooks(seriesId: KodanshaSeriesId): List<KodanshaBook> {
        val request = Request.Builder().url(
            apiUrl.newBuilder()
                .addPathSegments("product/forSeries/${seriesId.id}")
                .build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getBook(bookId: KodanshaBookId): KodanshaResponse<KodanshaBook> {
        val request = Request.Builder().url(
            apiUrl.newBuilder().addPathSegments("product/${bookId.id}")
                .build()
        ).build()

        return parseJson(client.execute(request))
    }

    fun getThumbnail(url: HttpUrl): Image {
        val request = Request.Builder().url(url).build()
        val bytes = client.executeWithByteResponse(request)

        return Image(bytes)
    }

    private inline fun <reified T : Any> parseJson(json: String): T {
        return moshi.adapter<T>().lenient().fromJson(json) ?: throw RuntimeException("Could not parse Json")
    }
}

