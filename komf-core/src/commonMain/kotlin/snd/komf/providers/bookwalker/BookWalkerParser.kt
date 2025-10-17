package snd.komf.providers.bookwalker

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.http.decodeURLPart
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import snd.komf.model.BookRange
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerBookListPage
import snd.komf.providers.bookwalker.model.BookWalkerSearchResult
import snd.komf.providers.bookwalker.model.BookWalkerSeriesBook
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId
import snd.komf.util.BookNameParser
import snd.komf.util.replaceFullwidthChars

class BookWalkerParser {
    private val baseUrl = "https://global.bookwalker.jp"
    private val dateFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_FULL)
        char(' ')
        day()
        chars(", ")
        year()
    }

    fun parseSearchResults(results: String): Collection<BookWalkerSearchResult> {
        val document = Ksoup.parse(results)
        return document.getElementsByClass("o-tile-list").first()?.children()
            ?.map { parseSearchResult(it) }
            ?: emptyList()
    }

    fun parseSeriesBooks(seriesBooks: String): BookWalkerBookListPage {
        val document = Ksoup.parse(seriesBooks)
        val books = document.getElementsByClass("o-tile-list").first()?.children()
            ?.map { parseSeriesBook(it) }
            ?: emptyList()
        val pageElement = document.getElementsByClass("pager-area").firstOrNull()?.child(0)
        val currentPage = pageElement?.children()?.first { it.className() == "on" }
            ?.text()?.toInt() ?: 1
        val totalPages = pageElement?.children()?.mapNotNull { it.text().toIntOrNull() }?.max() ?: 1
        return BookWalkerBookListPage(page = currentPage, totalPages = totalPages, books = books)
    }

    fun parseBook(book: String): BookWalkerBook {
        val document = Ksoup.parse(book)
        val synopsis = document.getElementsByClass("synopsis-text").first()?.wholeText()?.trim()?.replace("\n\n", "\n")
        val image =
            document.getElementsByClass("book-img").first()?.firstElementChild()?.firstElementChild()?.attr("src")
        val name = document.getElementsByClass("detail-book-title").first()!!.child(0).textNodes().first().text().trim()
        val productDetail = document.getElementsByClass("product-detail").first()!!.child(0)
        val seriesTitleElement = productDetail.children()
            .firstOrNull { it.child(0).text() == "Series Title" }
            ?.child(1)
        val seriesTitle = seriesTitleElement?.text()?.let { parseSeriesName(it) }
        val seriesId = seriesTitleElement?.getElementsByTag("a")?.first()?.attr("href")?.let { parseSeriesId(it) }
        val japaneseTitles = productDetail.children().firstOrNull { it.child(0).text() == "Japanese Title" }
            ?.child(1)?.child(0)

        var japaneseTitle: String? = null
        var romajiTitle: String? = null
        japaneseTitles?.let { titleElement ->
            when (titleElement.children().size) {
                0 -> japaneseTitle = replaceFullwidthChars(titleElement.text())
                else -> {
                    japaneseTitle = titleElement.child(0).textNodes()
                        .firstOrNull()?.text()
                        ?.removeSuffix(" (")?.trim()
                        ?.let { replaceFullwidthChars(it) }
                    romajiTitle = titleElement.child(0).getElementsByClass("product-detail-romaji")
                        .first()?.text()
                        ?.removeSuffix(")")?.trim()
                        ?.let { replaceFullwidthChars(it) }
                }
            }
        }
        val authors = productDetail.children()
            .firstOrNull { it.child(0).text() == "Author" || it.child(0).text() == "By (author)" }
            ?.child(1)?.children()
            ?.map { it.text() }
            ?.map { replaceFullwidthChars(it) } ?: emptyList()

        val artists = productDetail.children()
            .firstOrNull { it.child(0).text() == "Artist" || it.child(0).text() == "By (artist)" }
            ?.child(1)?.children()
            ?.map { it.text() }
            ?.map { replaceFullwidthChars(it) } ?: authors
        val publisher = productDetail.children().first { it.child(0).text() == "Publisher" }
            .child(1).text()
        val genres = productDetail.children().firstOrNull { it.child(0).text() == "Genre" }
            ?.child(1)?.child(0)?.children()?.map { it.text() }
            ?: emptyList()
        val availableSince = productDetail.children().firstOrNull { it.child(0).text() == "Available since" }
            ?.child(1)?.text()?.split("/")?.first()
            ?.replace("\\(.*\\) PT ".toRegex(), "")?.trim()
            ?.let { LocalDate.parse(it, dateFormat) }

        return BookWalkerBook(
            id = parseDocumentBookId(document),
            seriesId = seriesId,
            name = name,
            number = parseBookNumber(name),
            seriesTitle = seriesTitle,
            japaneseTitle = japaneseTitle,
            romajiTitle = romajiTitle,
            artists = artists,
            authors = authors,
            publisher = publisher,
            genres = genres,
            availableSince = availableSince,
            synopsis = synopsis,
            imageUrl = image
        )
    }

    private fun parseSeriesBook(book: Element): BookWalkerSeriesBook {
        val titleElement = book.getElementsByClass("a-tile-ttl").first()!!
        return BookWalkerSeriesBook(
            id = parseBookId(titleElement.child(0).attr("href")),
            name = titleElement.text(),
            number = parseBookNumber(titleElement.text())
        )
    }

    private fun parseSearchResult(result: Element): BookWalkerSearchResult {
        val imageUrl = getSearchResultThumbnail(result)
        val titleElement = result.getElementsByClass("a-tile-ttl").first()!!
        val resultUrl = titleElement.child(0).attr("href")
        val seriesId = parseSeriesId(resultUrl)
        val bookId = if (seriesId == null) parseBookId(resultUrl) else null

        return BookWalkerSearchResult(
            seriesId = seriesId,
            bookId = bookId,
            seriesName = parseSeriesName(titleElement.text()),
            imageUrl = imageUrl,
        )
    }

    private fun parseSeriesId(url: String): BookWalkerSeriesId? {
        if (url.startsWith("$baseUrl/series/").not()) return null

        return url.removePrefix("$baseUrl/series/")
            .replace("/.*/$".toRegex(), "")
            .removeSuffix("/")
            .let { BookWalkerSeriesId(it.decodeURLPart()) }
    }

    private fun parseBookId(url: String): BookWalkerBookId {
        return url.removePrefix("$baseUrl/")
            .replace("/.*/$".toRegex(), "")
            .removeSuffix("/")
            .let { BookWalkerBookId(it.decodeURLPart()) }
    }

    private fun parseSeriesName(name: String): String {
        return name.trimEnd()
            .replace("\\(?(Manga|Light Novels| Vol. \\d$)\\)?$".toRegex(), "")
            .trim()
    }

    private fun getSearchResultThumbnail(result: Element): String? {
        return result.getElementsByClass("a-tile-thumb-img").first()
            ?.child(0)?.attr("data-srcset")
            ?.split(",")?.get(1)
            ?.removeSuffix(" 2x")
            ?.trim()
    }

    private fun parseDocumentBookId(document: Document): BookWalkerBookId {
        return parseBookId(document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content"))
    }

    private fun parseBookNumber(name: String): BookRange? {
        return BookNameParser.getVolumes(name)
            ?: "(?i)(?<!chapter)\\s\\d+".toRegex().findAll(name).lastOrNull()?.value?.toDoubleOrNull()
                ?.let { BookRange(it, it) }
    }
}
