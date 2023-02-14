package org.snd.metadata.providers.yenpress

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.snd.common.http.HttpClient
import org.snd.metadata.model.Image
import org.snd.metadata.providers.yenpress.model.YenPressBook
import org.snd.metadata.providers.yenpress.model.YenPressBookId
import org.snd.metadata.providers.yenpress.model.YenPressSearchResult

const val yenPressBaseUrl = "https://yenpress.com/"

class YenPressClient(
    private val client: HttpClient
) {
    private val baseUrl: HttpUrl = yenPressBaseUrl.toHttpUrl()
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

    fun getBookThumbnail(book: YenPressBook): Image? {
        return book.imageUrl?.toHttpUrlOrNull()?.let {
            val request = Request.Builder().url(it).build()
            val bytes = client.executeWithByteResponse(request)
            Image(bytes)
        }
    }
}
