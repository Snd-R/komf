package snd.komf.providers.comicvine

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.URLBuilder
import io.ktor.http.ParametersBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.text.Regex
import kotlinx.serialization.json.Json
import snd.komf.model.Image
import snd.komf.providers.comicvine.ComicVineClient.ComicVineTypeId.ISSUE
import snd.komf.providers.comicvine.ComicVineClient.ComicVineTypeId.VOLUME
import snd.komf.providers.comicvine.model.ComicVineIssue
import snd.komf.providers.comicvine.model.ComicVineIssueId
import snd.komf.providers.comicvine.model.ComicVineSearchResult
import snd.komf.providers.comicvine.model.ComicVineStoryArc
import snd.komf.providers.comicvine.model.ComicVineStoryArcId
import snd.komf.providers.comicvine.model.ComicVineVolume
import snd.komf.providers.comicvine.model.ComicVineVolumeId
import snd.komf.providers.comicvine.model.ComicVineVolumeSearch

private const val baseUrl = "https://comicvine.gamespot.com/api"

class ComicVineClient(
    private val ktor: HttpClient,
    private val apiKey: String,
    private val comicVineSearchLimit: Int? = 10,
    private val rateLimiter: ComicVineRateLimiter,
    private val cacheDatabaseFile: String,
) {
    private val cache = ComicVineCache(cacheDatabaseFile)

    private fun buildUrlString(
        url: String,
    ): String {
        val params = sortedMapOf(
            Pair("api_key", apiKey),
            Pair("format", "json"),
        )

        val encodedParams = params.entries.joinToString("&") { (key, value) ->
            val k = URLEncoder.encode(key, StandardCharsets.UTF_8)
            val v = URLEncoder.encode(value, StandardCharsets.UTF_8)
            "$k=$v"
        }

        return "$url?$encodedParams"
    }

    private suspend fun <T> getCachedApi(url: String): ComicVineSearchResult<T> {
        val fullUrl = buildUrlString(url)

        val cachedResult = cache.getEntry(fullUrl)

        if (cachedResult != null) {
            return Json.decodeFromString(cachedResult);
        }

        val response: ComicVineSearchResult<T> = ktor.get(fullUrl).body()

        cache.addEntry(fullUrl, Json.encodeToString(response))

        return response
    }

    suspend fun searchVolume(name: String): ComicVineSearchResult<List<ComicVineVolumeSearch>> {
        rateLimiter.searchAcquire()
        return ktor.get("$baseUrl/search/") {
            parameter("query", name)
            parameter("format", "json")
            parameter("resources", "volume")
            parameter("limit", comicVineSearchLimit)
            parameter("api_key", apiKey)
        }.body()
    }

    suspend fun getVolume(id: ComicVineVolumeId): ComicVineSearchResult<ComicVineVolume> {
        rateLimiter.volumeAcquire()
        return getCachedApi("$baseUrl/volume/${VOLUME.id}-${id.value}/")
    }

    suspend fun getIssue(id: ComicVineIssueId): ComicVineSearchResult<ComicVineIssue> {
        rateLimiter.issueAcquire()
        return getCachedApi("$baseUrl/issue/${ISSUE.id}-${id.value}/")
    }

    suspend fun getStoryArc(id: ComicVineStoryArcId): ComicVineSearchResult<ComicVineStoryArc> {
        rateLimiter.storyArcAcquire()
        return getCachedApi("$baseUrl/story_arc/${ComicVineTypeId.STORY_ARC.id}-${id.value}/")
    }

    suspend fun getCover(url: String): Image {
        rateLimiter.coverAcquire()
        val bytes: ByteArray = ktor.get(url).body()
        return Image(bytes)
    }

    private enum class ComicVineTypeId(val id: Int) {
        VOLUME(4050),
        ISSUE(4000),
        STORY_ARC(4045)
    }
}
