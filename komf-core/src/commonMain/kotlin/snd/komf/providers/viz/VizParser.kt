package snd.komf.providers.viz

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.ktor.http.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import snd.komf.providers.viz.model.AgeRating.ALL_AGES
import snd.komf.providers.viz.model.AgeRating.MATURE
import snd.komf.providers.viz.model.AgeRating.TEEN
import snd.komf.providers.viz.model.AgeRating.TEEN_PLUS
import snd.komf.providers.viz.model.VizAllBooksId
import snd.komf.providers.viz.model.VizBook
import snd.komf.providers.viz.model.VizBookId
import snd.komf.providers.viz.model.VizSeriesBook
import snd.komf.util.BookNameParser

class VizParser {
    private val baseUrl = "https://www.viz.com"
    private val writerRoles = setOf("Story by", "Story and Art by", "Storyboards by")
    private val artistRoles = setOf("Story and Art by", "Art by")
    private val dateFormat = LocalDate.Format {
        monthName(MonthNames.ENGLISH_FULL)
        char(' ')
        dayOfMonth(Padding.NONE)
        chars(", ")
        year()
    }

    fun parseSearchResults(results: String): Collection<VizSeriesBook> {
        val document = Ksoup.parse(results)
        return document.getElementById("results")?.children()
            ?.mapNotNull { parseSeriesBook(it) }
            ?.filter { it.number?.start?.toInt() == 1 } ?: emptyList()
    }

    private fun parseSeriesBook(result: Element): VizSeriesBook {
        val imageUrl = result.child(0).getElementsByTag("img").firstOrNull()?.attr("data-original")
        val titleElement = result.child(1).child(1)
        val id = titleElement.attr("href").decodeURLPart()
            .removePrefix("/read/manga/")
        val bookNumber = BookNameParser.getVolumes(titleElement.text())
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
        val document = Ksoup.parse(book)
        val productRow = document.getElementById("product_row")!!
        val cover = productRow.getElementById("product_image_block")?.getElementsByTag("img")?.attr("src")
        val purchaseLinksBlock = productRow.getElementById("purchase_links_block")!!
        val titleElement = purchaseLinksBlock.getElementsByTag("h2").first()!!
        val genres = purchaseLinksBlock.child(0).child(0).getElementsByTag("a")
            .filter { element -> !element.hasClass("bg-yellow") }
            .map { it.text() }
        val bookNumber = BookNameParser.getVolumes(titleElement.text())
        val summary = productRow.child(1).child(1).text()
        val details = productRow.child(1).child(2)
        val authors = details.child(0).child(0).text()
        val releaseDate = details.getElementsByClass("o_release-date").firstOrNull()?.text()
            ?.removePrefix("Release ")
            ?.let { LocalDate.parse(it, dateFormat) }
        val isbn = details.getElementsByClass("o_isbn13").firstOrNull()?.text()?.removePrefix("ISBN-13 ")
        val eisbn = details.getElementsByClass("o_eisbn13").firstOrNull()?.text()?.removePrefix("eISBN-13 ")
        val ageRating = details.child(1)
            .children()
            .firstOrNull { it.text().startsWith("Age Rating") }
            ?.text()?.removePrefix("Age Rating ").let {
                when (it) {
                    "All Ages" -> ALL_AGES
                    "Teen" -> TEEN
                    "Teen Plus" -> TEEN_PLUS
                    "Mature" -> MATURE
                    else -> null
                }
            }

        return VizBook(
            id = parseBookId(document),
            name = titleElement.text(),
            seriesName = getSeriesName(titleElement.text()),
            number = bookNumber,
            releaseDate = releaseDate,
            description = summary,
            coverUrl = cover,
            genres = genres,
            isbn = isbn ?: eisbn,
            ageRating = ageRating,
            authorStory = parseAuthor(authors, writerRoles),
            authorArt = parseAuthor(authors, artistRoles),
            allBooksId = parseLinkToAllBooks(titleElement)?.let { VizAllBooksId(it) }
        )
    }

    fun parseSeriesAllBooks(books: String): Collection<VizSeriesBook> {
        val document = Ksoup.parse(books)

        return document.getElementById("c-0-s-0")?.child(0)
            ?.children()?.mapNotNull { parseSeriesBook(it) } ?: emptyList()
    }

    private fun getSeriesName(name: String): String {
        return name.replace(", Vol. [0-9]+".toRegex(), "")
    }

    private fun parseBookId(document: Document): VizBookId {
        val id = document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content")
            .removePrefix("$baseUrl/read/manga/")

        return VizBookId(id.decodeURLPart())
    }

    private fun parseAuthor(authors: String, roles: Collection<String>): String? {
        return authors.split(",").ifEmpty { authors.split(";") }
            .map { it.trim() }
            .firstNotNullOfOrNull { roleAndAuthor ->
                roles.firstOrNull { roleAndAuthor.startsWith(it) }
                    ?.let { roleAndAuthor.removePrefix("$it ") }
            }
    }

    private fun parseLinkToAllBooks(titleElement: Element): String? {
        val titleLink = titleElement.getElementsByTag("a").first()
        val prefixLink = titleElement.previousElementSibling()?.let { if (it.tagName() != "a") null else it }
        val link = (titleLink ?: prefixLink) ?: return null

        return link
            .attr("href")
            .let { it.decodeURLPart() }
            .removeSurrounding("/read/manga/", "/all")

    }

}
