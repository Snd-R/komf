package org.snd.metadata.providers.bookwalker

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.snd.metadata.providers.bookwalker.model.*
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class BookWalkerParser {
    private val baseUrl = "https://global.bookwalker.jp"

    fun parseSearchResults(results: String): Collection<BookWalkerSearchResult> {
        val document = Jsoup.parse(results)
        return document.getElementsByClass("o-tile-list").first()?.children()
            ?.mapNotNull { parseSearchResult(it) }
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
        val name = document.getElementsByClass("detail-book-title").first()!!.child(0).textNodes().first().text()
        val productDetail = document.getElementsByClass("product-detail").first()!!.child(0)
        val seriesTitle = productDetail.children().first { it.child(0).text() == "Series Title" }
            .child(1).text().let { parseSeriesName(it) }
        val japaneseTitles = productDetail.children().firstOrNull { it.child(0).text() == "Japanese Title" }
            ?.child(1)?.child(0)?.child(0)
        val japaneseTitle = japaneseTitles?.textNodes()?.firstOrNull()?.text()?.removeSuffix(" (")
        val authors = productDetail.children().firstOrNull { it.child(0).text() == "Author" || it.child(0).text() == "By (author)" }
            ?.child(1)?.children()?.map { it.text() } ?: emptyList()
        val artists = productDetail.children().firstOrNull { it.child(0).text() == "Artist" || it.child(0).text() == "By (artist)" }
            ?.child(1)?.children()?.map { it.text() } ?: authors
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
            name = name,
            number = parseBookNumber(name),
            seriesTitle = seriesTitle,
            japaneseTitle = japaneseTitle,
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
            id = getBookId(titleElement.child(0).attr("href")),
            name = titleElement.text(),
            number = parseBookNumber(titleElement.text())
        )
    }

    private fun parseSearchResult(result: Element): BookWalkerSearchResult? {
        val imageUrl = getSearchResultThumbnail(result)
        val titleElement = result.getElementsByClass("a-tile-ttl").first()!!
        val id = getSeriesId(titleElement.child(0).attr("href")) ?: return null

        return BookWalkerSearchResult(
            id = id,
            seriesName = parseSeriesName(titleElement.text()),
            imageUrl = imageUrl,
        )
    }

    private fun getSeriesId(url: String): BookWalkerSeriesId? {
        if (url.startsWith("$baseUrl/series/").not()) return null

        return url.removePrefix("$baseUrl/series/")
            .replace("/.*/$".toRegex(), "")
            .let { BookWalkerSeriesId(URLDecoder.decode(it, "UTF-8")) }
    }

    private fun getBookId(url: String): BookWalkerBookId {
        return url.removePrefix("$baseUrl/")
            .replace("/.*/$".toRegex(), "")
            .let { BookWalkerBookId(URLDecoder.decode(it, "UTF-8")) }
    }

    private fun parseSeriesName(name: String): String {
        return name.replace("( \\(?Manga\\)?)+$".toRegex(), "")
    }

    private fun getSearchResultThumbnail(result: Element): String? {
        return result.getElementsByClass("a-tile-thumb-img").first()
            ?.child(0)?.attr("data-srcset")
            ?.split(",")?.get(1)
            ?.removeSuffix(" 2x")
    }

    private fun parseBookNumber(name: String): Int? {
        return " (?<bookNumber>[0-9]+)\$".toRegex()
            .find(name)
            ?.groups?.get("bookNumber")?.value
            ?.toIntOrNull()
    }

    private fun parseDocumentBookId(document: Document): BookWalkerBookId {
        return getBookId(document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content"))
    }
}
