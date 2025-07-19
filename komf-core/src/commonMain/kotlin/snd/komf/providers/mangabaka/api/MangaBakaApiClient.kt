package snd.komf.providers.mangabaka.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import snd.komf.providers.mangabaka.MangaBakaDataSource
import snd.komf.providers.mangabaka.MangaBakaSeries
import snd.komf.providers.mangabaka.MangaBakaSeriesId
import snd.komf.providers.mangabaka.MangaBakaType

class MangaBakaApiClient(private val ktor: HttpClient) : MangaBakaDataSource {
    private val baseUrl = "https://api.mangabaka.dev"

    override suspend fun search(title: String, types: List<MangaBakaType>?): List<MangaBakaSeries> {
        return ktor.get("${baseUrl}/v1/series/search") {
            parameter("q", title)
            types?.forEach { parameter("type", it.name.lowercase()) }
        }.body<MangaBakaSearchResponse>().results
    }

    override suspend fun getSeries(id: MangaBakaSeriesId): MangaBakaSeries {
        return ktor.get("${baseUrl}/v1/series/${id}").body<MangaBakaResponse>().data
    }
}