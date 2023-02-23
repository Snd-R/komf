package org.snd.mediaserver

import org.snd.config.MetadataPostProcessingConfig
import org.snd.mediaserver.model.SeriesAndBookMetadata
import org.snd.mediaserver.model.mediaserver.MediaServerBook
import org.snd.metadata.BookNameParser
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesTitle

class MetadataPostProcessor(
    private val config: MetadataPostProcessingConfig
) {

    fun process(metadata: SeriesAndBookMetadata): SeriesAndBookMetadata {
        val seriesMetadata = postProcessSeries(metadata.seriesMetadata)
        val bookMetadata = postProcessBooks(metadata.bookMetadata)
        return SeriesAndBookMetadata(seriesMetadata, bookMetadata)
    }

    private fun postProcessSeries(series: SeriesMetadata): SeriesMetadata {
        val seriesTitle = if (config.seriesTitle) seriesTitle(series.titles) ?: series.title else null

        val altTitles = if (config.alternativeSeriesTitles)
            series.titles.filter {
                it != seriesTitle &&
                        (it.language == null || it.language in config.alternativeSeriesTitleLanguages)
            }.sortedBy { it.language }.distinct()
        else emptyList()
        val tags = if (config.scoreTag && series.score != null) series.tags.plus("score: ${series.score.toInt()}") else series.tags

        return series.copy(
            title = seriesTitle,
            titles = altTitles,
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
        val range = BookNameParser.getVolumes(book.name) ?: BookNameParser.getChapters(book.name)

        return metadata.copy(
            number = range ?: metadata.number,
            numberSort = range?.start ?: metadata.numberSort
        )
    }

    private fun seriesTitle(titles: Collection<SeriesTitle>): SeriesTitle? {
        return titles.find { it.language == config.seriesTitleLanguage }
    }
}