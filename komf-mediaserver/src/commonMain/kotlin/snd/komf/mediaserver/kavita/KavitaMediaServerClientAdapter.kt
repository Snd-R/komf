package snd.komf.mediaserver.kavita

import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.kavita.model.KavitaAgeRating
import snd.komf.mediaserver.kavita.model.KavitaAgeRating.UNKNOWN
import snd.komf.mediaserver.kavita.model.KavitaAuthor
import snd.komf.mediaserver.kavita.model.KavitaChapter
import snd.komf.mediaserver.kavita.model.KavitaGenre
import snd.komf.mediaserver.kavita.model.KavitaLibrary
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.COLORIST
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.COVER_ARTIST
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.EDITOR
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.INKER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.LETTERER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.PENCILLER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.PUBLISHER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.TRANSLATOR
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.WRITER
import snd.komf.mediaserver.kavita.model.KavitaPublicationStatus
import snd.komf.mediaserver.kavita.model.KavitaSeries
import snd.komf.mediaserver.kavita.model.KavitaSeriesId
import snd.komf.mediaserver.kavita.model.KavitaSeriesMetadata
import snd.komf.mediaserver.kavita.model.KavitaTag
import snd.komf.mediaserver.kavita.model.KavitaVolume
import snd.komf.mediaserver.kavita.model.request.KavitaSeriesMetadataUpdateRequest
import snd.komf.mediaserver.kavita.model.request.KavitaSeriesUpdateRequest
import snd.komf.mediaserver.kavita.model.toKavitaChapterId
import snd.komf.mediaserver.kavita.model.toKavitaLibraryId
import snd.komf.mediaserver.kavita.model.toKavitaSeriesId
import snd.komf.mediaserver.model.MediaServerAlternativeTitle
import snd.komf.mediaserver.model.MediaServerAuthor
import snd.komf.mediaserver.model.MediaServerBook
import snd.komf.mediaserver.model.MediaServerBookId
import snd.komf.mediaserver.model.MediaServerBookMetadata
import snd.komf.mediaserver.model.MediaServerBookMetadataUpdate
import snd.komf.mediaserver.model.MediaServerBookThumbnail
import snd.komf.mediaserver.model.MediaServerLibrary
import snd.komf.mediaserver.model.MediaServerLibraryId
import snd.komf.mediaserver.model.MediaServerSeries
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerSeriesMetadata
import snd.komf.mediaserver.model.MediaServerSeriesMetadataUpdate
import snd.komf.mediaserver.model.MediaServerSeriesThumbnail
import snd.komf.mediaserver.model.MediaServerThumbnailId
import snd.komf.mediaserver.model.Page
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.SeriesStatus
import snd.komf.model.WebLink
import java.nio.file.Path

class KavitaMediaServerClientAdapter(private val kavitaClient: KavitaClient) : MediaServerClient {

    override suspend fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries {
        val kavitaSeriesId = seriesId.toKavitaSeriesId()
        val series = kavitaClient.getSeries(kavitaSeriesId)
        val metadata = kavitaClient.getSeriesMetadata(kavitaSeriesId)
        val details = kavitaClient.getSeriesDetails(kavitaSeriesId)
        return series.toMediaServerSeries(metadata, details.totalCount)
    }

    override suspend fun getSeries(libraryId: MediaServerLibraryId, pageNumber: Int): Page<MediaServerSeries> {
        val kavitaLibraryId = libraryId.toKavitaLibraryId()
        val kavitaPage = kavitaClient.getSeries(kavitaLibraryId, pageNumber)
        return Page(
            content = kavitaPage.content.map {
                val metadata = kavitaClient.getSeriesMetadata(it.id)
                val details = kavitaClient.getSeriesDetails(it.id)
                it.toMediaServerSeries(metadata, details.totalCount)
            },
            pageNumber = kavitaPage.currentPage,
            totalElements = kavitaPage.totalItems,
            totalPages = kavitaPage.totalPages
        )
    }

    override suspend fun getSeriesThumbnail(seriesId: MediaServerSeriesId): Image? {
        return runCatching { kavitaClient.getSeriesCover(seriesId.toKavitaSeriesId()) }.getOrNull()
    }

    override suspend fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail> {
        return emptyList()
    }

    override suspend fun getBook(bookId: MediaServerBookId): MediaServerBook {
        val chapterId = bookId.toKavitaChapterId()
        val chapter = kavitaClient.getChapter(chapterId)
        val volume = kavitaClient.getVolume(chapter.volumeId)

        return chapter.toMediaServerBook(volume)
    }

    override suspend fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook> {
        return kavitaClient.getVolumes(seriesId.toKavitaSeriesId())
            .flatMap { volume -> volume.chapters.map { it.toMediaServerBook(volume) } }
    }

    override suspend fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail> {
        return emptyList()
    }

