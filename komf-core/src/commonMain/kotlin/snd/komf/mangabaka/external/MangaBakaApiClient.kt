package snd.komf.mangabaka.external

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import snd.komf.mangabaka.model.MangaBakaContentRating
import snd.komf.mangabaka.model.MangaBakaResponse
import snd.komf.mangabaka.model.MangaBakaSearchResponse
import snd.komf.mangabaka.model.MangaBakaSeries
import snd.komf.mangabaka.model.MangaBakaSeriesId
import snd.komf.mangabaka.model.MangaBakaSeriesTag
import snd.komf.mangabaka.model.MangaBakaTagId
import snd.komf.mangabaka.model.MangaBakaType
import snd.komf.providers.mangabaka.MangaBakaDataSource

class MangaBakaApiClient(private val ktor: HttpClient) : MangaBakaDataSource {
    private val baseUrl = "https://api.mangabaka.org"

    override suspend fun search(
        title: String,
        types: List<MangaBakaType>?,
        typesNot: List<MangaBakaType>?
    ): List<MangaBakaSeries> {
        return ktor.get("${baseUrl}/v1/series/search") {
            parameter("q", title)
            types?.forEach { parameter("type", it.name.lowercase()) }
            typesNot?.forEach { parameter("type_not", it.name.lowercase()) }
        }.body<MangaBakaSearchResponse>().data
    }

    override suspend fun getSeries(id: MangaBakaSeriesId): MangaBakaSeries {
        return ktor.get("${baseUrl}/v1/series/${id}").body<MangaBakaResponse<MangaBakaSeries>>().data
    }

    suspend fun getTags(
        query: String? = null,
        ids: List<MangaBakaTagId>? = null,
        parent: MangaBakaTagId? = null,
        depth: Int? = null,
        contentRatings: List<MangaBakaContentRating>? = null,
        isGenre: Boolean? = null,
        page: Int? = null,
        limit: Int? = null
    ): List<MangaBakaSeriesTag> {
        val response: MangaBakaResponse<List<MangaBakaSeriesTag>> = ktor.get("${baseUrl}/v1/tags") {
            query?.let { parameter("q", it) }
            ids?.forEach { parameter("id", it.value) }
            parent?.let { parameter("parent", it.value) }
            depth?.let { parameter("depth", it) }
            contentRatings?.forEach { parameter("content_rating", it.name.lowercase()) }
            isGenre?.let { parameter("is_genre", it) }
            page?.let { parameter("page", it) }
            limit?.let { parameter("limit", it) }
        }.body()
        return response.data
    }
}