package snd.komf.providers.yenpress

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import snd.komf.model.Image
import snd.komf.providers.yenpress.model.YenPressBook
import snd.komf.providers.yenpress.model.YenPressBookId
import snd.komf.providers.yenpress.model.YenPressBookShort
import snd.komf.providers.yenpress.model.YenPressMoreBooksResponse
import snd.komf.providers.yenpress.model.YenPressSearchResponse
import snd.komf.providers.yenpress.model.YenPressSearchResult
import snd.komf.providers.yenpress.model.YenPressSeriesId

const val yenPressBaseUrl = "https://yenpress.com/"

class YenPressClient(
    private val ktor: HttpClient,
) {
    private val searchUrl = "https://enterprise-search.yenpress.com/"
    private val parser = YenPressParser()

    private var searchKey = "search-vhfh3tijxttuxhjjmzgajcd4"

    suspend fun searchSeries(name: String): List<YenPressSearchResult> {
        return try {
            search(name)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                this.searchKey = fetchSearchKey()
                search(name)
            } else throw e
        }
    }

    private suspend fun search(name: String): List<YenPressSearchResult> {
        return ktor.post("$searchUrl/api/as/v1/engines/yenpress/search.json") {
            header("Authorization", "Bearer $searchKey")
            contentType(ContentType.Application.Json)
            setBody(constructQueryPayloadFor(name))
        }.body<YenPressSearchResponse>().results
    }

    suspend fun getBookList(id: YenPressSeriesId): List<YenPressBookShort> {
        val allBooks = mutableListOf<YenPressBookShort>()

        var nextOrd: Int? = 99999
        while (nextOrd != null) {
            val batch = getMoreBooks(id, 99999)
            allBooks.addAll(batch.books)
            nextOrd = batch.nextOrd
        }
        return allBooks
    }

    private suspend fun getMoreBooks(id: YenPressSeriesId, nextOrd: Int): YenPressMoreBooksResponse {
        val document = ktor.get("$yenPressBaseUrl/series/get_more/${id.value}") {
            parameter("next_ord", nextOrd)
            header("x-requested-with", "XMLHttpRequest")
        }.bodyAsText()

        return parser.parseMoreBooksResponse(document)
    }

    suspend fun getBook(bookId: YenPressBookId): YenPressBook {
        val document = ktor.get("$yenPressBaseUrl/titles/${bookId.value}").bodyAsText()
        return parser.parseBook(document, bookId)
    }

    suspend fun getBookThumbnail(book: YenPressBook): Image? {
        return book.imageUrl?.let { url ->
            val bytes: ByteArray = ktor.get(url).body()
            Image(bytes)
        }
    }

    private suspend fun fetchSearchKey(): String {
        val document = ktor.get("$yenPressBaseUrl/search").bodyAsText()
        return parser.parseSearchKey(document)
    }

    private fun constructQueryPayloadFor(searchQuery: String): JsonObject {
        return buildJsonObject {
            put("query", searchQuery)

            putJsonObject("filters") {
                putJsonArray("all") {
                    add(buildJsonObject {
                        putJsonArray("all") {
                            add(buildJsonObject { put("type", "series") })
                        }
                    })

                }
            }
            put("precision", 2)

            putJsonObject("search_fields") {
                putJsonObject("title") {
                    put("weight", 10)
                }
            }
            putJsonObject("result_fields") {
                putJsonObject("title") {
                    putJsonObject("raw") {}
                }
                putJsonObject("url") {
                    putJsonObject("raw") {}
                }
                putJsonObject("image") {
                    putJsonObject("raw") {}
                }
            }

            putJsonObject("page") {
                put("size", 10)
                put("current", 1)
            }
        }
    }

}