    override suspend fun getBookThumbnail(bookId: MediaServerBookId): Image? {
        return runCatching { kavitaClient.getChapterCover(bookId.toKavitaChapterId()) }.getOrNull()
    }

    override suspend fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary {
        return kavitaClient.getLibraries().first { it.id == libraryId.toKavitaLibraryId() }
            .toMediaServerLibrary()
    }

    override suspend fun updateSeriesMetadata(
        seriesId: MediaServerSeriesId,
        metadata: MediaServerSeriesMetadataUpdate
    ) {
        val localizedName = metadata.alternativeTitles?.find { it.language != null }
        if (metadata.title != null || localizedName != null) {
            val series = kavitaClient.getSeries(seriesId.toKavitaSeriesId())
            kavitaClient.updateSeries(
                series.toKavitaTitleUpdate(
                    metadata.title?.name,
                    metadata.titleSort?.name,
                    localizedName?.name
                )
            )
        }

        val oldMetadata = kavitaClient.getSeriesMetadata(seriesId.toKavitaSeriesId())
        kavitaClient.updateSeriesMetadata(metadata.toKavitaSeriesMetadataUpdate(oldMetadata))
    }

    override suspend fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId) {
        val series = kavitaClient.getSeries(seriesId.toKavitaSeriesId())
        kavitaClient.updateSeries(series.toKavitaCoverResetRequest())
    }

    override suspend fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate) {}

    override suspend fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId) {}

    override suspend fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?) {
        kavitaClient.resetChapterLock(bookId.toKavitaChapterId())
    }

    override suspend fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
        val series = kavitaClient.getSeries(seriesId.toKavitaSeriesId())
        kavitaClient.updateSeries(series.toKavitaCoverResetRequest())
        kavitaClient.updateSeriesMetadata(kavitaSeriesResetRequest(seriesId.toKavitaSeriesId()))
    }

    override suspend fun uploadSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image,
        selected: Boolean
    ): MediaServerSeriesThumbnail? {
        kavitaClient.uploadSeriesCover(seriesId.toKavitaSeriesId(), thumbnail)
        return null
    }

    override suspend fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image,
        selected: Boolean
    ): MediaServerBookThumbnail? {
        kavitaClient.uploadBookCover(bookId.toKavitaChapterId(), thumbnail)
        return null
    }

    override suspend fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId) {
        kavitaClient.scanLibrary(libraryId.toKavitaLibraryId())
        kavitaClient.scanSeries(seriesId.toKavitaSeriesId())
    }
}


private fun KavitaSeries.toMediaServerSeries(metadata: KavitaSeriesMetadata, bookCount: Int): MediaServerSeries {
    return MediaServerSeries(
        id = MediaServerSeriesId(id.value.toString()),
        libraryId = MediaServerLibraryId(libraryId.toString()),
        name = originalName,
        booksCount = bookCount,
        metadata = metadata.toMediaServerSeriesMetadata(this),
        url = folderPath,
        deleted = false,
    )
}

private fun KavitaChapter.toMediaServerBook(volume: KavitaVolume): MediaServerBook {
    val filePath = Path.of(files.first().filePath)
    val fileName = filePath.fileName.toString()

    return MediaServerBook(
        id = MediaServerBookId(id.value.toString()),
        seriesId = MediaServerSeriesId(volume.seriesId.toString()),
        libraryId = null,
        seriesTitle = title,
        name = fileName,
        url = filePath.toString(),
        number = number?.toIntOrNull() ?: 0,
        oneshot = false,
        metadata = toMediaServerBookMetadata(),
        deleted = false,
    )
}

private fun KavitaChapter.toMediaServerBookMetadata(): MediaServerBookMetadata {
    val authors = writers.map { MediaServerAuthor(it.name, AuthorRole.WRITER.name) } +
            coverArtists.map { MediaServerAuthor(it.name, AuthorRole.COVER.name) } +
            pencillers.map { MediaServerAuthor(it.name, AuthorRole.PENCILLER.name) } +
            letterers.map { MediaServerAuthor(it.name, AuthorRole.LETTERER.name) } +
            inkers.map { MediaServerAuthor(it.name, AuthorRole.INKER.name) } +
            colorists.map { MediaServerAuthor(it.name, AuthorRole.COLORIST.name) } +
            editors.map { MediaServerAuthor(it.name, AuthorRole.EDITOR.name) } +
            translators.map { MediaServerAuthor(it.name, AuthorRole.TRANSLATOR.name) }

    return MediaServerBookMetadata(
        title = title,
        summary = summary?.ifBlank { null },
        number = number ?: "0",
        numberSort = number,
        releaseDate = releaseDate.date,
        authors = authors,
        tags = tags.map { it.title },
        isbn = isbn.ifBlank { null },
        links = emptyList(),

        titleLock = false,
        summaryLock = false,
        numberLock = false,
        numberSortLock = false,
        releaseDateLock = false,
        authorsLock = false,
        tagsLock = false,
        isbnLock = false,
        linksLock = false,
    )
}

