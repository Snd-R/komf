package org.snd.metadata.providers.viz

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.snd.infra.HttpClient
import org.snd.infra.HttpException
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.providers.viz.model.VizAllBooksId
import org.snd.metadata.providers.viz.model.VizBook
import org.snd.metadata.providers.viz.model.VizBookId
import org.snd.metadata.providers.viz.model.VizSeriesBook

class VizClient(
    private val client: HttpClient
) {
    private val baseUrl: HttpUrl = "https://www.viz.com/".toHttpUrl()
    private val parser = VizParser()

    fun searchSeries(name: String): Collection<VizSeriesBook> {
        val searchQuery = "$name, Vol. 1"
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("search")
                .addQueryParameter("search", searchQuery)
                .addQueryParameter("category", "Manga")
                .build()
        ).build()

        return parser.parseSearchResults(client.execute(request))
    }

    fun getAllBooks(id: VizAllBooksId): Collection<VizSeriesBook> {
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("/read/manga/${id.id}/all")
                .build()
        ).build()

        return parser.parseSeriesAllBooks(client.execute(request))
    }

    fun getBook(bookId: VizBookId): VizBook {
        val request = Request.Builder()
            .url(
                baseUrl.newBuilder()
                    .addPathSegments("/read/manga/")
                    .addPathSegments(bookId.id).build()
            )
            .build()

        val response = client.execute(request)

        return parser.parseBook(response)
    }

    fun getThumbnail(url: HttpUrl): Thumbnail? {
        val request = Request.Builder().url(url).build()
        return try {
            val bytes = client.executeWithByteResponse(request)
            Thumbnail(bytes)
        } catch (e: HttpException) {
            if (e.code == 403) null
            else throw e
        }

    }
}
