package snd.komf.providers.lambiek

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import snd.komf.model.Image
import snd.komf.providers.lambiek.model.LambiekBook
import snd.komf.providers.lambiek.model.LambiekBookId
import snd.komf.providers.lambiek.model.LambiekSearchResult
import snd.komf.providers.lambiek.model.LambiekSeries
import snd.komf.providers.lambiek.model.LambiekSeriesBook
import snd.komf.providers.lambiek.model.LambiekSeriesId

const val lambiekBaseUrl = "https://www.lambiek.net"

class LambiekClient(
    private val ktor: HttpClient,
) {
    private val parser = LambiekParser()

    suspend fun searchSeries(name: String): List<LambiekSearchResult> {
        val html = ktor.submitForm(
            url = "$lambiekBaseUrl/search/",
            formParameters = parameters {
                append("keyword", name)
                append("searchtype", "loose")
                append("series_search", "1")
                append("artist_search", "0")
                append("title_search", "0")
                append("publisher_search", "0")
                append("description_search", "0")
                append("name_search", "0")
                append("realname_search", "0")
                append("keyword_search", "0")
                append("content_search", "0")
            }
        ).bodyAsText()
        return parser.parseSearchResults(html)
    }

    suspend fun getSeriesPage(seriesId: LambiekSeriesId): LambiekSeries {
        val html = ktor.get("$lambiekBaseUrl/shop/series/${seriesId.slug}/").bodyAsText()
        return parser.parseSeriesPage(html, seriesId)
    }

    suspend fun getCollections(seriesId: LambiekSeriesId): List<LambiekSeriesBook> {
        val html = ktor.get("$lambiekBaseUrl/collections/${seriesId.slug}/").bodyAsText()
        return parser.parseCollections(html, seriesId)
    }

    suspend fun getBook(seriesId: LambiekSeriesId, book: LambiekSeriesBook): LambiekBook {
        val html = ktor.get(
            "$lambiekBaseUrl/shop/series/${seriesId.slug}/${book.id.id}/${book.slug}.html"
        ).bodyAsText()
        return parser.parseBookPage(html, book.id)
    }

    suspend fun getBookById(seriesId: LambiekSeriesId, bookId: LambiekBookId, bookSlug: String): LambiekBook {
        val html = ktor.get(
            "$lambiekBaseUrl/shop/series/${seriesId.slug}/${bookId.id}/$bookSlug.html"
        ).bodyAsText()
        return parser.parseBookPage(html, bookId)
    }

    suspend fun getCover(url: String): Image? {
        return try {
            val bytes: ByteArray = ktor.get(
                if (url.startsWith("http")) url else "$lambiekBaseUrl$url"
            ).body()
            Image(bytes)
        } catch (_: Exception) {
            null
        }
    }
}
