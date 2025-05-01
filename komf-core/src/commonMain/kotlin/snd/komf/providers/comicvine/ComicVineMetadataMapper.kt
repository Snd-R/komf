package snd.komf.providers.comicvine

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import kotlinx.datetime.LocalDate
import snd.komf.model.Author
import snd.komf.model.AuthorRole.COLORIST
import snd.komf.model.AuthorRole.COVER
import snd.komf.model.AuthorRole.EDITOR
import snd.komf.model.AuthorRole.INKER
import snd.komf.model.AuthorRole.LETTERER
import snd.komf.model.AuthorRole.PENCILLER
import snd.komf.model.AuthorRole.WRITER
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.BookStoryArc
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.Publisher
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.comicvine.model.ComicVineIssue
import snd.komf.providers.comicvine.model.ComicVineStoryArc
import snd.komf.providers.comicvine.model.ComicVineVolume
import snd.komf.providers.comicvine.model.ComicVineVolumeSearch

class ComicVineMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
) {

    fun toSeriesSearchResult(volume: ComicVineVolumeSearch): SeriesSearchResult {
        return SeriesSearchResult(
            url = volume.siteDetailUrl,
            imageUrl = volume.image?.mediumUrl,
            title = seriesTitle(volume),
            provider = CoreProviders.COMIC_VINE,
            resultId = volume.id.toString()
        )
    }

    fun toSeriesMetadata(
        volume: ComicVineVolume,
        cover: Image?
    ): ProviderSeriesMetadata {
        val metadata = SeriesMetadata(
            title = SeriesTitle(volume.name, null, null),
            titles = listOf(SeriesTitle(volume.name, null, null)),
            summary = volume.description?.let { parseDescription(it) },
            publisher = volume.publisher?.name?.let { Publisher(it) },
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
        storyArcs: List<ComicVineStoryArc>,
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
            storyArcs = storyArcs.map { arc ->
                val index = arc.issues.indexOfFirst { it.id == issue.id }
                require(index != -1) { "Story arc does not contain this issue arcs:${arc.issues.map { it.id }}; issue ${issue.id} " }

                BookStoryArc(name = arc.name, number = index + 1)
            },
            thumbnail = cover,
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
        return Ksoup.parse(description)
            .child(0).child(1).children()
            .joinToString("") { parseDescription(it) }
            .trim()
    }

    private fun parseDescription(node: Node): String {
        return when (node) {
            is Element -> parseDescriptionElement(node)
            is TextNode -> node.getWholeText()
            else -> ""
        }
    }

    private fun parseDescriptionElement(element: Element): String {
        val prependText = when (element.tag().name()) {
            "br", "p", "h2", "h3", "h4" -> "\n\n"
            "li" -> "\n  - "
            else -> ""
        }

        return "$prependText${element.childNodes().joinToString("") { parseDescription(it) }}"
    }

    private fun seriesTitle(volume: ComicVineVolumeSearch): String {
        val publisher = volume.publisher?.name?.let { " ($it)" } ?: ""
        val startYearString = volume.startYear?.let { " ($it)" } ?: ""
        return "${volume.name}$startYearString$publisher"
    }
}