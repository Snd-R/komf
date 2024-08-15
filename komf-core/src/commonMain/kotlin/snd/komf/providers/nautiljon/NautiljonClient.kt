package snd.komf.providers.nautiljon

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import snd.komf.model.Image
import snd.komf.providers.nautiljon.model.SearchResult
import snd.komf.providers.nautiljon.model.NautiljonSeries
import snd.komf.providers.nautiljon.model.NautiljonSeriesId
import snd.komf.providers.nautiljon.model.NautiljonVolume
import snd.komf.providers.nautiljon.model.NautiljonVolumeId

const val nautiljonBaseUrl = "https://www.nautiljon.com"

class NautiljonClient(
    private val ktor: HttpClient,
) {
    private val parser = NautiljonParser()

    suspend fun searchSeries(name: String): Collection<SearchResult> {
        val document = ktor.get("$nautiljonBaseUrl/mangas") { parameter("q", name) }.bodyAsText()
        return parser.parseSearchResults(document)
    }

    suspend fun getSeries(seriesId: NautiljonSeriesId): NautiljonSeries {
        val document = ktor.get("$nautiljonBaseUrl/mangas/${seriesId.value}.html").bodyAsText()
        return parser.parseSeries(document)
    }

    suspend fun getBook(seriesId: NautiljonSeriesId, bookId: NautiljonVolumeId): NautiljonVolume {
        val document = ktor.get("$nautiljonBaseUrl/mangas/${seriesId.value}/volume-${bookId.value}.html").bodyAsText()
        return parser.parseVolume(document)
    }

    suspend fun getSeriesThumbnail(series: NautiljonSeries): Image? {
        val url = series.imageUrl ?: return null
        val bytes: ByteArray = ktor.get(url).body()
        return Image(bytes)
    }

    suspend fun getVolumeThumbnail(volume: NautiljonVolume): Image? {
        val url = volume.imageUrl ?: return null
        val bytes: ByteArray = ktor.get(url).body()
        return Image(bytes)
    }
}
