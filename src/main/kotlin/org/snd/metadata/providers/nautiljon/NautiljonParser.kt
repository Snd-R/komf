package org.snd.metadata.providers.nautiljon

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.snd.metadata.providers.nautiljon.model.Chapter
import org.snd.metadata.providers.nautiljon.model.SearchResult
import org.snd.metadata.providers.nautiljon.model.Series
import org.snd.metadata.providers.nautiljon.model.SeriesId
import org.snd.metadata.providers.nautiljon.model.SeriesVolume
import org.snd.metadata.providers.nautiljon.model.Volume
import org.snd.metadata.providers.nautiljon.model.VolumeId
import java.net.URLDecoder
import java.time.LocalDate
import java.time.Year
import java.time.format.DateTimeFormatter


class NautiljonParser {
    private val baseUrl = "https://www.nautiljon.com"

    fun parseSearchResults(results: String): Collection<SearchResult> {
        val document = Jsoup.parse(results)
        val searchTable = document.getElementsByClass("search").firstOrNull()
            ?.getElementsByTag("tbody")?.firstOrNull() ?: return emptyList()
        val entries = searchTable.getElementsByTag("tr")

        return entries.map { entry ->
            val searchData = entry.children()
            val imageUrl = baseUrl + searchData[0].getElementsByTag("a").firstOrNull()?.attr("im")

            val titleElem = searchData[1].child(1)
            val id = titleElem.attr("href").removeSurrounding("/mangas/", ".html")
            val title = titleElem.attr("title")
            val altTitle = searchData[1].getElementsByClass("infos_small").firstOrNull()?.text()?.removeSurrounding("(", ")")

            val description = searchData[1].getElementsByTag("p").firstOrNull()?.wholeText()

            val type = searchData[2].text()
            val volumesNumber = searchData[3].text().toInt()
            val startDate = searchData[7].text().toIntOrNull()?.let { Year.of(it) }
            val score = searchData[8].text().removeSuffix("/10").toDoubleOrNull()

            SearchResult(
                id = SeriesId(id),
                title = title,
                alternativeTitle = altTitle,
                description = description,
                imageUrl = imageUrl,
                type = type,
                volumesNumber = volumesNumber,
                startDate = startDate,
                score = score
            )
        }
    }

    fun parseSeries(series: String): Series {
        val document = Jsoup.parse(series)
        val dataEntries = document.getElementsByClass("infosFicheTop").first()!!
            .getElementsByClass("liste_infos").first()!!
            .getElementsByTag("li")
        val (country, startYear) = parseCountryAndStartYear(dataEntries)
        val authorsStory = parseAuthorsStory(dataEntries)
        val authorsArt = parseAuthorsArt(dataEntries).ifEmpty { authorsStory }
        val (numberOfVolumes, status) = parseNumberOfVolumesAndStatus(dataEntries)

        val originalTitles = parseOriginalTitles(dataEntries)
        val (romajiTitle, japaneseTitle) = if (originalTitles.size == 2) {
            originalTitles[0] to originalTitles[1]
        } else {
            null to null
        }

        return Series(
            id = parseSeriesId(document),
            title = parseTitle(document),
            alternativeTitles = parseAlternativeTitles(dataEntries),
            romajiTitle = romajiTitle,
            japaneseTitle = japaneseTitle,
            description = parseDescription(document),
            imageUrl = parseImageUrl(document),
            country = country,
            startYear = startYear,
            type = parseType(dataEntries),
            status = status,
            numberOfVolumes = numberOfVolumes,
            genres = parseGenres(dataEntries),
            themes = parseThemes(dataEntries),
            authorsStory = authorsStory,
            authorsArt = authorsArt,
            originalPublisher = parseOriginalPublisher(dataEntries),
            frenchPublisher = parseFrenchPublisher(dataEntries),
            recommendedAge = parseRecommendedAge(dataEntries),
            score = parseScore(document),
            volumes = parseVolumes(document)
        )
    }

    fun parseVolume(volume: String): Volume {
        val document = Jsoup.parse(volume)
        val dataEntries = document.getElementsByClass("infosFicheTop").first()!!
            .getElementsByClass("liste_infos").first()!!
            .getElementsByTag("li")
        val authorsStory = parseAuthorsStory(dataEntries)
        val authorsArt = parseAuthorsArt(dataEntries).ifEmpty { authorsStory }
        val seriesId = document.getElementById("infos_fiche_manga")!!.children()
            .first { it.child(0).tagName() == "h3" }.child(0).child(0).attr("href")
            .removeSurrounding("/mangas/", ".html")
        return Volume(
            id = parseVolumeId(document),
            seriesId = SeriesId(URLDecoder.decode(seriesId, "UTF-8")),
            number = parseVolumeNumber(document),
            originalPublisher = parseOriginalPublisher(dataEntries),
            frenchPublisher = parseFrenchPublisher(dataEntries),
            originalReleaseDate = parseVolumeOriginalReleaseDate(dataEntries),
            frenchReleaseDate = parseVolumeFrenchReleaseDate(dataEntries),
            numberOfPages = parseNumberOfPages(dataEntries),
            description = parseDescription(document),
            score = parseScore(document),
            imageUrl = parseImageUrl(document),
            chapters = parseChapters(document),
            authorsStory = authorsStory,
            authorsArt = authorsArt,
        )
    }

