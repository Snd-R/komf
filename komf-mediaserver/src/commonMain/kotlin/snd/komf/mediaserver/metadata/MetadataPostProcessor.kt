package snd.komf.mediaserver.metadata

import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.SeriesAndBookMetadata
import snd.komf.model.BookMetadata
import snd.komf.model.MediaType
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
    private val scoreTag: Boolean,
    private val readingDirectionValue: ReadingDirection?,
    private val languageValue: String?,
    private val fallbackToAltTitle: Boolean
) {

    fun process(metadata: SeriesAndBookMetadata): SeriesAndBookMetadata {
        val seriesMetadata = postProcessSeries(metadata.seriesMetadata)
        val bookMetadata = postProcessBooks(metadata.bookMetadata)

        val updated = handleKomgaOneshot(seriesMetadata, bookMetadata)

        return updated
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

        val tags = if (scoreTag) {
            series.score?.let { score -> series.tags.plus("score: ${score.toInt()}") } ?: series.tags
        } else series.tags

        return series.copy(
            title = seriesTitle,
            titles = altsWithoutSeriesTitle,
            readingDirection = readingDirectionValue ?: series.readingDirection,
            language = languageValue ?: series.language,
            tags = tags,
        )
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
                summary = metadata?.summary ?: seriesMetadata.summary,
                tags = metadata?.tags?.toSet() ?: seriesMetadata.tags.toSet(),
                links = metadata?.links?.toList() ?: seriesMetadata.links.toList(),
                thumbnail = metadata?.thumbnail ?: seriesMetadata.thumbnail
            )
        }.toMap()

        val newSeriesMetadata = seriesMetadata.copy(
            thumbnail = null //series thumbnail should be null for oneshots
        )

        return SeriesAndBookMetadata(newSeriesMetadata, newBookMetadata)
    }
}
