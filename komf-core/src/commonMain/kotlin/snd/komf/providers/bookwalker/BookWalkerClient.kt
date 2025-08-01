package snd.komf.providers.bookwalker

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import snd.komf.model.Image
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerBookInfo
import snd.komf.providers.bookwalker.model.BookWalkerBookListPage
import snd.komf.providers.bookwalker.model.BookWalkerCategory
import snd.komf.providers.bookwalker.model.BookWalkerSearchResult
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId

const val bookWalkerBaseUrl = "https://global.bookwalker.jp"

class BookWalkerClient(
    ktor: HttpClient,
    json: Json
) {
    private val parser = BookWalkerParser()
    private val apiClient = ktor.config { install(ContentNegotiation) { json(json) } }
    private val htmlClient = ktor.config {
        defaultRequest {
            cookie("safeSearch", "111")
            cookie("glSafeSearch", "1")
            cookie("mySetting/showCoverR15", "1")
        }
    }

    suspend fun searchSeries(name: String, category: BookWalkerCategory): Collection<BookWalkerSearchResult> {

        return try {
            val document = htmlClient.get("$bookWalkerBaseUrl/search/") {
                parameter("word", name)
                parameter("qcat", category.number)
                parameter("np", 0)
            }.bodyAsText()
            parser.parseSearchResults(document)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.NotFound) emptyList()
            else throw e
        }

    }

    suspend fun getSeriesBooks(id: BookWalkerSeriesId, page: Int): BookWalkerBookListPage {
        val document = htmlClient.get("$bookWalkerBaseUrl/series/${id.id}/") {
            parameter("page", page)
        }.bodyAsText()
        return parser.parseSeriesBooks(document)
    }

    suspend fun getBook(id: BookWalkerBookId): BookWalkerBook {
        val document = htmlClient.get("$bookWalkerBaseUrl/${id.id}/").bodyAsText()
        return parser.parseBook(document)
    }

    suspend fun getBookApi(bookId: BookWalkerBookId): BookWalkerBookInfo {
        val result: List<BookWalkerBookInfo> = apiClient.get("https://member-app.bookwalker.jp/api/books/updates") {
            parameter("fileType", "EPUB")
            parameter(bookId.id.removePrefix("de"), "0")
        }.body()

        check(result.isNotEmpty()) { "Failed to retrieve book info for bookId ${bookId.id}" }
        return result.first()
    }

    suspend fun getThumbnail(url: String): Image? {
        val bytes: ByteArray = apiClient.get(url).body()
        return Image(bytes)
    }
}
