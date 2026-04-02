package snd.komf.providers.stripinfo

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import io.github.oshai.kotlinlogging.KotlinLogging
import snd.komf.providers.stripinfo.model.StripInfoAlbum
import snd.komf.providers.stripinfo.model.StripInfoAlbumId
import snd.komf.providers.stripinfo.model.StripInfoCredit
import snd.komf.providers.stripinfo.model.StripInfoRole
import snd.komf.providers.stripinfo.model.StripInfoSearchResult
import snd.komf.providers.stripinfo.model.StripInfoSeries
import snd.komf.providers.stripinfo.model.StripInfoSeriesAlbum
import snd.komf.providers.stripinfo.model.StripInfoSeriesId

private val logger = KotlinLogging.logger {}

class StripInfoParser {
    private val baseUrl = "https://www.stripinfo.be"

    fun parseSearchResults(html: String): List<StripInfoSearchResult> {
        val document = Ksoup.parse(html)
        // Search results are in sections with h4 "Exacte resultaten" and "Overige resultaten"
        // Each result is a table row with: <a href="/reeks/index/{id}_{slug}">{title}</a>
        return document.select("a[href~=/reeks/index/\\d+_]")
            .filter { element ->
                // Only select links in the search results area, not in navigation
                val href = element.attr("href")
                href.contains("/reeks/index/") && element.closest("table") != null
            }
            .mapNotNull { element ->
                val href = element.attr("href")
                val idStr = href.substringAfter("/reeks/index/").substringBefore("_")
                val id = idStr.toIntOrNull() ?: return@mapNotNull null
                val title = element.text().trim()
                if (title.isBlank()) return@mapNotNull null

                StripInfoSearchResult(
                    id = StripInfoSeriesId(id),
                    title = title,
                    url = if (href.startsWith("http")) href else "$baseUrl$href",
                )
            }
            .distinctBy { it.id }
    }

    fun parseSeries(html: String, seriesId: StripInfoSeriesId): StripInfoSeries {
        val document = Ksoup.parse(html)
        val seriesSection = document.selectFirst("#ComicSeries") ?: document

        // Title from microdata
        val title = seriesSection.selectFirst("[itemprop=name]")?.attr("content")
            ?: document.selectFirst("h1 img.pageLogo")?.attr("alt")
            ?: ""

        // Logo/image URL from h1 > a > img
        val imageUrl = document.selectFirst("h1 img.pageLogo")?.attr("src")

        // Dates from microdata
        val startDate = seriesSection.selectFirst("[itemprop=startDate]")?.attr("content")
        val endDate = seriesSection.selectFirst("[itemprop=endDate]")?.attr("content")
        val startYear = startDate?.substringBefore("-")?.toIntOrNull()
        val endYear = endDate?.substringBefore("-")?.toIntOrNull()

        // Rating from microdata
        val ratingValue = seriesSection
            .selectFirst("[itemprop=aggregateRating] [itemprop=ratingValue]")
            ?.attr("content")?.toDoubleOrNull()
        val reviewCount = seriesSection
            .selectFirst("[itemprop=aggregateRating] [itemprop=reviewCount]")
            ?.attr("content")?.toIntOrNull()

        // Albums from table rows with ComicIssue microdata
        val albums = seriesSection.select("tr[itemscope][itemtype='https://schema.org/ComicIssue']")
            .mapNotNull { row -> parseSeriesAlbumRow(row) }

        // Sidebar data
        val sidebar = document.selectFirst("aside") ?: document

        // Authors from sidebar
        val authors = parseSidebarSection(sidebar, "Auteur(s)")
            .mapNotNull { it.text().trim().ifBlank { null } }
            .filter { it != "..." }

        // Publishers from sidebar
        val publishers = parseSidebarSection(sidebar, "Uitgever(s)")
            .mapNotNull { it.text().trim().ifBlank { null } }

        // Language from sidebar
        val language = parseSidebarSection(sidebar, "Oorspronkelijke taal")
            .firstOrNull()?.text()?.trim()

        // Tags from sidebar
        val tags = sidebar.select("#reeksTags a")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        if (title.isBlank()) logger.warn { "StripInfo series $seriesId: title is blank — selector may have changed" }
        if (albums.isEmpty()) logger.warn { "StripInfo series $seriesId: no albums found — ComicIssue selector may have changed" }

        return StripInfoSeries(
            id = seriesId,
            title = title,
            imageUrl = imageUrl,
            startYear = startYear,
            endYear = endYear,
            rating = ratingValue,
            ratingCount = reviewCount,
            authors = authors,
            publishers = publishers,
            tags = tags,
            language = language,
            albums = albums,
        )
    }