private fun KavitaLibrary.toMediaServerLibrary() = MediaServerLibrary(
    id = MediaServerLibraryId(id.value.toString()),
    name = name,
    roots = folders
)

private fun KavitaSeriesMetadata.toMediaServerSeriesMetadata(series: KavitaSeries): MediaServerSeriesMetadata {
    val status = when (publicationStatus) {
        KavitaPublicationStatus.ONGOING -> SeriesStatus.ONGOING
        KavitaPublicationStatus.HIATUS -> SeriesStatus.HIATUS
        KavitaPublicationStatus.COMPLETED -> SeriesStatus.COMPLETED
        KavitaPublicationStatus.CANCELLED -> SeriesStatus.ABANDONED
        KavitaPublicationStatus.ENDED -> SeriesStatus.ENDED
    }
    val authors = writers.map { MediaServerAuthor(it.name, AuthorRole.WRITER.name) } +
            coverArtists.map { MediaServerAuthor(it.name, AuthorRole.COVER.name) } +
            pencillers.map { MediaServerAuthor(it.name, AuthorRole.PENCILLER.name) } +
            letterers.map { MediaServerAuthor(it.name, AuthorRole.LETTERER.name) } +
            inkers.map { MediaServerAuthor(it.name, AuthorRole.INKER.name) } +
            colorists.map { MediaServerAuthor(it.name, AuthorRole.COLORIST.name) } +
            editors.map { MediaServerAuthor(it.name, AuthorRole.EDITOR.name) } +
            translators.map { MediaServerAuthor(it.name, AuthorRole.TRANSLATOR.name) }

    val authorsLock = sequenceOf(
        writerLocked,
        coverArtistLocked,
        pencillerLocked,
        lettererLocked,
        inkerLocked,
        coloristLocked,
        editorLocked,
        translatorLocked
    ).any { it } //TODO per role locks?

    return MediaServerSeriesMetadata(
        status = status,
        title = series.name,
        titleSort = series.sortName,
        alternativeTitles = series.localizedName?.let { listOf(MediaServerAlternativeTitle("Localized", it)) }
            ?: emptyList(),
        summary = summary ?: "",
        readingDirection = null,
        publisher = null,
        alternativePublishers = publishers.map { it.name }.toSet(),
        ageRating = ageRating.ageRating,
        language = language,
        genres = genres.map { it.title },
        tags = tags.map { it.title },
        totalBookCount = if (totalCount == 0) null else totalCount,
        authors = authors,
        releaseYear = releaseYear,
        links = webLinks?.split(",")?.map { WebLink(it, it) } ?: emptyList(),

        statusLock = publicationStatusLocked,
        titleLock = series.nameLocked,
        titleSortLock = series.sortNameLocked,
        summaryLock = summaryLocked,
        readingDirectionLock = false,
        publisherLock = publisherLocked,
        ageRatingLock = ageRatingLocked,
        languageLock = languageLocked,
        genresLock = genresLocked,
        tagsLock = tagsLocked,
        totalBookCountLock = false,
        authorsLock = authorsLock,
        releaseYearLock = releaseYearLocked,
        alternativeTitlesLock = series.localizedNameLocked,
        linksLock = false,
    )
}

