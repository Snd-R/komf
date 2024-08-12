package snd.komf.providers.viz

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.providers.viz.model.VizAllBooksId
import snd.komf.providers.viz.model.VizBook
import snd.komf.providers.viz.model.VizBookId
import snd.komf.providers.viz.model.VizBookReleaseType
import snd.komf.providers.viz.model.VizSeriesBook

const val vizBaseUrl = "https://www.viz.com"

class VizClient(
    private val ktor: HttpClient
) {
    private val parser = VizParser()

    suspend fun searchSeries(name: String): Collection<VizSeriesBook> {
        val searchQuery = "$name, Vol. 1"
        val document = ktor.get("$vizBaseUrl/search") {
            parameter("search", searchQuery)
            parameter("category", "Manga")
        }.bodyAsText()

        return parser.parseSearchResults(document)
    }

    suspend fun getAllBooks(id: VizAllBooksId): Collection<VizSeriesBook> {
        val document = ktor.get("$vizBaseUrl/read/manga/${id.id}/all").bodyAsText()
        return parser.parseSeriesAllBooks(document)
    }

    suspend fun getBook(bookId: VizBookId, type: VizBookReleaseType): VizBook {
        val document = ktor.get("$vizBaseUrl/read/manga/${bookId.value}/${type.name.lowercase()}").bodyAsText()
        return parser.parseBook(document)
    }

    suspend fun getThumbnail(url: String): Image? {
        return try {
            val bytes: ByteArray = ktor.get(url).body()
            Image(bytes)
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Forbidden) null
            else throw e
        }

    }
}
