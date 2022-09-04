package org.snd.metadata.providers.viz

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.snd.metadata.providers.viz.model.AgeRating.*
import org.snd.metadata.providers.viz.model.VizAllBooksId
import org.snd.metadata.providers.viz.model.VizBook
import org.snd.metadata.providers.viz.model.VizBookId
import org.snd.metadata.providers.viz.model.VizSeriesBook
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

class VizParser {
    private val baseUrl = "https://www.viz.com"
    private val writerRoles = setOf("Story by", "Story and Art by", "Storyboards by")
    private val artistRoles = setOf("Story and Art by", "Art by")

    fun parseSearchResults(results: String): Collection<VizSeriesBook> {
        val document = Jsoup.parse(results)
        return document.getElementById("results")?.children()
            ?.mapNotNull { parseSeriesBook(it) }
            ?.filter { it.number == 1 } ?: emptyList()
    }

    private fun parseSeriesBook(result: Element): VizSeriesBook {
        val imageUrl = result.child(0).getElementsByTag("img").firstOrNull()?.attr("data-original")
        val titleElement = result.child(1).child(1)
        val id = URLDecoder.decode(titleElement.attr("href"), "UTF-8")
            .removePrefix("/read/manga/")
        val bookNumber = getBookNumber(titleElement.text())
        val final = result.child(0).child(0).text() == "Final Volume!"

        return VizSeriesBook(
            id = VizBookId(id),
            name = titleElement.text(),
            seriesName = getSeriesName(titleElement.text()),
            number = bookNumber,
            imageUrl = imageUrl,
            final = final
        )
    }

    fun parseBook(book: String): VizBook {
        val document = Jsoup.parse(book)
        val productRow = document.getElementById("product_row")!!
        val cover = productRow.getElementById("product_image_block")?.getElementsByTag("img")?.attr("src")
        val purchaseLinksBlock = productRow.getElementById("purchase_links_block")!!
        val titleElement = purchaseLinksBlock.getElementsByTag("h2").first()!!
        val genres = purchaseLinksBlock.child(0).child(0).getElementsByTag("a")
            .filter { element -> !element.hasClass("bg-yellow") }
            .map { it.text() }
        val bookNumber = getBookNumber(titleElement.text())
        val summary = productRow.child(1).child(1).text()
        val details = productRow.child(1).child(2)
        val authors = details.child(0).child(0).text()
        val releaseDate = details.getElementsByClass("o_release-date").firstOrNull()?.text()
            ?.removePrefix("Release ")
            ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("MMMM d, yyyy", ENGLISH)) }
        val isbn = details.getElementsByClass("o_isbn13").firstOrNull()?.text()?.removePrefix("ISBN-13 ")
        val ageRating = details.child(1).child(3).text().removePrefix("Age Rating ").let {
            when (it) {
                "All Ages" -> ALL_AGES
                "Teen" -> TEEN
                "Teen Plus" -> TEEN_PLUS
                "Mature" -> MATURE
                else -> null
            }
        }
        val linkToAllBooks = titleElement.getElementsByTag("a").first()!!.attr("href")
            .let { URLDecoder.decode(it, "UTF-8") }
            .removeSurrounding("/read/manga/", "/all")

        return VizBook(
            id = parseBookId(document),
            name = titleElement.text(),
            seriesName = getSeriesName(titleElement.text()),
            number = bookNumber,
            releaseDate = releaseDate,
            description = summary,
            coverUrl = cover,
            genres = genres,
            isbn = isbn,
            ageRating = ageRating,
            authorStory = parseAuthor(authors, writerRoles),
            authorArt = parseAuthor(authors, artistRoles),
            allBooksId = VizAllBooksId(linkToAllBooks)
        )
    }

    fun parseSeriesAllBooks(books: String): Collection<VizSeriesBook> {
        val document = Jsoup.parse(books)

        return document.getElementById("c-0-s-0")?.child(0)
            ?.children()?.mapNotNull { parseSeriesBook(it) } ?: emptyList()
    }

    private fun getBookNumber(name: String): Int? {
        return ", Vol. (?<bookNumber>[0-9]+)".toRegex()
            .find(name)
            ?.groups?.get("bookNumber")?.value
            ?.toIntOrNull()
    }

    private fun getSeriesName(name: String): String {
        return name.replace(", Vol. [0-9]+".toRegex(), "")
    }

    private fun parseBookId(document: Document): VizBookId {
        val id = document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content")
            .removePrefix("$baseUrl/read/manga/")

        return VizBookId(URLDecoder.decode(id, "UTF-8"))
    }

    private fun parseAuthor(authors: String, roles: Collection<String>): String? {
        return authors.split(",").ifEmpty { authors.split(";") }
            .map { it.trim() }
            .firstNotNullOfOrNull { roleAndAuthor ->
                roles.firstOrNull { roleAndAuthor.startsWith(it) }
                    ?.let { roleAndAuthor.removePrefix("$it ") }
            }
    }

}
