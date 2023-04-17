package org.snd.metadata.providers.comicvine

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole.COLORIST
import org.snd.metadata.model.metadata.AuthorRole.COVER
import org.snd.metadata.model.metadata.AuthorRole.EDITOR
import org.snd.metadata.model.metadata.AuthorRole.INKER
import org.snd.metadata.model.metadata.AuthorRole.LETTERER
import org.snd.metadata.model.metadata.AuthorRole.PENCILLER
import org.snd.metadata.model.metadata.AuthorRole.WRITER
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.BookRange
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.comicvine.model.ComicVineIssue
import org.snd.metadata.providers.comicvine.model.ComicVineVolume
import org.snd.metadata.providers.comicvine.model.ComicVineVolumeSearch
import java.time.LocalDate

class ComicVineMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {

    fun toSeriesSearchResult(volume: ComicVineVolumeSearch): SeriesSearchResult {
        return SeriesSearchResult(
            imageUrl = volume.image?.mediumUrl,
            title = seriesTitle(volume),
            provider = Provider.COMIC_VINE,
            resultId = volume.id.toString()
        )
    }

    fun toSeriesMetadata(
        volume: ComicVineVolume,
        cover: Image?
    ): ProviderSeriesMetadata {
        val metadata = SeriesMetadata(
            titles = listOf(SeriesTitle(volume.name, null, null)),
            summary = volume.description?.let { parseDescription(it) },
            publisher = volume.publisher?.name,
            releaseDate = ReleaseDate(volume.startYear?.toIntOrNull(), null, null),
            links = listOf(WebLink("ComicVine", volume.siteDetailUrl)),
            thumbnail = cover
        )
        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(volume.id.toString()),
            metadata = metadata,
            books = (volume.issues ?: emptyList()).map {
                SeriesBook(
                    id = ProviderBookId(it.id.toString()),
                    number = it.issueNumber?.toDoubleOrNull()
                        ?.let { number -> BookRange(number, number) },
                    name = it.name,
                    type = null,
                    edition = null
                )
            }
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(
        issue: ComicVineIssue,
        cover: Image?
    ): ProviderBookMetadata {
        val metadata = BookMetadata(
            title = issue.name,
            summary = issue.description?.let { parseDescription(it) },
            number = issue.issueNumber?.toDoubleOrNull()?.let { BookRange(it, it) },
            numberSort = issue.issueNumber?.toDoubleOrNull(),
            releaseDate = (issue.storeDate ?: issue.coverDate)?.let { LocalDate.parse(it) },
            authors = getAuthors(issue),
            links = listOf(WebLink("ComicVine", issue.siteDetailUrl)),
            thumbnail = cover
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(issue.id.toString()),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun getAuthors(issue: ComicVineIssue): List<Author> {
        return (issue.personCredits ?: emptyList())
            .flatMap { person -> person.role.split(", ").map { person.name to it } }
            .flatMap { (name, role) ->
                when (role) {
                    "writer", "plotter", "scripter" -> listOf(Author(name, WRITER))
                    "penciller", "penciler", "breakdowns" -> listOf(Author(name, PENCILLER))
                    "inker", "finishes" -> listOf(Author(name, INKER))
                    "colorist", "colourist", "colorer", "colourer" -> listOf(Author(name, COLORIST))
                    "letterer" -> listOf(Author(name, LETTERER))
                    "cover", "covers", "coverartist", "cover artist" -> listOf(Author(name, COVER))
                    "editor" -> listOf(Author(name, EDITOR))
                    "artist" -> listOf(Author(name, PENCILLER), Author(name, INKER))
                    else -> emptyList()
                }
            }
    }

    private fun parseDescription(description: String): String {
        return Jsoup.parse(description)
            .child(0).child(1).children()
            .joinToString("") { parseDescription(it) }
            .trim()
    }

    private fun parseDescription(node: Node): String {
        return when (node) {
            is Element -> parseDescriptionElement(node)
            is TextNode -> node.wholeText
            else -> ""
        }
    }

    private fun parseDescriptionElement(element: Element): String {
        val prependText = when (element.tag().name) {
            "br", "p", "h2", "h3", "h4" -> "\n\n"
            "li" -> "\n  - "
            else -> ""
        }

        return "$prependText${element.childNodes().joinToString("") { parseDescription(it) }}"
    }

    private fun seriesTitle(volume: ComicVineVolumeSearch): String {
        val startYearString = volume.startYear?.let { " ($it)" } ?: ""
        return "${volume.name}$startYearString"
    }
}