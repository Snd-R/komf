package snd.komf.providers.mangabaka.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.providers.mangabaka.remote.model.MangaBakaSearchResponse
import snd.komf.providers.mangabaka.remote.model.MangaBakaSeries
import snd.komf.providers.mangabaka.remote.model.MangaBakaSeriesId
import snd.komf.providers.mangabaka.remote.model.MangaBakaType

class MangaBakaClient(private val ktor: HttpClient) {
    private val baseUrl = "https://mangabaka.dev"

    suspend fun searchSeries(
        title: String,
        types: List<MangaBakaType>? = null,
        page: Int = 1,
    ): MangaBakaSearchResponse {
        return ktor.get("${baseUrl}/api/v1/series/search") {
            parameter("q", title)
            parameter("page", page.toString())
            types?.forEach { parameter("type", it.name.lowercase()) }
        }.body<MangaBakaSearchResponse>()
    }

    suspend fun getSeries(id: MangaBakaSeriesId): MangaBakaSeries {
        return ktor.get("${baseUrl}/api/v1/series/${id}.json").body<MangaBakaSeries>()
    }

    suspend fun getCoverBytes(url: String): Image? {
        val response = ktor.get(url)
        return Image(
            response.body(),
            response.contentType()?.let { "${it.contentType}/${it.contentSubtype}" }
        )
    }
}