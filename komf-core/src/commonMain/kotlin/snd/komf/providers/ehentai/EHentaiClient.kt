package snd.komf.providers.ehentai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import snd.komf.model.Image
import snd.komf.providers.ehentai.model.EHentaiBook
import snd.komf.providers.ehentai.model.EHentaiResponse

class EHentaiClient(
    private val ktor: HttpClient
) {
    private val apiUrl: String = "https://api.e-hentai.org/api.php"
    private val baseUrl: String = "https://e-hentai.org"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun searchByGidList(
        gidList: List<Pair<Int, String>>
    ): EHentaiResponse {
        if (gidList.isEmpty()) return EHentaiResponse()

        /* Load limiting: 25 entries per request */
        val chunks = gidList.chunked(25)

        return coroutineScope {
            val responses = chunks.map { chunk ->
                async {
                    val responseText = ktor.post(apiUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(buildJsonObject {
                            put("method", "gdata")
                            putJsonArray("gidlist") {
                                chunk.forEach { (gid, token) ->
                                    addJsonArray {
                                        add(gid)
                                        add(token)
                                    }
                                }
                            }
                            put("namespace", 1)
                        })
                    }.bodyAsText()
                    json.decodeFromString<EHentaiResponse>(responseText)
                }
            }.awaitAll()

            EHentaiResponse(
                gmetadata = responses.flatMap { it.gmetadata },
                error = responses.firstNotNullOfOrNull { it.error }
            )
        }
    }

    suspend fun searchByTitle(
        title: String
    ): EHentaiResponse {
        val htmlResponse: String = ktor.get("$baseUrl/") {
            url { parameters.append("f_search", title) }
        }.bodyAsText()

        val galleryRegex = """/g/(\d+)/([a-f0-9]+)""".toRegex()
        val gidList = galleryRegex.findAll(htmlResponse)
            .map { matchResult ->
                val gid = matchResult.groupValues[1].toInt()
                val token = matchResult.groupValues[2]
                Pair(gid, token)
            }
            .distinct()
            .toList()

        if (gidList.isEmpty()) {
            return EHentaiResponse(error = "Empty gidList for search: $title")
        }

        return searchByGidList(gidList)
    }

    suspend fun getThumbnail(book: EHentaiBook): Image? {
        return book.thumb?.ifBlank { null }?.let { url ->
            val bytes = ktor.get(url).body<ByteArray>()
            Image(bytes)
        }
    }
}