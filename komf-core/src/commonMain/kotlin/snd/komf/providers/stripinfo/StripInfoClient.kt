package snd.komf.providers.stripinfo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import snd.komf.model.Image
import snd.komf.providers.stripinfo.model.StripInfoAlbum
import snd.komf.providers.stripinfo.model.StripInfoAlbumId
import snd.komf.providers.stripinfo.model.StripInfoSearchResult
import snd.komf.providers.stripinfo.model.StripInfoSeries
import snd.komf.providers.stripinfo.model.StripInfoSeriesId

const val stripInfoBaseUrl = "https://www.stripinfo.be"

class StripInfoClient(
    private val ktor: HttpClient,
) {
    private val parser = StripInfoParser()

    suspend fun searchSeries(name: String): List<StripInfoSearchResult> {
        val html = ktor.get("$stripInfoBaseUrl/zoek/zoek") {
            parameter("zoekstring", name)
            parameter("zoektype", "reeks")
        }.bodyAsText()
        return parser.parseSearchResults(html)
    }

    suspend fun getSeries(seriesId: StripInfoSeriesId): StripInfoSeries {
        val html = ktor.get("$stripInfoBaseUrl/reeks/index/${seriesId.id}").bodyAsText()
        return parser.parseSeries(html, seriesId)
    }

    suspend fun getAlbum(albumId: StripInfoAlbumId): StripInfoAlbum {
        val html = ktor.get("$stripInfoBaseUrl/reeks/strip/${albumId.id}").bodyAsText()
        return parser.parseAlbum(html, albumId)
    }

    suspend fun getCover(url: String): Image? {
        return try {
            val bytes: ByteArray = ktor.get(url).body()
            Image(bytes)
        } catch (_: Exception) {
            null
        }
    }
}