    fun parseAlbum(html: String, albumId: StripInfoAlbumId): StripInfoAlbum {
        val document = Ksoup.parse(html)
        val issueSection = document.selectFirst("[itemtype='https://schema.org/ComicIssue']") ?: document

        // Microdata fields
        val number = issueSection.selectFirst("[itemprop=issueNumber]")?.text()?.trim()
        val title = issueSection.selectFirst("[itemprop=name]")?.text()?.trim()
            ?: issueSection.selectFirst("[itemprop=name]")?.attr("content")?.trim()
        val pageCount = issueSection.selectFirst("[itemprop=pageEnd]")?.text()?.trim()?.toIntOrNull()
        val isbn = issueSection.selectFirst("[itemprop=identifier]")?.text()?.trim()
        val publisher = issueSection.selectFirst("[itemprop=publisher] [itemprop=name]")?.text()?.trim()

        // Parse detail table for additional metadata
        val detailTable = document.select("table.lijst tr, table tr")
        var year: Int? = null
        var language: String? = null
        var binding: String? = null
        var colorInfo: String? = null
        var barcode: String? = null
        val credits = mutableListOf<StripInfoCredit>()

        for (row in detailTable) {
            val cells = row.select("td")
            if (cells.size < 2) continue
            val label = cells[0].text().trim().removeSuffix(":")
            val valueCell = cells[1]
            val value = valueCell.text().trim()

            when {
                label.equals("Scenario", ignoreCase = true) -> {
                    parseCreditsFromCell(valueCell, StripInfoRole.SCENARIO, credits)
                }
                label.equals("Tekeningen", ignoreCase = true) -> {
                    parseCreditsFromCell(valueCell, StripInfoRole.TEKENINGEN, credits)
                }
                label.equals("Kleuren", ignoreCase = true) -> {
                    parseCreditsFromCell(valueCell, StripInfoRole.KLEUREN, credits)
                }
                label.equals("Cover", ignoreCase = true) && valueCell.selectFirst("a[href*=/auteur/]") != null -> {
                    parseCreditsFromCell(valueCell, StripInfoRole.COVER, credits)
                }
                label.equals("Jaar", ignoreCase = true) -> year = value.toIntOrNull()
                label.equals("Taal", ignoreCase = true) -> language = value
                label.equals("Kaft", ignoreCase = true) -> binding = value
                label.startsWith("Kleur", ignoreCase = true) -> colorInfo = value
                label.equals("Barcode", ignoreCase = true) -> barcode = value
            }
        }

        // Try to extract series ID from canonical URL or breadcrumb links
        val seriesId = document.select("a[href*=/reeks/index/]")
            .firstOrNull()
            ?.attr("href")
            ?.substringAfter("/reeks/index/")
            ?.substringBefore("_")
            ?.toIntOrNull()
            ?.let { StripInfoSeriesId(it) }

        // Cover image
        val imageUrl = document.selectFirst("img[src*=image.php]")?.attr("src")

        if (number == null) logger.debug { "StripInfo album $albumId: no issue number found" }
        if (credits.isEmpty()) logger.debug { "StripInfo album $albumId: no credits parsed — check detail table selectors" }

        return StripInfoAlbum(
            id = albumId,
            seriesId = seriesId,
            title = title,
            number = number,
            year = year,
            pageCount = pageCount,
            isbn = isbn,
            barcode = barcode,
            language = language,
            binding = binding,
            colorInfo = colorInfo,
            publisher = publisher,
            imageUrl = imageUrl,
            credits = credits,
        )
    }

    private fun parseSeriesAlbumRow(row: Element): StripInfoSeriesAlbum? {
        val number = row.selectFirst("[itemprop=issueNumber]")?.text()?.trim()
        val titleElement = row.selectFirst("[itemprop=name]")
        val title = titleElement?.text()?.trim()
        val url = row.selectFirst("[itemprop=url]")?.attr("href") ?: return null
        val albumIdStr = url.substringAfter("/reeks/strip/").substringBefore("_")
        val albumId = albumIdStr.toIntOrNull() ?: return null
        val yearRange = row.selectFirst("td.secondcol")?.text()?.trim()

        return StripInfoSeriesAlbum(
            id = StripInfoAlbumId(albumId),
            number = number,
            title = title,
            yearRange = yearRange,
        )
    }

    private fun parseSidebarSection(sidebar: Element, sectionTitle: String): List<Element> {
        val header = sidebar.select("h4").firstOrNull { it.text().trim().startsWith(sectionTitle) }
            ?: return emptyList()

        // Collect elements between this h4 and the next h4
        val items = mutableListOf<Element>()
        var sibling = header.nextElementSibling()
        while (sibling != null && sibling.tagName() != "h4") {
            if (sibling.tagName() == "ul") {
                items.addAll(sibling.select("li"))
            }
            sibling = sibling.nextElementSibling()
        }
        return items
    }

    private fun parseCreditsFromCell(
        cell: Element,
        role: StripInfoRole,
        credits: MutableList<StripInfoCredit>,
    ) {
        val authorLinks = cell.select("a[href*=/auteur/]")
        if (authorLinks.isNotEmpty()) {
            for (link in authorLinks) {
                val name = link.text().trim()
                if (name.isBlank()) continue
                val authorIdStr = link.attr("href")
                    .substringAfter("/auteur/index/")
                    .substringBefore("_")
                val authorId = authorIdStr.toIntOrNull()
                credits.add(StripInfoCredit(name = name, role = role, authorId = authorId))
            }
        } else {
            // Fallback: plain text name without link
            val name = cell.text().trim()
            if (name.isNotBlank()) {
                credits.add(StripInfoCredit(name = name, role = role, authorId = null))
            }
        }
    }
}
