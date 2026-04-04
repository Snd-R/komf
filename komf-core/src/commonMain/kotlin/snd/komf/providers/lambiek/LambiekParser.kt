package snd.komf.providers.lambiek

import com.fleeksoft.ksoup.Ksoup
import io.github.oshai.kotlinlogging.KotlinLogging
import snd.komf.providers.lambiek.model.LambiekBook
import snd.komf.providers.lambiek.model.LambiekBookId
import snd.komf.providers.lambiek.model.LambiekSearchResult
import snd.komf.providers.lambiek.model.LambiekSeries
import snd.komf.providers.lambiek.model.LambiekSeriesBook
import snd.komf.providers.lambiek.model.LambiekSeriesId

private val logger = KotlinLogging.logger {}

class LambiekParser {

    /**
     * Parses the HTML returned by a POST to /search/ (with series_search=1).
     * Extracts unique series slugs from book table rows.
     */
    fun parseSearchResults(html: String): List<LambiekSearchResult> {
        val document = Ksoup.parse(html)
        // First table: books with columns title | series | artist
        val table = document.selectFirst("table") ?: return emptyList()
        val rows = table.select("tbody tr")

        return rows.mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 2) return@mapNotNull null
            val link = cells[0].selectFirst("a") ?: return@mapNotNull null
            val href = link.attr("href").trim()
            // Extract series slug: /shop/series/{slug}/{book-slug}.html
            val slug = href
                .substringAfter("/shop/series/", "")
                .substringBefore("/")
                .takeIf { it.isNotBlank() } ?: return@mapNotNull null

            // Series column may include format suffix like "sc" or "hc" — strip it
            val rawSeriesLabel = cells[1].text().trim()
            val seriesName = cleanSeriesLabel(rawSeriesLabel).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val artistName = cells.getOrNull(2)?.text()?.trim()?.takeIf { it.isNotBlank() }

