package org.snd.metadata.providers.yenpress

import org.jsoup.Jsoup
import org.snd.metadata.BookNameParser
import org.snd.metadata.providers.yenpress.model.YenPressAuthor
import org.snd.metadata.providers.yenpress.model.YenPressBook
import org.snd.metadata.providers.yenpress.model.YenPressBookId
import org.snd.metadata.providers.yenpress.model.YenPressBookShort
import org.snd.metadata.providers.yenpress.model.YenPressMoreBooksResponse
import org.snd.metadata.providers.yenpress.model.YenPressSeriesId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class YenPressParser {
    private val nextOrdRegex = "&next_ord=\\d+$".toRegex()
    private val searchKeyRegex = "\"search_key\":\"(?<searchKey>.*)\",".toRegex()

    fun parseBook(book: String, bookId: YenPressBookId): YenPressBook {
        val document = Jsoup.parse(book)

        val headingElement = document.getElementsByClass("heading-content").first()!!
        val title = headingElement.getElementsByClass("heading").first()!!.text()
        val authors = headingElement.getElementsByClass("story-details").first()
            ?.children()?.textNodes()
            ?.take(2)?.map { it.text() }
            ?.chunked(2)?.map { (role, name) ->
                YenPressAuthor(
                    role = role.removeSuffix(":"),
                    name = name
                )
            } ?: emptyList()

        val bookInfo = document.getElementsByClass("book-info").first()!!
        val description = bookInfo.getElementsByClass("content-heading-txt").first()
            ?.child(1)?.text()
        val cover = bookInfo.getElementsByClass("series-cover").first()
            ?.getElementsByTag("img")?.first()
            ?.attr("data-src")

        val bookDetailsElement = document.getElementsByClass("book-details").first()!!.children().last()!!
        val genres = bookDetailsElement.getElementsByClass("txt-hold").first()?.children()?.last()
            ?.children()?.map { it.text() }
            ?: emptyList()

        val bookDetails = bookDetailsElement.getElementsByClass("detail-info").first()
            ?.getElementsByTag("div")?.toList()
            ?.filter { div -> div.children().none { it.tagName() == "div" } }
            ?.associate {
                it.child(0).text() to it.child(1).text()
            } ?: emptyMap()
        val pageCount = bookDetails["Page Count"]?.removeSuffix(" pages")?.toIntOrNull()

        val releaseDate = bookDetails["Release Date"]
            ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MMM dd, yyyy")) }

        val seriesId = bookDetailsElement.getElementsByClass("social-share").first()!!
            .getElementsByClass("center-btn-page").first()!!
            .getElementsByClass("main-btn black").first()!!
            .attr("href").removePrefix("/series/")

        return YenPressBook(
            id = bookId,
            name = title,
            number = BookNameParser.getVolumes(title),
            seriesId = YenPressSeriesId(seriesId),

            authors = authors,
            description = description,
            genres = genres,
            seriesName = bookDetails["Series"],
            pageCount = pageCount,
            releaseDate = releaseDate,
            isbn = bookDetails["ISBN"],
            ageRating = bookDetails["Age Rating"],
            imprint = bookDetails["Imprint"],
            imageUrl = cover,
        )
    }

    fun parseMoreBooksResponse(booksDocument: String): YenPressMoreBooksResponse {
        val document = Jsoup.parse(booksDocument)
        val books = document.getElementsByClass("inline_block")
            .map {
                val link = it.child(0)
                val bookId = YenPressBookId(link.attr("href").removePrefix("/titles/"))
                val name = link.child(1).text()

                YenPressBookShort(
                    id = bookId,
                    number = BookNameParser.getVolumes(name),
                    name = name,
                )
            }
        val nextOrd = document.getElementsByClass("show-more")
            .firstOrNull()
            ?.attr("data-url")
            ?.let { nextOrdRegex.find(it)?.value }
            ?.toInt()

        return YenPressMoreBooksResponse(
            books = books,
            nextOrd = nextOrd
        )
    }

    fun parseSearchKey(search: String): String {
        return Jsoup.parse(search).head()
            .getElementsByTag("script")
            .mapNotNull { searchKeyRegex.find(it.data()) }
            .firstNotNullOf { it.groups["searchKey"]?.value }
    }
}
