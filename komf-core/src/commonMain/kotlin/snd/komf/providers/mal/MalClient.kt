package snd.komf.providers.mal

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import snd.komf.model.Image
import snd.komf.providers.mal.model.MalSearchResults
import snd.komf.providers.mal.model.MalSeries


class MalClient(private val ktor: HttpClient) {
    private val baseUrl = "https://api.myanimelist.net"
    private val includeFields: Set<String> = setOf(
        "id",
        "title",
        "main_picture",
        "alternative_titles",
        "start_date",
        "end_date",
        "synopsis",
        "mean",
        "rank",
        "popularity",
        "num_list_users",
        "num_scoring_users",
        "nsfw",
        "genres",
        "created_at",
        "updated_at",
        "media_type",
        "status",
        "num_volumes",
        "num_chapters",
        "authors{first_name,last_name}",
        "pictures",
        "background",
        "serialization{name}",
        "main_picture",
        "pictures"
    )

    suspend fun searchSeries(name: String): MalSearchResults {
        return ktor.get("$baseUrl/v2/manga") {
            parameter("fields", "alternative_titles,media_type")
            parameter("q", name)
            parameter("nsfw", "true")
        }.body()
    }

    suspend fun getSeries(id: Int): MalSeries {
        return ktor.get("$baseUrl/v2/manga/$id") {
            parameter("fields", includeFields.joinToString())
        }.body()
    }

    suspend fun getThumbnail(series: MalSeries): Image? {
        return series.mainPicture?.medium?.let {
            val bytes: ByteArray = ktor.get("it").body()
            Image(bytes)
        }
    }
}
