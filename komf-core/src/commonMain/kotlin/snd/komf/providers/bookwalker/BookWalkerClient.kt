package snd.komf.providers.bookwalker

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerBookListPage
import snd.komf.providers.bookwalker.model.BookWalkerCategory
import snd.komf.providers.bookwalker.model.BookWalkerSearchResult
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId

const val bookWalkerBaseUrl = "https://global.bookwalker.jp"

class BookWalkerClient(
    private val ktor: HttpClient
) {
    private val parser = BookWalkerParser()

    suspend fun searchSeries(name: String, category: BookWalkerCategory): Collection<BookWalkerSearchResult> {

        return try {
            val document = ktor.get("$bookWalkerBaseUrl/search/") {
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
        val document = ktor.get("$bookWalkerBaseUrl/series/${id.id}/") {
            parameter("page", page)
        }.bodyAsText()
        return parser.parseSeriesBooks(document)
    }

    suspend fun getBook(id: BookWalkerBookId): BookWalkerBook {
        val document = ktor.get("$bookWalkerBaseUrl/${id.id}/").bodyAsText()
        return parser.parseBook(document)
    }

    suspend fun getThumbnail(url: String?): Image? {
        return url?.let {
            val bytes: ByteArray = ktor.get(it).body()
            Image(bytes)
        }
    }
}
