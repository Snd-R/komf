package org.snd.metadata.providers.yenpress

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.snd.common.exceptions.HttpException
import org.snd.common.http.HttpClient
import org.snd.common.http.MEDIA_TYPE_JSON
import org.snd.metadata.model.Image
import org.snd.metadata.providers.yenpress.model.YenPressBook
import org.snd.metadata.providers.yenpress.model.YenPressBookId
import org.snd.metadata.providers.yenpress.model.YenPressBookShort
import org.snd.metadata.providers.yenpress.model.YenPressMoreBooksResponse
import org.snd.metadata.providers.yenpress.model.YenPressSearchResult
import org.snd.metadata.providers.yenpress.model.YenPressSearchResults
import org.snd.metadata.providers.yenpress.model.YenPressSeriesId

const val yenPressBaseUrl = "https://yenpress.com/"

class YenPressClient(
    private val client: HttpClient,
    private val moshi: Moshi
) {
    private val baseUrl: HttpUrl = yenPressBaseUrl.toHttpUrl()
    private val searchUrl = "https://enterprise-search.yenpress.com/".toHttpUrl()
    private val parser = YenPressParser()

    private var searchKey = "search-kmcvvfh6ckq7dny1sqdnd1r9"

    fun searchSeries(name: String): List<YenPressSearchResult> {
        return try {
            search(name)
        } catch (e: HttpException.Unauthorized) {
            this.searchKey = fetchSearchKey()
            search(name)
        }
    }

    private fun search(name: String): List<YenPressSearchResult> {
        val request = Request.Builder()
            .url(
                searchUrl.newBuilder()
                    .addPathSegments("api/as/v1/engines/yenpress/search.json")
                    .build()
            )
            .post(constructQueryPayload(name).toRequestBody(MEDIA_TYPE_JSON))
            .header("Authorization", "Bearer $searchKey")
            .build()
        val response = client.execute(request)

        return moshi.adapter<YenPressSearchResults>().fromJson(response)?.results ?: throw RuntimeException()
    }

    fun getBookList(id: YenPressSeriesId): List<YenPressBookShort> {
        return generateSequence(getMoreBooks(id, 99999)) { it.nextOrd?.let { next -> getMoreBooks(id, next) } }
            .flatMap { it.books }
            .sortedBy { it.number?.start }
            .toList()
    }

    private fun getMoreBooks(id: YenPressSeriesId, nextOrd: Int): YenPressMoreBooksResponse {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("series/get_more/${id.id}")
                    .addQueryParameter("next_ord", nextOrd.toString())
                    .build()
            )
            .header("x-requested-with", "XMLHttpRequest")
            .build()

        val response = client.execute(request)

        return parser.parseMoreBooksResponse(response)
    }

    fun getBook(bookId: YenPressBookId): YenPressBook {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("titles/${bookId.id}").build())
            .build()

        val response = client.execute(request)

        return parser.parseBook(response, bookId)
    }

    fun getBookThumbnail(book: YenPressBook): Image? {
        return book.imageUrl?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Image(bytes)
        }
    }

    private fun fetchSearchKey(): String {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments("search").build())
            .build()

        val response = client.execute(request)
        return parser.parseSearchKey(response)
    }

    private fun constructQueryPayload(searchQuery: String): String {
        val query: Map<String, *> = mapOf(
            "query" to searchQuery,
            "filters" to mapOf("all" to listOf(mapOf("all" to mapOf("type" to "series")))),
            "precision" to 2,
            "search_fields" to mapOf(
                "title" to mapOf("weight" to 10),
            ),
            "result_fields" to mapOf(
                "title" to mapOf("raw" to emptyMap<Nothing, Nothing>()),
                "url" to mapOf("raw" to emptyMap()),
                "image" to mapOf("raw" to emptyMap())
            ),
            "page" to mapOf(
                "size" to 10,
                "current" to 1
            )
        )
        return moshi.adapter<Map<String, *>>().toJson(query)
    }
}
