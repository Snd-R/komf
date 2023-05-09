package org.snd.metadata.providers.bookwalker

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.snd.common.StringUtils.replaceFullwidthChars
import org.snd.metadata.BookNameParser
import org.snd.metadata.model.metadata.BookRange
import org.snd.metadata.providers.bookwalker.model.BookWalkerBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerBookId
import org.snd.metadata.providers.bookwalker.model.BookWalkerBookListPage
import org.snd.metadata.providers.bookwalker.model.BookWalkerSearchResult
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesBook
import org.snd.metadata.providers.bookwalker.model.BookWalkerSeriesId
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class BookWalkerParser {
    private val baseUrl = "https://global.bookwalker.jp"

    fun parseSearchResults(results: String): Collection<BookWalkerSearchResult> {
        val document = Jsoup.parse(results)
        return document.getElementsByClass("o-tile-list").first()?.children()
            ?.map { parseSearchResult(it) }
            ?: emptyList()
    }

    fun parseSeriesBooks(seriesBooks: String): BookWalkerBookListPage {
        val document = Jsoup.parse(seriesBooks)
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
        val document = Jsoup.parse(book)
        val synopsis = document.getElementsByClass("synopsis-text").first()?.wholeText()?.trim()?.replace("\n\n", "\n")
        val image = document.getElementsByClass("book-img").first()?.firstElementChild()?.firstElementChild()?.attr("src")
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
        val authors = productDetail.children().firstOrNull { it.child(0).text() == "Author" || it.child(0).text() == "By (author)" }
            ?.child(1)?.children()
            ?.map { it.text() }
            ?.map { replaceFullwidthChars(it) } ?: emptyList()

        val artists = productDetail.children().firstOrNull { it.child(0).text() == "Artist" || it.child(0).text() == "By (artist)" }
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
            ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH)) }

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

        return BookWalkerSearchResult(
            seriesId = parseSeriesId(resultUrl),
            bookId = parseBookId(resultUrl),
            seriesName = parseSeriesName(titleElement.text()),
            imageUrl = imageUrl,
        )
    }

    private fun parseSeriesId(url: String): BookWalkerSeriesId? {
        if (url.startsWith("$baseUrl/series/").not()) return null

        return url.removePrefix("$baseUrl/series/")
            .replace("/.*/$".toRegex(), "")
            .removeSuffix("/")
            .let { BookWalkerSeriesId(URLDecoder.decode(it, "UTF-8")) }
    }

    private fun parseBookId(url: String): BookWalkerBookId {
        return url.removePrefix("$baseUrl/")
            .replace("/.*/$".toRegex(), "")
            .removeSuffix("/")
            .let { BookWalkerBookId(URLDecoder.decode(it, "UTF-8")) }
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
