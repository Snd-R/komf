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
    private val apiClient: HttpClient,
    private val imgClient: HttpClient
) {
    companion object {
        private const val API_URL = "https://api.e-hentai.org/api.php"
        private const val BASE_URL = "https://e-hentai.org"
        private const val MAX_PAGES = 3
        private val GALLERY_REGEX = """/g/(\d+)/([a-f0-9]+)""".toRegex()
        private val NEXT_URL_REGEX = """var\s+nexturl="([^"]+)"""".toRegex()

        private val JSON_PARSER = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
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
                    val responseText = apiClient.post(API_URL) {
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
                    JSON_PARSER.decodeFromString<EHentaiResponse>(responseText)
                }
            }.awaitAll()

            EHentaiResponse(
                gmetadata = responses.flatMap { it.gmetadata },
                error = responses.firstNotNullOfOrNull { it.error }
            )
        }
    }

    suspend fun searchByTitle(title: String): EHentaiResponse {
        val gidList = mutableListOf<Pair<Int, String>>()
        var currentUrl: String? = null

        var currentPage = 0
        while (currentPage < MAX_PAGES) {
            val htmlResponse = if (currentUrl == null) {
                apiClient.get(BASE_URL) {
                    url { parameters.append("f_search", title) }
                }.bodyAsText()
            } else {
                apiClient.get(currentUrl).bodyAsText()
            }

            val gidInPage = GALLERY_REGEX.findAll(htmlResponse)
                .map { it.groupValues[1].toInt() to it.groupValues[2] }
                .toList()

            if (gidInPage.isEmpty()) break
            gidList.addAll(gidInPage)

            currentUrl = NEXT_URL_REGEX.find(htmlResponse)
                ?.groupValues?.get(1)
                ?.replace("&amp;", "&") ?: break

            currentPage++
        }

        val distinctGidList = gidList.distinct()
        return when {
            distinctGidList.isNotEmpty() -> searchByGidList(distinctGidList)
            else -> EHentaiResponse(error = "Empty gidList for search: $title")
        }
    }

    suspend fun getThumbnail(book: EHentaiBook): Image? {
        return book.thumb?.ifBlank { null }?.let { url ->
            val bytes = imgClient.get(url).body<ByteArray>()
            Image(bytes)
        }
    }
}