package snd.komf.providers.mangadex

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import snd.komf.model.Image
import snd.komf.providers.mangadex.model.MangaDexCoverArt
import snd.komf.providers.mangadex.model.MangaDexManga
import snd.komf.providers.mangadex.model.MangaDexMangaId
import snd.komf.providers.mangadex.model.MangaDexPagedResponse
import snd.komf.providers.mangadex.model.MangaDexResponse

const val filesUrl: String = "https://uploads.mangadex.org"
private const val apiUrl: String = "https://api.mangadex.org"

class MangaDexClient(private val ktor: HttpClient) {

    suspend fun searchSeries(
        title: String,
        limit: Int = 5,
        offset: Int = 0,
    ): MangaDexPagedResponse<List<MangaDexManga>> {
        return ktor.get("$apiUrl/manga") {
            parameter("limit", limit.toString())
            parameter("offset", offset.toString())
            parameter("includes[]", "artist")
            parameter("includes[]", "author")
            parameter("includes[]", "cover_art")
            parameter("order[relevance]", "desc")
            parameter("contentRating[]", "safe")
            parameter("contentRating[]", "suggestive")
            parameter("contentRating[]", "erotica")
            parameter("contentRating[]", "pornographic")
            parameter("title", title)
        }.body()
    }

    suspend fun getSeries(mangaId: MangaDexMangaId): MangaDexManga {
        val response: MangaDexResponse<MangaDexManga> = ktor.get("$apiUrl/manga/${mangaId.value}") {
            parameter("includes[]", "artist")
            parameter("includes[]", "author")
            parameter("includes[]", "cover_art")
        }.body()

        return response.data
    }

    suspend fun getSeriesCovers(
        mangaId: MangaDexMangaId,
        limit: Int = 100,
        offset: Int = 0
    ): MangaDexPagedResponse<List<MangaDexCoverArt>> {
        return ktor.get("$apiUrl/cover") {
            parameter("limit", limit.toString())
            parameter("offset", offset.toString())
            parameter("manga[]", mangaId.value)
        }.body()
    }

    suspend fun getCover(coverId: String): MangaDexCoverArt {
        val response: MangaDexPagedResponse<MangaDexCoverArt> = ktor.get("$apiUrl/cover/$coverId").body()

        return response.data
    }

    suspend fun getCover(mangaId: MangaDexMangaId, fileName: String): Image {
        val bytes = ktor.get("$filesUrl/covers/${mangaId.value}/$fileName.512.jpg")
            .body<ByteArray>()
        return Image(bytes)
    }
}
