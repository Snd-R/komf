package snd.komf.mediaserver.metadata

import kotlinx.serialization.Serializable
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.SeriesAndBookMetadata
import snd.komf.model.BookMetadata
import snd.komf.model.MediaType
import snd.komf.model.PublisherType.ORIGINAL
import snd.komf.model.ReadingDirection
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesTitle
import snd.komf.util.BookNameParser
import snd.komf.util.replaceFullwidthChars
import snd.komf.util.stripAccents


class MetadataPostProcessor(
    private val libraryType: MediaType,

    private val seriesTitle: Boolean,
    private val seriesTitleLanguage: String?,
    private val alternativeSeriesTitles: Boolean,
    private val alternativeSeriesTitleLanguages: List<String>,
    private val orderBooks: Boolean,
    private val readingDirectionValue: ReadingDirection?,
    private val languageValue: String?,
    private val fallbackToAltTitle: Boolean,

    private val scoreTag: Boolean,
    private val scoreTagName: String?,
    private val originalPublisherTagName: String?,
    private val publisherTagNames: List<PublisherTagNameConfig>
) {

    fun process(metadata: SeriesAndBookMetadata): SeriesAndBookMetadata {
        val seriesMetadata = postProcessSeries(metadata.seriesMetadata)
        val bookMetadata = postProcessBooks(metadata.bookMetadata)

        return handleKomgaOneshot(seriesMetadata, bookMetadata)
    }

    private fun postProcessSeries(series: SeriesMetadata): SeriesMetadata {
        val altTitles = if (alternativeSeriesTitles)
            series.titles.asSequence()
                .filter { (it.language == null || it.language in alternativeSeriesTitleLanguages) }
                .sortedWith(compareBy(nullsLast()) { it.language })
                .distinctBy { distinctName(it.name) }
                .toList()
        else emptyList()

        val seriesTitle = if (seriesTitle) {
            chooseSeriesTitle(series) ?: if (fallbackToAltTitle) altTitles.firstOrNull() else null
        } else null

        val altsWithoutSeriesTitle =
            if (seriesTitle != null) altTitles.filter { distinctName(it.name) != distinctName(seriesTitle.name) }
            else altTitles

        val tags = series.tags.toMutableList()
        tags.addScoreTag(series)
        tags.addOriginalPublisherTag(series)
        publisherTagNames.forEach { tags.addPublisherTag(series, it.tagName, it.language) }

        return series.copy(
            title = seriesTitle,
            titles = altsWithoutSeriesTitle,
            readingDirection = readingDirectionValue ?: series.readingDirection,
            language = series.language ?: languageValue,
            tags = tags,
        )
    }

    private fun MutableList<String>.addScoreTag(series: SeriesMetadata) {
        val seriesScore = series.score?.toInt()
        if (scoreTagName != null && seriesScore != null) add("$scoreTagName: $seriesScore")
        else if (scoreTag && seriesScore != null) add("score: $seriesScore")
    }

    private fun MutableList<String>.addOriginalPublisherTag(
        series: SeriesMetadata,
    ) {
        val publishers = series.alternativePublishers + listOfNotNull(series.publisher)
        if (originalPublisherTagName != null) {
            publishers.firstOrNull { it.type == ORIGINAL }
                ?.let { publisher -> add("${originalPublisherTagName}: ${publisher.name}") }
        }
    }

    private fun MutableList<String>.addPublisherTag(
        series: SeriesMetadata,
        tagName: String,
        language: String,
    ) {
        val publishers = series.alternativePublishers + listOfNotNull(series.publisher)
        publishers.firstOrNull { it.languageTag.equals(language, true) }
            ?.let { publisher -> add("${tagName}: ${publisher.name}") }

    }

    private fun postProcessBooks(books: Map<MediaServerBook, BookMetadata?>): Map<MediaServerBook, BookMetadata?> {
        return if (orderBooks) {
            books.map { (book, metadata) ->
                book to orderBook(book, (metadata ?: BookMetadata()))
            }.toMap()
        } else books
    }

    private fun orderBook(book: MediaServerBook, metadata: BookMetadata): BookMetadata {
        val range = when (libraryType) {
            MediaType.MANGA -> BookNameParser.getVolumes(book.name)
                ?: BookNameParser.getChapters(book.name)
                ?: BookNameParser.getBookNumber(book.name)

            MediaType.NOVEL, MediaType.COMIC -> BookNameParser.getBookNumber(book.name)

            MediaType.WEBTOON -> BookNameParser.getChapters(book.name)
                ?: BookNameParser.getBookNumber(book.name)
        }

        return metadata.copy(
            number = range ?: metadata.number,
            numberSort = range?.start ?: metadata.numberSort
        )
    }

    private fun chooseSeriesTitle(series: SeriesMetadata): SeriesTitle? {
        val chosenTitle = if (seriesTitleLanguage == null) series.titles.firstOrNull()
        else series.titles.find { it.language == seriesTitleLanguage }

        return chosenTitle ?: series.title
    }

    private fun distinctName(title: String): String {
        return replaceFullwidthChars(
            stripAccents(
                title.replace(" ", "")
            )
        ).lowercase()
    }

    private fun handleKomgaOneshot(
        seriesMetadata: SeriesMetadata,
        bookMetadata: Map<MediaServerBook, BookMetadata?>
    ): SeriesAndBookMetadata {
        if (bookMetadata.size > 1 || bookMetadata.keys.firstOrNull()?.oneshot == false) {
            return SeriesAndBookMetadata(seriesMetadata, bookMetadata)
        }

        val newBookMetadata = bookMetadata.map { (book, metadata) ->
            book to BookMetadata(
                title = metadata?.title,
                summary = metadata?.summary?.ifBlank { null } ?: seriesMetadata.summary,
                tags = metadata?.tags?.toSet()?.ifEmpty { null } ?: seriesMetadata.tags.toSet(),
                links = metadata?.links?.toList()?.ifEmpty { null } ?: seriesMetadata.links.toList(),
                thumbnail = metadata?.thumbnail ?: seriesMetadata.thumbnail
            )
        }.toMap()

        val newSeriesMetadata = seriesMetadata.copy(
            thumbnail = null //series thumbnail should be null for oneshots
        )

        return SeriesAndBookMetadata(newSeriesMetadata, newBookMetadata)
    }
}

@Serializable
data class PublisherTagNameConfig(
    val tagName: String,
    val language: String
)
