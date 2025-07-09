package snd.komf.providers.webtoons

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.providers.webtoons.model.Episode
import snd.komf.providers.webtoons.model.EpisodeListApiResponse
import snd.komf.providers.webtoons.model.SearchApiResponse
import snd.komf.providers.webtoons.model.Title
import snd.komf.providers.webtoons.model.WebtoonsSeries
import snd.komf.providers.webtoons.model.WebtoonsSeriesId

class WebtoonsClient(private val ktor: HttpClient) {
    private val parser = WebtoonsParser()

    companion object {
        const val BASE_URL = "https://www.webtoons.com"
        const val MOBILE_BASE_URL = "https://m.webtoons.com"
        const val IMAGE_BASE_URL = "https://webtoon-phinf.pstatic.net"
    }

    private val baseHeaders: Headers = Headers.build {
        append(HttpHeaders.Referrer, "$BASE_URL/en")
    }

    private val mobileHeaders: Headers = Headers.build {
        append(HttpHeaders.Referrer, MOBILE_BASE_URL)
    }

    suspend fun searchSeries(name: String): Pair<Collection<Title>, Collection<Title>> {
        // Search separately since otherwise we only get three results of each
        // The combined mobile API returns only 3 of each. The desktop API only searches for originals
        //	https://m.webtoons.com/undefined/search/result?keyword=foo&searchType=WEBTOON&start=1
        //	https://m.webtoons.com/undefined/search/result?keyword=foo&searchType=CHALLENGE&start=1
        val webtoons: SearchApiResponse = ktor.get("$MOBILE_BASE_URL/undefined/search/result") { parameter("keyword", name); parameter("searchType", "WEBTOON"); headers { appendAll(mobileHeaders) } }.body()
        val originals: SearchApiResponse = ktor.get("$MOBILE_BASE_URL/undefined/search/result") { parameter("keyword", name); parameter("searchType", "CHALLENGE"); headers { appendAll(mobileHeaders) } }.body()

        return Pair(
            webtoons.result.webtoonResult?.titleList ?: emptyList(),
            originals.result.challengeResult?.titleList ?: emptyList()
        )
    }

    suspend fun getSeries(id: WebtoonsSeriesId): WebtoonsSeries {
        val document = ktor.get("$BASE_URL${id.value}") { headers { appendAll(baseHeaders) } }.bodyAsText()
        return parser.parseSeries(document)
    }

    suspend fun getChapters(id: WebtoonsSeriesId): Collection<Episode> {
        // https://m.webtoons.com/api/v1/webtoon/10101/episodes?pageSize=30
        val chaptersPath = if (id.value.contains("/canvas/")) "canvas" else "webtoon"
        val urlSegment = Url(id.value)
        val titleNo = urlSegment.parameters["title_no"] ?: urlSegment.parameters["titleNo"]
            ?: throw Exception("Expected either 'title_no' or 'titleNo' parameter to be present in the URL '${id}'")
        val episodes: EpisodeListApiResponse = ktor.get("$MOBILE_BASE_URL/api/v1/$chaptersPath/$titleNo/episodes") { parameter("pageSize", "99999"); headers { appendAll(mobileHeaders) } }.body()
        return episodes.result.episodeList
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

    suspend fun getChapterThumbnail(chapter: Episode): Image {
        val urlRaw = IMAGE_BASE_URL + chapter.thumbnail
        // Fetch at full quality, though here it doesn't seem to do much
        val builder = URLBuilder(urlRaw)
        builder.parameters.remove("type")
        val bytes: ByteArray = ktor.get(builder.build()) { headers { appendAll(mobileHeaders) } }.body()
        return Image(bytes)
    }
}