package snd.komf.providers.hentag

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import snd.komf.model.Image

class HentagClient(private val ktor: HttpClient) {
    private val baseUrl = "https://hentag.com/api/v1"

    suspend fun searchByTitle(
        title: String,
        language: String? = null
    ): List<HentagBook> {
        return ktor.post("$baseUrl/search/vault/title") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("title", title)
                language?.let { put("language", it) }
            })
        }.body()
    }

    suspend fun searchByUrls(
        urls: List<String>,
        language: String? = null
    ): List<HentagBook> {
        return ktor.post("$baseUrl/search/vault/url") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("urls") { urls.forEach { add(it) } }
                language?.let { put("language", it) }
            })
        }.body()
    }

    suspend fun searchByIds(
        ids: List<String>,
        language: String? = null
    ): List<HentagBook> {
        return ktor.post("$baseUrl/search/vault/id") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("ids") { ids.forEach { add(it) } }
                language?.let { put("language", it) }
            })
        }.body()
    }

    suspend fun getCover(series: HentagBook): Image? {
        return series.coverImageUrl?.let {
            val bytes = ktor.get(it).body<ByteArray>()
            Image(bytes)
        }
    }
}