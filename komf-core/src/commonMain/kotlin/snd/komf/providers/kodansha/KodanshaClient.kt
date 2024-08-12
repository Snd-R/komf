package snd.komf.providers.kodansha

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import snd.komf.model.Image
import snd.komf.providers.kodansha.model.KodanshaBook
import snd.komf.providers.kodansha.model.KodanshaBookId
import snd.komf.providers.kodansha.model.KodanshaResponse
import snd.komf.providers.kodansha.model.KodanshaSearchResult
import snd.komf.providers.kodansha.model.KodanshaSeries
import snd.komf.providers.kodansha.model.KodanshaSeriesId

const val kodanshaBaseUrl = "https://kodansha.us/"

class KodanshaClient(private val ktor: HttpClient) {
    private val apiUrl = "https://api.kodansha.us"

    suspend fun search(name: String): KodanshaResponse<List<KodanshaSearchResult>> {
        return ktor.get("$apiUrl/search/V3") {
            parameter("query", name)
        }.body()
    }

    suspend fun getSeries(seriesId: KodanshaSeriesId): KodanshaResponse<KodanshaSeries> {
        return ktor.get("$apiUrl/series/V2/${seriesId.id}").body()
    }

    suspend fun getAllSeriesBooks(seriesId: KodanshaSeriesId): List<KodanshaBook> {
        return ktor.get("$apiUrl/product/forSeries/${seriesId.id}").body()
    }

    suspend fun getBook(bookId: KodanshaBookId): KodanshaResponse<KodanshaBook> {
        return ktor.get("$apiUrl/product/${bookId.id}").body()
    }

    suspend fun getThumbnail(url: String): Image {
        val bytes: ByteArray = ktor.get(url).body()
        return Image(bytes)
    }
}

