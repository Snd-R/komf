package org.snd.metadata.providers.yenpress

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.snd.metadata.BookNameParser
import org.snd.metadata.providers.yenpress.model.YenPressBook
import org.snd.metadata.providers.yenpress.model.YenPressBookId
import org.snd.metadata.providers.yenpress.model.YenPressSearchResult
import org.snd.metadata.providers.yenpress.model.YenPressSeriesBook
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class YenPressParser {
    private val baseUrl = "https://yenpress.com"

    fun parseSearchResults(results: String): Collection<YenPressSearchResult> {
        val document = Jsoup.parse(results)
        return document.getElementById("yen-press-0")
            ?.children()
            ?.mapNotNull { parseSearchResult(it) }
            ?: emptyList()
    }

    private fun parseSearchResult(result: Element): YenPressSearchResult? {
        val bookDetailsElement = result.getElementsByClass("book-detail").firstOrNull() ?: return null
        val title = bookDetailsElement.child(0).text()

        val bookNumber = BookNameParser.getVolumes(title)
        if (bookNumber?.start?.toInt() != 1) return null

        val bookLinksElements = bookDetailsElement.getElementsByTag("a")
        val id = bookLinksElements.firstOrNull { it.text() == "Digital download" }?.attr("href")
            ?: bookLinksElements.firstOrNull { it.text() == "Paperback" }?.attr("href")
            ?: return null
        val coverImage = result.getElementsByTag("img").firstOrNull()?.attr("src")

        return YenPressSearchResult(
            id = YenPressBookId(URLDecoder.decode(id, "UTF-8")),
            imageUrl = coverImage,
            title = title
        )
    }

    fun parseBook(book: String): YenPressBook {
        val document = Jsoup.parse(book)
        val coverStrip = document.getElementById("book-cover-strip")!!
        val coverImage = coverStrip.getElementsByClass("book-cover").first()
            ?.child(0)?.getElementsByTag("img")
            ?.attr("src")
            ?.let { if (it == "https://yenpress-us.imgix.net/missing-cover.jpg") null else it }
            ?.removeSuffix("?auto=format&w=298")
            ?.plus("?w=1000")

        val title = document.getElementById("book-title")!!.text()
        val description = document.getElementById("book-description-full")?.text()
        val genres = document.getElementById("book-categories")?.textNodes()?.get(3)?.text()
            ?.split("/")?.map { it.trim() }
        val bookDetails = document.getElementById("book-details")
        val isbn = bookDetails?.child(1)?.getElementsByTag("li")
            ?.firstOrNull { element -> element.child(0).text() == "ISBN-13:" }
            ?.child(1)?.text()
        val releaseDate = bookDetails?.child(1)?.getElementsByTag("li")
            ?.firstOrNull { element -> element.child(0).text() == "On Sale Date:" }
            ?.child(1)?.text()
            ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MM/dd/yyyy")) }
        val imprint = bookDetails?.child(1)?.getElementsByTag("li")
            ?.firstOrNull { element -> element.child(0).text() == "Imprint:" }
            ?.child(1)?.text()
            ?: "Yen Press"

        val seriesBooks = document.getElementById("isbn-grid-0")
            ?.getElementsByClass("book-wrapper")
            ?.map { it.child(0).child(0) }
            ?.map { it.attr("href") to it.attr("alt") }
            ?.map { (id, name) ->
                YenPressSeriesBook(id = YenPressBookId(id), number = BookNameParser.getVolumes(name), name = name)
            } ?: emptyList()

        return YenPressBook(
            id = parseBookId(document),
            name = title,
            number = BookNameParser.getVolumes(title),
            releaseDate = releaseDate,
            description = description,
            imageUrl = coverImage,
            genres = genres ?: emptyList(),
            isbn = isbn,
            imprint = imprint,
            seriesBooks = seriesBooks
        )
    }

    private fun parseBookId(document: Document): YenPressBookId {
        val id = document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content")
            .removePrefix("$baseUrl/")

        return YenPressBookId(URLDecoder.decode(id, "UTF-8"))
    }
}
