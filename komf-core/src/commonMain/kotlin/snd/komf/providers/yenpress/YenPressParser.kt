package snd.komf.providers.yenpress

import com.fleeksoft.ksoup.Ksoup
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import snd.komf.util.BookNameParser
import snd.komf.providers.yenpress.model.YenPressAuthor
import snd.komf.providers.yenpress.model.YenPressBook
import snd.komf.providers.yenpress.model.YenPressBookId
import snd.komf.providers.yenpress.model.YenPressBookShort
import snd.komf.providers.yenpress.model.YenPressMoreBooksResponse
import snd.komf.providers.yenpress.model.YenPressSeriesId

class YenPressParser {
    private val nextOrdRegex = "&next_ord=(?<nextOrd>\\d+)$".toRegex()
    private val searchKeyRegex = "\"search_key\":\"(?<searchKey>.*)\",".toRegex()
    private val authorRegex = "(?<role>.*):\\s(?<name>.*)".toRegex()
    private val dateFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
        char(' ')
        dayOfMonth()
        chars(", ")
        year()
    }

    fun parseBook(book: String, bookId: YenPressBookId): YenPressBook {
        val document = Ksoup.parse(book)

        val headingElement = document.getElementsByClass("heading-content").first()!!
        val title = headingElement.getElementsByClass("heading").first()!!.text()
        val authors = headingElement.getElementsByClass("story-details").first()
            ?.children()
            ?.map { authorRegex.find(it.text())?.groups }
            ?.mapNotNull { regex ->
                regex?.get("role")?.value
                    ?.let { role ->
                        regex["name"]?.value
                            ?.let { name -> role to name }
                    }
            }
            ?.map { (role, name) ->
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
            ?.let { LocalDate.parse(it, dateFormat) }

        val seriesId = bookDetailsElement.getElementsByClass("social-share").first()!!
            .getElementsByClass("center-btn-page").first()!!
            .getElementsByClass("main-btn black").first()!!
            .attr("href")
            .removePrefix("/series/")
            .removeSuffix("?format=Digital")
        return YenPressBook(
            id = bookId,
            name = title,
            number = BookNameParser.getVolumes(title) ?: BookNameParser.getBookNumber(title),
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
        val document = Ksoup.parse(booksDocument)
        val books = document.getElementsByClass("inline_block")
            .map {
                val link = it.child(0)
                val bookId = YenPressBookId(link.attr("href").removePrefix("/titles/"))
                val name = link.child(1).text()

                YenPressBookShort(
                    id = bookId,
                    number = BookNameParser.getVolumes(name) ?: BookNameParser.getBookNumber(name),
                    name = name,
                )
            }
        val nextOrd = document.getElementsByClass("show-more")
            .firstOrNull()
            ?.attr("data-url")
            ?.let { nextOrdRegex.find(it)?.groups?.get("nextOrd")?.value }
            ?.toInt()

        return YenPressMoreBooksResponse(
            books = books,
            nextOrd = nextOrd
        )
    }

    fun parseSearchKey(search: String): String {
        return Ksoup.parse(search).head()
            .getElementsByTag("script")
            .mapNotNull { searchKeyRegex.find(it.data()) }
            .firstNotNullOf { it.groups["searchKey"]?.value }
    }
}