    private fun parseTitle(document: Document): String {
        return document.getElementsByClass("h1titre").first()
            ?.getElementsByAttributeValue("itemprop", "name")?.first()!!.text()
    }

    private fun parseImageUrl(document: Document): String? {
        val relativeUrl = document.getElementsByClass("infosFicheTop").first()
            ?.getElementsByClass("image_fiche")?.first()
            ?.getElementById("onglets_3_couverture")
            ?.attr("href")

        return relativeUrl?.let { baseUrl + it }
    }

    private fun parseScore(document: Document): Double? {
        return document.getElementsByClass("infosFicheTop").first()
            ?.getElementsByClass("stats_notes")?.first()
            ?.getElementsByAttributeValue("itemprop", "ratingValue")?.first()
            ?.text()?.toDoubleOrNull()
    }

    private fun parseDescription(document: Document): String? {
        return document.getElementsByClass("description").firstOrNull()?.wholeText()
            ?.let { if (it == "N/C") null else it }
    }

    private fun parseAlternativeTitles(dataEntries: Elements): Collection<String> {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Titre alternatif :") }
            ?.textNodes()?.first()?.text()?.split("/")?.map { it.trim() }
            ?: emptyList()
    }

    private fun parseOriginalTitles(dataEntries: Elements): List<String> {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Titre original :") }
            ?.textNodes()?.first()?.text()?.split("/")?.map { it.trim() }
            ?: emptyList()
    }

    private fun parseCountryAndStartYear(dataEntries: Elements): Pair<String?, Year?> {
        val countryNode = dataEntries
            .firstOrNull { it.child(0).text().equals("Origine :") }
        val country = countryNode?.textNodes()?.joinToString("") { it.text() }
            ?.replace("-", "")?.trim()
        val startYear = countryNode?.getElementsByAttributeValue("itemprop", "datePublished")
            ?.attr("content")?.toIntOrNull()?.let { Year.of(it) }

        return country to startYear
    }

    private fun parseType(dataEntries: Elements): String? {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Type :") }?.child(1)?.text()
    }

    private fun parseGenres(dataEntries: Elements): Collection<String> {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Genres :") }
            ?.getElementsByTag("a")?.map { it.text() }
            ?: emptyList()
    }

    private fun parseThemes(dataEntries: Elements): Collection<String> {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Thèmes :") }
            ?.getElementsByTag("a")?.map { it.text() }
            ?: emptyList()
    }

    private fun parseAuthorsStory(dataEntries: Elements): Collection<String> {
        return dataEntries
            .filter { entry ->
                val node = entry.child(0).text()
                node.equals("Auteur :") || node.equals("Auteur original :") || node.equals("Scénariste :")
            }
            .map { it.getElementsByTag("a").text() }
    }

    private fun parseAuthorsArt(dataEntries: Elements): Collection<String> {
        return dataEntries
            .filter { entry -> entry.child(0).text().equals("Dessinateur :") }
            .map { it.getElementsByTag("a").text() }
    }

    private fun parseOriginalPublisher(dataEntries: Elements): String? {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Éditeur VO :") }
            ?.getElementsByAttributeValue("itemprop", "publisher")?.first()?.text()
    }

    private fun parseFrenchPublisher(dataEntries: Elements): String? {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Éditeur VF :") }
            ?.getElementsByAttributeValue("itemprop", "publisher")?.first()?.text()
    }

    private fun parseNumberOfVolumesAndStatus(dataEntries: Elements): Pair<Int?, String?> {
        val numberOfVolumesRegex = "([0-9].*)\\s?(\\(.*\\))".toRegex()
        val numberOfVolumesText = dataEntries
            .firstOrNull { it.child(0).text().equals("Nb volumes VO :") }?.textNodes()?.first()?.text()
        val regexGroups = numberOfVolumesText?.let { numberOfVolumesRegex.find(it)?.groupValues }
        val numberOfVolumes = regexGroups?.get(1)?.trim()?.toIntOrNull()
        val status = regexGroups?.get(2)?.removeSurrounding("(", ")")

        return numberOfVolumes to status
    }

