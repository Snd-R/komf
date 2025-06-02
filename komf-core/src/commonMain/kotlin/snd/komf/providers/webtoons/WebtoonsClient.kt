package snd.komf.providers.webtoons

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.providers.webtoons.model.SearchResult
import snd.komf.providers.webtoons.model.WebtoonsChapter
import snd.komf.providers.webtoons.model.WebtoonsSeries
import snd.komf.providers.webtoons.model.WebtoonsSeriesId

class WebtoonsClient(private val ktor: HttpClient) {
    private val parser = WebtoonsParser()

    private val baseUrl = "https://www.webtoons.com"
    private val mobileBaseUrl = "https://m.webtoons.com"

    private val baseHeaders: Headers = Headers.build {
        append(HttpHeaders.Referrer, "$baseUrl/en")
    }

    private val mobileHeaders: Headers = Headers.build {
        append(HttpHeaders.Referrer, mobileBaseUrl)
    }

    suspend fun searchSeries(name: String): Collection<SearchResult> {
        val document = ktor.get("$baseUrl/en/search") { parameter("keyword", name); headers { appendAll(baseHeaders) } }
            .bodyAsText()
        return parser.parseSearchResults(document)
    }

    suspend fun getSeries(id: WebtoonsSeriesId): WebtoonsSeries {
        val document = ktor.get("$baseUrl${id.value}") { headers { appendAll(baseHeaders) } }.bodyAsText()
        return parser.parseSeries(document)
    }

    private suspend fun getSeriesMobile(id: WebtoonsSeriesId): String {
        val document = ktor.get("$mobileBaseUrl${id.value}") { headers { appendAll(mobileHeaders) } }.bodyAsText()
        return document
    }

    suspend fun getChapters(id: WebtoonsSeriesId): Collection<WebtoonsChapter> {
        // The mobile html contains all the chapters in one single list, no paging needed
        val mobileSeries = getSeriesMobile(id)
        return parser.parseChapters(mobileSeries)
    }

    suspend fun getSeriesWithChapters(id: WebtoonsSeriesId): WebtoonsSeries {
        val series = getSeries(id)

        series.chapters = getChapters(id)
        return series
    }

    suspend fun getSeriesThumbnail(series: WebtoonsSeries): Image? {
        val urlRaw = series.thumbnailUrl ?: return null
        // Fetch at full quality
        val builder = URLBuilder(urlRaw)
        builder.parameters.remove("type")
        val bytes: ByteArray = ktor.get(builder.build()) { headers { appendAll(baseHeaders) } }.body()
        return Image(bytes)
    }

    suspend fun getChapterThumbnail(chapter: WebtoonsChapter): Image {
        val urlRaw = chapter.thumbnailUrl
        // Fetch at full quality, though here it doesn't seem to do much
        val builder = URLBuilder(urlRaw)
        builder.parameters.remove("type")
        val bytes: ByteArray = ktor.get(builder.build()) { headers { appendAll(mobileHeaders) } }.body()
        return Image(bytes)
    }
}