private fun MediaServerSeriesMetadataUpdate.toKavitaSeriesMetadataUpdate(oldMeta: KavitaSeriesMetadata): KavitaSeriesMetadataUpdateRequest {
    val status = when (status) {
        SeriesStatus.ENDED -> KavitaPublicationStatus.ENDED
        SeriesStatus.ONGOING -> KavitaPublicationStatus.ONGOING
        SeriesStatus.ABANDONED -> KavitaPublicationStatus.CANCELLED
        SeriesStatus.HIATUS -> KavitaPublicationStatus.HIATUS
        SeriesStatus.COMPLETED -> KavitaPublicationStatus.COMPLETED
        null -> null
    }
    val publishers =
        if (publisher == null && alternativePublishers == null) oldMeta.publishers
        else ((alternativePublishers ?: emptyList()) + listOfNotNull(publisher))
            .map { KavitaAuthor(id = 0, name = it, role = PUBLISHER) }.toSet()

    val authors = authors?.groupBy { it.role.lowercase() }
    val ageRating = ageRating
        ?.let { metadataRating ->
            KavitaAgeRating.entries
                .filter { it.ageRating != null }
                .sortedBy { it.ageRating }
                .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                ?: KavitaAgeRating.ADULTS_ONLY
        }

    val metadata = oldMeta.copy(
        publicationStatus = status ?: oldMeta.publicationStatus,
        summary = summary ?: oldMeta.summary,
        publishers = publishers,
        genres = genres?.let { deduplicate(it) }?.map { KavitaGenre(id = 0, title = it) }?.toSet() ?: oldMeta.genres,
        tags = tags?.let { deduplicate(it) }?.map { KavitaTag(id = 0, title = it) }?.toSet() ?: oldMeta.tags,
        writers = authors
            ?.get(AuthorRole.WRITER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = WRITER) }?.toSet()
            ?.ifEmpty { oldMeta.writers } ?: oldMeta.writers,
        coverArtists = authors
            ?.get(AuthorRole.COVER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = COVER_ARTIST) }?.toSet()
            ?.ifEmpty { oldMeta.coverArtists } ?: oldMeta.coverArtists,
        pencillers = authors
            ?.get(AuthorRole.PENCILLER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = PENCILLER) }?.toSet()
            ?.ifEmpty { oldMeta.pencillers } ?: oldMeta.pencillers,
        inkers = authors
            ?.get(AuthorRole.INKER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = INKER) }?.toSet()
            ?.ifEmpty { oldMeta.inkers } ?: oldMeta.inkers,
        colorists = authors
            ?.get(AuthorRole.COLORIST.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = COLORIST) }?.toSet()
            ?.ifEmpty { oldMeta.colorists } ?: oldMeta.colorists,
        letterers = authors
            ?.get(AuthorRole.LETTERER.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = LETTERER) }?.toSet()
            ?.ifEmpty { oldMeta.letterers } ?: oldMeta.letterers,
        editors = authors
            ?.get(AuthorRole.EDITOR.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = EDITOR) }?.toSet() ?: oldMeta.editors,
        translators = authors
            ?.get(AuthorRole.TRANSLATOR.name.lowercase())
            ?.map { KavitaAuthor(id = 0, name = it.name, role = TRANSLATOR) }?.toSet() ?: oldMeta.translators,
        ageRating = ageRating ?: oldMeta.ageRating,
        language = language ?: oldMeta.language,
        releaseYear = releaseYear ?: oldMeta.releaseYear,
        webLinks = links?.joinToString(separator = ",") { it.url } ?: oldMeta.webLinks
    )
    return KavitaSeriesMetadataUpdateRequest(metadata)
}

private val normalizeRegex = "[^\\p{L}0-9+!]".toRegex()
private fun deduplicate(values: Collection<String>) = values
    .map { normalizeRegex.replace(it, "").trim().lowercase() to it }
    .distinctBy { (normalized, _) -> normalized }
    .map { (_, value) -> value }

private fun kavitaSeriesResetRequest(seriesId: KavitaSeriesId): KavitaSeriesMetadataUpdateRequest {
    val metadata = KavitaSeriesMetadata(
        id = 0,
        seriesId = seriesId,
        summary = "",
        genres = emptySet(),
        tags = emptySet(),
        writers = emptySet(),
        coverArtists = emptySet(),
        publishers = emptySet(),
        characters = emptySet(),
        pencillers = emptySet(),
        inkers = emptySet(),
        colorists = emptySet(),
        letterers = emptySet(),
        editors = emptySet(),
        translators = emptySet(),
        ageRating = UNKNOWN,
        releaseYear = 0,
        language = "",
        maxCount = 0,
        totalCount = 0,
        publicationStatus = KavitaPublicationStatus.ONGOING,
        webLinks = "",

        languageLocked = false,
        summaryLocked = false,
        ageRatingLocked = false,
        publicationStatusLocked = false,
        genresLocked = false,
        tagsLocked = false,
        writerLocked = false,
        characterLocked = false,
        coloristLocked = false,
        editorLocked = false,
        inkerLocked = false,
        lettererLocked = false,
        pencillerLocked = false,
        publisherLocked = false,
        translatorLocked = false,
        coverArtistLocked = false,
        releaseYearLocked = false,
    )
    return KavitaSeriesMetadataUpdateRequest(metadata)
}

private fun KavitaSeries.toKavitaTitleUpdate(newName: String?, newSortName: String?, newLocalizedName: String?) =
    KavitaSeriesUpdateRequest(
        id = id,
        name = newName?.trim() ?: name,
        sortName = newSortName?.trim() ?: sortName,
        localizedName = newLocalizedName?.trim() ?: localizedName,
        nameLocked = nameLocked,
        sortNameLocked = sortNameLocked,
        localizedNameLocked = localizedNameLocked,

        coverImageLocked = coverImageLocked
    )

private fun KavitaSeries.toKavitaCoverResetRequest() = KavitaSeriesUpdateRequest(
    id = id,
    name = name,
    localizedName = localizedName,
    sortName = sortName,
    nameLocked = false,
    sortNameLocked = false,
    localizedNameLocked = false,

    coverImageLocked = false
)