    private fun parseVolumeOriginalReleaseDate(dataEntries: Elements): LocalDate? {
        return runCatching {
            val dateElement = dataEntries
                .firstOrNull { it.child(0).text().equals("Date de parution VO :") }

            (dateElement?.getElementsByAttributeValue("itemprop", "datePublished")?.first()?.text()
                ?: dateElement?.textNodes()?.firstOrNull()?.text())
                ?.trim()
                ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
        }.getOrNull()
    }

    private fun parseVolumeFrenchReleaseDate(dataEntries: Elements): LocalDate? {
        return runCatching {
            val dateElement = dataEntries
                .firstOrNull { it.child(0).text().equals("Date de parution VF :") }

            (dateElement?.getElementsByAttributeValue("itemprop", "datePublished")?.first()?.text()
                ?: dateElement?.textNodes()?.firstOrNull()?.text())
                ?.trim()
                ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }
        }.getOrNull()
    }

    private fun parseNumberOfPages(dataEntries: Elements): Int? {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Nombre de pages :") }
            ?.getElementsByAttributeValue("itemprop", "numberOfPages")?.first()?.text()
            ?.toIntOrNull()
    }

    private fun parseRecommendedAge(dataEntries: Elements): Int? {
        return dataEntries
            .firstOrNull { it.child(0).text().equals("Âge conseillé :") }
            ?.textNodes()?.firstOrNull()?.text()
            ?.removeSuffix(" ans et +")?.trim()
            ?.toIntOrNull()
    }

    private fun parseVolumes(document: Document): Collection<SeriesVolume> {
        val volumesBlock = document.getElementsByClass("top_bloc")
            .firstOrNull { it.child(0).text() == "Volumes" }
            ?.child(1)?.children()
            ?.filter { element -> element.tag().name == "h2" || element.tag().name == "div" }
            ?: emptyList()

        return if (volumesBlock.size == 1) {
            parseEditionVolumes(null, volumesBlock.first())
        } else {

            volumesBlock
                .chunked(2)
                .flatMap { (edition, volumes) -> parseEditionVolumes(edition, volumes) }
        }
    }

    private fun parseEditionVolumes(edition: Element?, volumes: Element): List<SeriesVolume> {
        val volumeElements = volumes.children()
            .filter { element -> element.tag().name == "h3" || element.tag().name == "div" }
        if (volumeElements.isEmpty()) return emptyList()

        return volumeElements.asSequence().chunked(2)
            .map { (type, volumeElements) ->
                type.text() to volumeElements.getElementsByClass("unVol")
            }.first()
            .let { (type, volumeElements) ->
                volumeElements.mapNotNull { volume ->
                    val volumeName = volume.child(1).text()
                    val volumeNumber = volumeName.removePrefix("Vol. ").toIntOrNull()
                    if (volumeNumber == null) null
                    else
                        SeriesVolume(
                            id = parseVolumeId(volume),
                            number = volumeNumber,
                            edition = edition?.let { parseEditionName(it) },
                            type = type,
                            name = volumeName
                        )
                }
            }
    }

    private fun parseVolumeId(volume: Element): VolumeId {
        val id = ".*/volume-(.*).html".toRegex().find(volume.child(0).attr("href"))?.groupValues!![1]
        return VolumeId(id)
    }

    private fun parseEditionName(edition: Element): String? {
        val editionName = edition.child(0).textNodes().first().text().trim()
            .replace("^[EÉ]dition ".toRegex(), "")
            .let { ("\\((.*?)\\)".toRegex().find(it)?.groupValues?.get(1) ?: it) }
            .replace(" [EÉ]dition$".toRegex(), "")

        return if (editionName == "par défaut") null
        else editionName.lowercase()
    }

    private fun parseVolumeNumber(document: Document): Int {
        val title = parseTitle(document)

        return ".*Vol. ([0-9]*)".toRegex().find(title)?.groupValues!![1].toInt()
    }

    private fun parseVolumeId(document: Document): VolumeId {
        val id = document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content").let {
                ".*/volume-(.*).html".toRegex().find(it)?.groupValues!![1]
            }
        return VolumeId(id)
    }

    private fun parseSeriesId(document: Document): SeriesId {
        val id = document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content")
            .removeSurrounding("$baseUrl/mangas/", ".html")

        return SeriesId(URLDecoder.decode(id, "UTF-8"))
    }

    private fun parseChapters(document: Document): Collection<Chapter> {
        val regex = "(Chapitre (?<number>[0-9]*))( : (?<name>.*))?".toRegex()
        return document.getElementsByClass("chapitres").firstOrNull()?.textNodes()
            ?.mapNotNull {
                val groups = regex.find(it.text())?.groups
                val number = groups?.get("number")?.value?.toIntOrNull()
                val name = groups?.get("number")?.value
                number?.let {
                    Chapter(name, number)
                }
            }
            ?: emptyList()
    }
}
