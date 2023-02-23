package org.snd.metadata

import org.snd.mediaserver.model.mediaserver.MediaServerBookId
import org.snd.metadata.comicinfo.model.ComicInfo
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.SeriesMetadata

object MetadataMerger {
    fun mergeSeriesMetadata(
        originalSeriesMetadata: SeriesMetadata,
        newSeriesMetadata: SeriesMetadata,
    ): SeriesMetadata {
        return SeriesMetadata(
            status = originalSeriesMetadata.status ?: newSeriesMetadata.status,
            titles = originalSeriesMetadata.titles + newSeriesMetadata.titles,
            summary = originalSeriesMetadata.summary ?: newSeriesMetadata.summary,
            publisher = originalSeriesMetadata.publisher ?: newSeriesMetadata.publisher,
            alternativePublishers = originalSeriesMetadata.alternativePublishers.ifEmpty { newSeriesMetadata.alternativePublishers },
            readingDirection = originalSeriesMetadata.readingDirection ?: newSeriesMetadata.readingDirection,
            ageRating = originalSeriesMetadata.ageRating ?: newSeriesMetadata.ageRating,
            language = originalSeriesMetadata.language ?: newSeriesMetadata.language,
            genres = originalSeriesMetadata.genres.ifEmpty { newSeriesMetadata.genres },
            tags = originalSeriesMetadata.tags.ifEmpty { newSeriesMetadata.tags },
            totalBookCount = originalSeriesMetadata.totalBookCount ?: newSeriesMetadata.totalBookCount,
            authors = originalSeriesMetadata.authors.ifEmpty { newSeriesMetadata.authors },
            thumbnail = originalSeriesMetadata.thumbnail ?: newSeriesMetadata.thumbnail,
            links = (originalSeriesMetadata.links + newSeriesMetadata.links).distinctBy { it.label },
            score = originalSeriesMetadata.score ?: newSeriesMetadata.score,
        )
    }

    fun mergeBookMetadata(
        originalBookMetadata: Map<MediaServerBookId, BookMetadata?>,
        newBookMetadata: Map<MediaServerBookId, BookMetadata?>,
    ): Map<MediaServerBookId, BookMetadata?> = (originalBookMetadata.asSequence() + newBookMetadata.asSequence()).distinct()
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, values) -> mergeBookMetadata(values) }

    private fun mergeBookMetadata(metadata: Collection<BookMetadata?>): BookMetadata? {
        return metadata.filterNotNull().reduceOrNull { old, new ->
            BookMetadata(
                title = old.title ?: new.title,
                summary = old.summary ?: new.summary,
                number = old.number ?: new.number,
                numberSort = old.numberSort ?: new.numberSort,
                releaseDate = old.releaseDate ?: new.releaseDate,
                authors = old.authors.ifEmpty { new.authors },
                tags = old.tags.ifEmpty { new.tags },
                isbn = old.isbn ?: new.isbn,
                links = old.links + new.links,
                chapters = old.chapters.ifEmpty { new.chapters },
                startChapter = old.startChapter ?: new.startChapter,
                endChapter = old.endChapter ?: new.endChapter,
                thumbnail = old.thumbnail ?: new.thumbnail,
            )
        }
    }

    fun mergeComicInfoMetadata(old: ComicInfo, new: ComicInfo): ComicInfo {
        return ComicInfo(
            title = new.title ?: old.title,
            series = new.series ?: old.series,
            number = new.number ?: old.number,
            count = new.count ?: old.count,
            volume = new.volume ?: old.volume,
            alternateSeries = new.alternateSeries ?: old.alternateSeries,
            alternateNumber = new.alternateNumber ?: old.alternateNumber,
            alternateCount = new.alternateCount ?: old.alternateCount,
            summary = new.summary ?: old.summary,
            notes = new.notes ?: old.notes,
            year = new.year ?: old.year,
            month = new.month ?: old.month,
            day = new.day ?: old.day,
            writer = new.writer ?: old.writer,
            penciller = new.penciller ?: old.penciller,
            inker = new.inker ?: old.inker,
            colorist = new.colorist ?: old.colorist,
            letterer = new.letterer ?: old.letterer,
            coverArtist = new.coverArtist ?: old.coverArtist,
            editor = new.editor ?: old.editor,
            translator = new.translator ?: old.translator,
            publisher = new.publisher ?: old.publisher,
            imprint = new.imprint ?: old.imprint,
            genre = new.genre ?: old.genre,
            tags = new.tags ?: old.tags,
            web = new.web ?: old.web,
            pageCount = new.pageCount ?: old.pageCount,
            languageISO = new.languageISO ?: old.languageISO,
            format = new.format ?: old.format,
            blackAndWhite = new.blackAndWhite ?: old.blackAndWhite,
            manga = new.manga ?: old.manga,
            characters = new.characters ?: old.characters,
            teams = new.teams ?: old.teams,
            locations = new.locations ?: old.locations,
            scanInformation = new.scanInformation ?: old.scanInformation,
            storyArc = new.storyArc ?: old.storyArc,
            seriesGroup = new.seriesGroup ?: old.seriesGroup,
            ageRating = new.ageRating ?: old.ageRating,
            rating = new.rating ?: old.rating,
            pages = new.pages ?: old.pages
        )
    }

}
