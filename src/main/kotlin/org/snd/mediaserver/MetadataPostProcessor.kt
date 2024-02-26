package org.snd.mediaserver

import org.snd.common.StringUtils
import org.snd.config.MetadataPostProcessingConfig
import org.snd.mediaserver.model.SeriesAndBookMetadata
import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.metadata.BookNameParser
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesTitle

class MetadataPostProcessor(
    private val config: MetadataPostProcessingConfig,
    private val libraryType: MediaType
) {

    fun process(metadata: SeriesAndBookMetadata): SeriesAndBookMetadata {
        val seriesMetadata = postProcessSeries(metadata.seriesMetadata)
        val bookMetadata = postProcessBooks(metadata.bookMetadata)
        return SeriesAndBookMetadata(seriesMetadata, bookMetadata)
    }

    private fun postProcessSeries(series: SeriesMetadata): SeriesMetadata {
        val altTitles = if (config.alternativeSeriesTitles)
            series.titles.asSequence()
                .filter { (it.language == null || it.language in config.alternativeSeriesTitleLanguages) }
                .sortedWith(compareBy(nullsLast()) { it.language })
                .distinctBy { distinctName(it.name) }
                .toList()
        else emptyList()

        val seriesTitle = chooseSeriesTitle(series) ?: altTitles.firstOrNull()
        val altsWithoutSeriesTitle =
            if (seriesTitle != null) altTitles.filter { distinctName(it.name) != distinctName(seriesTitle.name) }
            else altTitles

        val tags = if (config.scoreTag && series.score != null) series.tags.plus("score: ${series.score.toInt()}") else series.tags

        return series.copy(
            title = seriesTitle,
            titles = altsWithoutSeriesTitle,
            readingDirection = config.readingDirectionValue ?: series.readingDirection,
            language = config.languageValue ?: series.language,
            tags = tags,
        )
    }

    private fun postProcessBooks(books: Map<MediaServerBook, BookMetadata?>): Map<MediaServerBook, BookMetadata?> {
        return if (config.orderBooks) {
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
        if (!config.seriesTitle) return null

        val chosenTitle = if (config.seriesTitleLanguage == null) series.titles.firstOrNull()
        else series.titles.find { it.language == config.seriesTitleLanguage }

        return chosenTitle ?: series.title
    }

    private fun distinctName(title: String): String {
        return title.replace(" ", "")
            .let { StringUtils.stripAccents(it) }
            .let { StringUtils.replaceFullwidthChars(it) }
            .lowercase()
    }
}