package org.snd.metadata.mangaupdates

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.snd.metadata.mangaupdates.model.Author
import org.snd.metadata.mangaupdates.model.Category
import org.snd.metadata.mangaupdates.model.Publisher
import org.snd.metadata.mangaupdates.model.RelatedSeries
import org.snd.metadata.mangaupdates.model.SearchResult
import org.snd.metadata.mangaupdates.model.Series
import org.snd.metadata.mangaupdates.model.Status
import org.snd.metadata.mangaupdates.model.Type
import java.net.URI
import java.time.Year

class MangaUpdatesParser {

    fun parseSeriesSearch(results: String): Collection<SearchResult> {
        val document = Jsoup.parse(results)
        return document.getElementsByClass("col-12 col-lg-6 p-3 text")
            .map { it.child(0) }
            .map { box ->
                val thumbnail = box.child(0).getElementsByTag("img")
                    .firstOrNull()?.attr("src")
                val isAdult = box.child(0).children()
                    .any { it.text() == "AdultContent" }

                val info = box.child(1).child(0)
                val titleLink = info.child(0).getElementsByTag("a")
                val id = titleLink.attr("href")
                    .removePrefix("https://www.mangaupdates.com/series.html?id=").toInt()
                val title = titleLink.text()
                val genres = info.child(1).getElementsByTag("a")
                    .firstOrNull()?.attr("title")?.split(",")
                    ?: emptyList()
                val summary = info.child(2).wholeText()

                val rating = "(?<year>\\d{4})*(?: - )*(?:(?<rating>\\d+\\.(\\d+)) / 10\\.0)*".toRegex()
                    .matchEntire(info.child(3).text())!!

                SearchResult(
                    id = id,
                    title = title,
                    summary = summary,
                    thumbnail = thumbnail,
                    genres = genres,
                    year = rating.groups["year"]?.value?.toInt()?.let { Year.of(it) },
                    rating = rating.groups["rating"]?.value?.toDouble(),
                    isAdult = isAdult
                )
            }
    }

    fun parseSeries(seriesId: Int, series: String): Series {
        val document = Jsoup.parse(series)

        val mainContent = requireNotNull(document.getElementById("main_content"))
        require(mainContent.childrenSize() > 1)
        val seriesInfo = mainContent.child(1).child(0)
        val col1 = seriesInfo.child(2).getElementsByClass("sContent")
        val col2 = seriesInfo.child(3).getElementsByClass("sContent")

        return Series(
            id = seriesId,
            title = parseTitle(seriesInfo),
            description = parseDescription(col1[0]),
            type = parseType(col1[1]),
            relatedSeries = parseRelatedSeries(col1[2]),
            associatedNames = parseAssociatedNames(col1[3]),
            status = parseStatus(col1[6]),
            image = parseImage(col2[0]),
            genres = parseGenres(col2[1]),
            categories = parseCategories(col2[2]),
            authors = parseAuthors(col2[5]),
            artists = parseAuthors(col2[6]),
            year = parseYear(col2[7]),
            originalPublisher = parseOriginalPublisher(col2[8]),
            englishPublishers = parseEnglishPublishers(col2[11]),
        )
    }

    private fun parseTitle(element: Element): String {
        return element.getElementsByClass("releasestitle").first()!!.text()
    }

    private fun parseAuthors(element: Element): List<Author> {
        val authors = element.children()
            .filter { it.tag().name == "a" && it.attr("title") == "Author Info" }
            .map {
                Author(
                    id = it.attr("href").removePrefix("https://www.mangaupdates.com/authors.html?id=").toInt(),
                    name = it.text()
                )
            }
        val unknownAuthors = element.textNodes()
            .map { it.text().trim() }
            .filter { it.isNotBlank() && it != "]" }
            .map { it.removeSuffix(" [") }
            .map { Author(name = it, id = null) }

        return authors + unknownAuthors
    }

    private fun parseDescription(element: Element): String? {
        if (element.text() == "N/A") return null

        val fullDescription = element.getElementById("div_desc_more")
        return (fullDescription ?: element).childNodes()
            .joinToString("") {
                when (it) {
                    is Element -> parseDescriptionPart(it)
                    is TextNode -> it.wholeText
                    else -> ""
                }
            }
    }

    private fun parseDescriptionPart(element: Element): String {
        return if (element.tag().name == "br") "\n"
        else if (element.text() == "Less...") ""
        else element.text()
    }

    private fun parseType(element: Element): Type? {
        if (element.text() == "N/A") return null
        return runCatching { Type.valueOf(element.text().trim().uppercase()) }
            .getOrNull()
    }

    private fun parseRelatedSeries(element: Element): List<RelatedSeries> {
        if (element.text() == "N/A") return emptyList()

        return element.children().filter { it.tag().name.equals("a") }.map {
            RelatedSeries(
                id = it.attr("href").removePrefix("series.html?id=").toInt(),
                name = it.text(),
                relation = it.nextElementSibling()?.text()?.trim()?.removeSurrounding("(", ")")
            )
        }
    }

    private fun parseAssociatedNames(element: Element): List<String> {
        if (element.text() == "N/A") return emptyList()
        return element.textNodes().map { it.text() }
    }

    private fun parseStatus(element: Element): Status? {
        if (element.text() == "N/A") return null

        return "\\(.*\\)".toRegex().find(element.text())?.value?.removeSurrounding("(", ")")?.let {
            runCatching { Status.valueOf(it.uppercase()) }
                .getOrNull()
        }
    }

    private fun parseImage(element: Element): URI? {
        val imageTag = element.getElementsByTag("img")
        if (imageTag.isEmpty()) return null

        return URI.create(element.getElementsByTag("img")[0].attr("src"))
    }

    private fun parseGenres(element: Element): List<String> {
        if (element.text() == "N/A") return emptyList()

        return element.getElementsByTag("a").map { it.text() }.dropLast(1)
    }

    private fun parseCategories(element: Element): List<Category> {
        if (element.text() == "N/A") return emptyList()

        return element.getElementsByTag("li")
            .map {
                it.getElementsByTag("a")
                    .first { link -> link.attr("rel").equals("nofollow") }
            }
            .map {
                val regex = "\\s-?[0-9]*\\s".toRegex()
                Category(
                    name = it.text(),
                    score = regex.find(it.attr("title"))!!.value.trim().toInt()
                )
            }
    }

    private fun parseYear(element: Element): Year? {
        if (element.text() == "N/A") return null

        return Year.of(element.text().toInt())
    }

    private fun parseOriginalPublisher(element: Element): Publisher? {
        val name = element.text()
        if (name == "N/A") return null

        val publisher = element.children()
            .firstOrNull { it.tag().name == "a" && it.attr("title") == "Publisher Info" }
            ?.let {
                val id = it.attr("href").removePrefix("https://www.mangaupdates.com/publishers.html?id=").toInt()
                Publisher(id = id, name = it.text())
            }

        val unknownPublisher = element.textNodes()
            .asSequence()
            .map { it.text().trim() }
            .filter { it.isNotBlank() && it != "]" }
            .map { it.removeSuffix(" [") }
            .map { Publisher(name = it, id = null) }
            .firstOrNull()

        return publisher ?: unknownPublisher
    }

    private fun parseEnglishPublishers(element: Element): List<Publisher> {
        if (element.text() == "N/A") return emptyList()

        return element.getElementsByTag("a").map {
            Publisher(
                id = it.attr("href").removePrefix("https://www.mangaupdates.com/publishers.html?id=").toInt(),
                name = it.text()
            )
        }
    }

}
