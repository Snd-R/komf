package org.snd.metadata.yenpress

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.snd.infra.HttpClient
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.yenpress.model.YenPressBook
import org.snd.metadata.yenpress.model.YenPressBookId
import org.snd.metadata.yenpress.model.YenPressSearchResult

class YenPressClient(
    private val client: HttpClient
) {
    private val baseUrl: HttpUrl = "https://yenpress.com/".toHttpUrl()
    private val parser = YenPressParser()

    fun searchSeries(name: String): Collection<YenPressSearchResult> {
        val searchQuery = "$name, Vol. 1"
        val request = Request.Builder().url(
            baseUrl.newBuilder().addPathSegments("search-list")
                .addQueryParameter("keyword", searchQuery)
                .build()
        ).build()

        return parser.parseSearchResults(client.execute(request))
    }

    fun getBook(bookId: YenPressBookId): YenPressBook {
        val request = Request.Builder()
            .url(baseUrl.newBuilder().addPathSegments(bookId.id).build())
            .build()

        val response = client.execute(request)

        return parser.parseBook(response)
    }

    fun getBookThumbnail(book: YenPressBook): Thumbnail? {
        return book.imageUrl?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Thumbnail(bytes)
        }
    }
}