            LambiekSearchResult(
                id = LambiekSeriesId(slug),
                seriesName = seriesName,
                artistName = artistName,
            )
        }
            .distinctBy { it.id }
    }

    /**
     * Parses the series page at /shop/series/{slug}/ (which shows the latest book).
     * Extracts series-level data: title (from breadcrumb), publisher, language, genres.
     */
    fun parseSeriesPage(html: String, seriesId: LambiekSeriesId): LambiekSeries {
        val document = Ksoup.parse(html)

        // Series title from breadcrumb: last <span itemprop="name"> inside .broodkruimel
        val breadcrumbItems = document.select(".broodkruimel [itemprop='name']")
        val title = breadcrumbItems.lastOrNull()?.text()?.trim()
            ?: seriesId.slug.replace("-", " ").replaceFirstChar { it.uppercaseChar() }

        // Publisher from schema.org
        val publisher = document.selectFirst("[itemprop='publisher'] a")?.text()?.trim()
            ?: document.selectFirst("[itemprop='publisher']")?.text()?.trim()

        // Language from itemprop
        val language = document.selectFirst("[itemprop='inLanguage']")?.text()?.trim()

        // Genres from itemprop
        val genres = document.select("[itemprop='genre']").map { it.text().trim() }.filter { it.isNotBlank() }

        if (title.isBlank()) logger.warn { "Lambiek series ${seriesId.slug}: title is blank" }

        return LambiekSeries(
            id = seriesId,
            title = title,
            publisher = publisher,
            language = language,
            genres = genres,
            books = emptyList(), // books come from parseCollections
        )
    }

    /**
     * Parses the /collections/{slug}/ page to extract all books in the series.
     */
    fun parseCollections(html: String, seriesId: LambiekSeriesId): List<LambiekSeriesBook> {
        val document = Ksoup.parse(html)
        val seriesSlug = seriesId.slug

        // Find all links pointing to this specific series' books
        val bookLinks = document.select("a[href*='/shop/series/$seriesSlug/']")
            .filter { link ->
                // Must have 4+ path segments: /shop/series/{slug}/{bookId}/{bookSlug}.html
                val href = link.attr("href")
                val segments = href.removePrefix("/").split("/")
                segments.size >= 5 && segments[2] == seriesSlug
            }

        val seen = mutableSetOf<Int>()
        return bookLinks.mapNotNull { link ->
            val href = link.attr("href")
            val segments = href.removePrefix("/").split("/")
            // segments: shop, series, {slug}, {bookId}, {bookSlug}.html
            val bookId = segments.getOrNull(3)?.toIntOrNull() ?: return@mapNotNull null
            if (!seen.add(bookId)) return@mapNotNull null

            val bookSlug = segments.getOrNull(4)?.removeSuffix(".html") ?: return@mapNotNull null

            // Volume and title from the nearest h2 sibling/parent
            val heading = link.parent()?.selectFirst("h2")
                ?: link.selectFirst("h2")
                ?: link.nextElementSibling()?.let { if (it.tagName() == "h2") it else null }
            val headingText = heading?.text()?.trim() ?: link.text().trim()
            val (volume, bookTitle) = parseVolumeAndTitle(headingText)

            // Cover image
            val imageUrl = link.selectFirst("img[src*='/share/image.php/']")?.attr("src")

            LambiekSeriesBook(
                id = LambiekBookId(bookId),
                slug = bookSlug,
                volume = volume,
                title = bookTitle,
                imageUrl = imageUrl,
            )
        }
    }

    /**
     * Parses an individual book page at /shop/series/{slug}/{bookId}/{bookSlug}.html
     * using schema.org Book microdata.
     */
    fun parseBookPage(html: String, bookId: LambiekBookId): LambiekBook {
        val document = Ksoup.parse(html)
        val bookSection = document.selectFirst("[itemtype='http://schema.org/Book']") ?: document

        fun itemprop(name: String) = bookSection.selectFirst("[itemprop='$name']")?.text()?.trim()
        fun itempropAttr(name: String, attr: String) = bookSection.selectFirst("[itemprop='$name']")?.attr(attr)?.trim()

        val title = itemprop("name")
        val illustrator = bookSection.selectFirst("[itemprop='illustrator'] a")?.text()?.trim()
            ?: itemprop("illustrator")
        val writer = itemprop("author")
        val publisher = bookSection.selectFirst("[itemprop='publisher'] a")?.text()?.trim()
            ?: itemprop("publisher")
        val releaseDate = itemprop("datePublished")
        val language = itemprop("inLanguage")
        val pageCount = itemprop("numberOfPages")?.toIntOrNull()
        val isbn = itempropAttr("isbn", "content")?.takeIf { it.isNotBlank() }
            ?: itemprop("isbn")
        val description = itemprop("description")?.takeIf { it.isNotBlank() }
        val imageUrl = bookSection.selectFirst("[itemprop='image']")?.attr("src")?.takeIf { it.isNotBlank() }
        val genres = bookSection.select("[itemprop='genre']").map { it.text().trim() }.filter { it.isNotBlank() }

        // Volume is not in microdata — find in .specifics divs
        val volume = bookSection.select(".specifics").firstOrNull { it.text().contains("volume:") }
            ?.selectFirst("span")?.text()?.trim()

        // Series slug from the series link in .specifics
        val seriesHref = bookSection.select(".specifics")
            .firstOrNull { it.text().startsWith("series:") }
            ?.selectFirst("span a")?.attr("href")
        val seriesSlug = seriesHref?.substringAfter("/shop/series/")?.removeSuffix("/")
        val seriesId = seriesSlug?.takeIf { it.isNotBlank() }?.let { LambiekSeriesId(it) }

        if (title.isNullOrBlank()) logger.debug { "Lambiek book $bookId: title is blank" }

        return LambiekBook(
            id = bookId,
            seriesId = seriesId,
            title = title,
            volume = volume,
            isbn = isbn,
            releaseDate = releaseDate,
            illustrator = illustrator,
            writer = writer,
            publisher = publisher,
            language = language,
            genres = genres,
            pageCount = pageCount,
            description = description,
            imageUrl = imageUrl,
        )
    }

    /**
     * Strips trailing format suffix from series column (e.g. "Suske en Wiske sc" → "Suske en Wiske").
     */
    private fun cleanSeriesLabel(raw: String): String =
        raw.replace(Regex("""\s+(sc|hc|pocket|junior|integraal)$""", RegexOption.IGNORE_CASE), "").trim()

    /**
     * Splits heading text like "89 De Dolle Musketiers" into ("89", "De Dolle Musketiers").
     * Returns (null, fullText) when no leading number is found.
     */
    private fun parseVolumeAndTitle(text: String): Pair<String?, String> {
        val match = Regex("""^(\d+)\s+(.+)$""").matchEntire(text.trim())
        return if (match != null) {
            match.groupValues[1] to match.groupValues[2]
        } else {
            null to text
        }
    }
}
