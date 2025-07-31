package snd.komf.providers.comicvine

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
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
) {

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
        return ktor.get("$baseUrl/volume/${VOLUME.id}-${id.value}/") {
            parameter("format", "json")
            parameter("api_key", apiKey)
        }.body()
    }

    suspend fun getIssue(id: ComicVineIssueId): ComicVineSearchResult<ComicVineIssue> {
        rateLimiter.issueAcquire()
        return ktor.get("$baseUrl/issue/${ISSUE.id}-${id.value}/") {
            parameter("format", "json")
            parameter("api_key", apiKey)
        }.body()
    }

    suspend fun getStoryArc(id: ComicVineStoryArcId): ComicVineSearchResult<ComicVineStoryArc> {
        rateLimiter.storyArcAcquire()
        return ktor.get("$baseUrl/story_arc/${ComicVineTypeId.STORY_ARC.id}-${id.value}/") {
            parameter("format", "json")
            parameter("api_key", apiKey)
        }.body()

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
