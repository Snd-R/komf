package snd.komf.mediaserver.kavita

import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.kavita.model.KavitaAgeRating
import snd.komf.mediaserver.kavita.model.KavitaAgeRating.UNKNOWN
import snd.komf.mediaserver.kavita.model.KavitaAuthor
import snd.komf.mediaserver.kavita.model.KavitaChapter
import snd.komf.mediaserver.kavita.model.KavitaChapterId
import snd.komf.mediaserver.kavita.model.KavitaGenre
import snd.komf.mediaserver.kavita.model.KavitaLibrary
import snd.komf.mediaserver.kavita.model.KavitaPublicationStatus
import snd.komf.mediaserver.kavita.model.KavitaSeries
import snd.komf.mediaserver.kavita.model.KavitaSeriesId
import snd.komf.mediaserver.kavita.model.KavitaSeriesMetadata
import snd.komf.mediaserver.kavita.model.KavitaTag
import snd.komf.mediaserver.kavita.model.KavitaVolume
import snd.komf.mediaserver.kavita.model.request.KavitaChapterMetadataUpdateRequest
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
import kotlin.io.path.nameWithoutExtension

class KavitaMediaServerClientAdapter(
    private val kavitaClient: KavitaClient,
    private val lockMatchedMetadata: Boolean
) : MediaServerClient {

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

    override suspend fun getLibraries(): List<MediaServerLibrary> {
        return kavitaClient.getLibraries().map { it.toMediaServerLibrary() }
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

    override suspend fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate) {
        val currentChapter = kavitaClient.getChapter(bookId.toKavitaChapterId())
        val request = metadata.toKavitaChapterMetadataUpdate(currentChapter)
        kavitaClient.updateChapterMetadata(request)
    }

    override suspend fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId) {}

    override suspend fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?) {
        kavitaClient.updateChapterMetadata(kavitaChapterResetRequest(bookId.toKavitaChapterId()))
    }

    override suspend fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
        val series = kavitaClient.getSeries(seriesId.toKavitaSeriesId())
        kavitaClient.updateSeries(series.toKavitaCoverResetRequest())
        kavitaClient.updateSeriesMetadata(kavitaSeriesResetRequest(seriesId.toKavitaSeriesId()))
    }

    override suspend fun uploadSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image,
        selected: Boolean,
        lock: Boolean
    ): MediaServerSeriesThumbnail? {
        kavitaClient.uploadSeriesCover(seriesId.toKavitaSeriesId(), thumbnail, lock)
        return null
    }

    override suspend fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image,
        selected: Boolean,
        lock: Boolean
    ): MediaServerBookThumbnail? {
        val chapterId = bookId.toKavitaChapterId()
        val chapter = kavitaClient.getChapter(chapterId)

        if (chapter.number == KavitaClient.LOOSE_LEAF_OR_DEFAULT_NUMBER.toString()) {
            // For chapter that doesn't have valid chapter number, i.e. Single Volume chapter in Kavita,
            // use uploadChapterCover api which updates both the chapter cover and the parent volume cover.
            kavitaClient.uploadChapterCover(chapterId, thumbnail, lock)
        } else {
            // Otherwise, only update parent volume cover.
            kavitaClient.uploadVolumeCover(chapter.volumeId, thumbnail, lock)
        }

        return null
    }

    override suspend fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId) {
        kavitaClient.scanLibrary(libraryId.toKavitaLibraryId())
        kavitaClient.scanSeries(seriesId.toKavitaSeriesId())
    }

    private fun <T> shouldLock(currentlyLocked: Boolean, newMetadata: T?): Boolean =
        currentlyLocked || (newMetadata != null && lockMatchedMetadata)

    private fun KavitaSeries.toKavitaTitleUpdate(newSortName: String?, newLocalizedName: String?) =
        KavitaSeriesUpdateRequest(
            id = id,
            sortName = newSortName?.trim() ?: sortName,
            localizedName = newLocalizedName?.trim() ?: localizedName,
            sortNameLocked = shouldLock(sortNameLocked, newSortName),
            localizedNameLocked = shouldLock(localizedNameLocked, newLocalizedName),

            coverImageLocked = coverImageLocked
        )

    private fun getRoles(authors: Map<String, List<MediaServerAuthor>>?, role: AuthorRole): Set<KavitaAuthor> = authors
        ?.get(role.name.lowercase())
        ?.map { KavitaAuthor(id = 0, name = it.name) }?.toSet() ?: emptySet()

    private fun MediaServerSeriesMetadataUpdate.toKavitaSeriesMetadataUpdate(currentMetadata: KavitaSeriesMetadata): KavitaSeriesMetadataUpdateRequest {
        val status = when (status) {
            SeriesStatus.ENDED -> KavitaPublicationStatus.ENDED
            SeriesStatus.ONGOING -> KavitaPublicationStatus.ONGOING
            SeriesStatus.ABANDONED -> KavitaPublicationStatus.CANCELLED
            SeriesStatus.HIATUS -> KavitaPublicationStatus.HIATUS
            SeriesStatus.COMPLETED -> KavitaPublicationStatus.COMPLETED
            null -> null
        }
        val publishers = ((alternativePublishers ?: emptyList()) + listOfNotNull(publisher))
            .map { KavitaAuthor(id = 0, name = it) }.toSet()
        val genres = genres?.let { deduplicate(it) }?.map { KavitaGenre(id = 0, title = it) }?.toSet() ?: emptySet()

        val authors = authors?.groupBy { it.role.lowercase() }
        val writers = getRoles(authors, AuthorRole.WRITER)
        val coverArtists = getRoles(authors, AuthorRole.COVER)
        val pencillers = getRoles(authors, AuthorRole.PENCILLER)
        val inkers = getRoles(authors, AuthorRole.INKER)
        val colorists = getRoles(authors, AuthorRole.COLORIST)
        val letterers = getRoles(authors, AuthorRole.LETTERER)
        val editors = getRoles(authors, AuthorRole.EDITOR)
        val translators = getRoles(authors, AuthorRole.TRANSLATOR)

        val ageRating = ageRating
            ?.let { metadataRating ->
                KavitaAgeRating.entries
                    .filter { it.ageRating != null }
                    .sortedBy { it.ageRating }
                    .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                    ?: KavitaAgeRating.ADULTS_ONLY
            }

        val metadata = KavitaSeriesMetadata(
            publicationStatus = status ?: currentMetadata.publicationStatus,
            summary = summary ?: currentMetadata.summary,
            publishers = publishers.ifEmpty { currentMetadata.publishers },
            genres = genres.ifEmpty { currentMetadata.genres },
            tags = tags?.let { deduplicate(it) }?.map { KavitaTag(id = 0, title = it) }?.toSet() ?: currentMetadata.tags,
            writers = writers.ifEmpty { currentMetadata.writers },
            coverArtists = coverArtists.ifEmpty { currentMetadata.coverArtists },
            pencillers = pencillers.ifEmpty { currentMetadata.pencillers },
            inkers = inkers.ifEmpty { currentMetadata.inkers },
            colorists = colorists.ifEmpty { currentMetadata.colorists },
            letterers = letterers.ifEmpty { currentMetadata.letterers },
            editors = editors.ifEmpty { currentMetadata.editors },
            translators = translators.ifEmpty { currentMetadata.translators },
            ageRating = ageRating ?: currentMetadata.ageRating,
            language = language ?: currentMetadata.language,
            releaseYear = releaseYear ?: currentMetadata.releaseYear,
            webLinks = links?.joinToString(separator = ",") { it.url } ?: currentMetadata.webLinks,

            // Passthrough data not provided by metadata provider
            id = currentMetadata.id,
            seriesId = currentMetadata.seriesId,
            characters = currentMetadata.characters,
            imprints = currentMetadata.imprints,
            teams = currentMetadata.teams,
            locations = currentMetadata.locations,
            maxCount = currentMetadata.maxCount,
            totalCount = currentMetadata.totalCount,

            // Set locks
            languageLocked = shouldLock(currentMetadata.languageLocked, language),
            summaryLocked = shouldLock(currentMetadata.summaryLocked, summary),
            ageRatingLocked = shouldLock(currentMetadata.ageRatingLocked, ageRating),
            publicationStatusLocked = shouldLock(currentMetadata.publicationStatusLocked, status),
            genresLocked = shouldLock(currentMetadata.genresLocked, genres.ifEmpty { null }),
            tagsLocked = shouldLock(currentMetadata.tagsLocked, tags),
            writerLocked = shouldLock(currentMetadata.writerLocked, writers.ifEmpty { null }),
            coloristLocked = shouldLock(currentMetadata.coloristLocked, colorists.ifEmpty { null }),
            editorLocked = shouldLock(currentMetadata.editorLocked, editors.ifEmpty { null }),
            inkerLocked = shouldLock(currentMetadata.inkerLocked, inkers.ifEmpty { null }),
            lettererLocked = shouldLock(currentMetadata.lettererLocked, letterers.ifEmpty { null }),
            pencillerLocked = shouldLock(currentMetadata.pencillerLocked, pencillers.ifEmpty { null }),
            publisherLocked = shouldLock(currentMetadata.publisherLocked, publishers.ifEmpty { null }),
            translatorLocked = shouldLock(currentMetadata.translatorLocked, translators.ifEmpty { null }),
            coverArtistLocked = shouldLock(currentMetadata.coverArtistLocked, coverArtists.ifEmpty { null }),
            releaseYearLocked = shouldLock(currentMetadata.releaseYearLocked, releaseYear),

            // Passthrough lock status for data not provided by metadata provider
            characterLocked = currentMetadata.characterLocked,
            imprintLocked = currentMetadata.imprintLocked,
            teamLocked = currentMetadata.teamLocked,
            locationLocked = currentMetadata.locationLocked,
        )

        return KavitaSeriesMetadataUpdateRequest(metadata)
    }

    private fun MediaServerBookMetadataUpdate.toKavitaChapterMetadataUpdate(currentChapter: KavitaChapter): KavitaChapterMetadataUpdateRequest {
        val tags = tags?.let { deduplicate(it) }?.map { KavitaTag(id = 0, title = it) }?.ifEmpty { null }
        val webLinks = links?.joinToString(",") { it.url }?.ifBlank { null }

        val authors = authors?.groupBy { it.role.lowercase() }
        val writers = getRoles(authors, AuthorRole.WRITER)
        val coverArtists = getRoles(authors, AuthorRole.COVER)
        val pencillers = getRoles(authors, AuthorRole.PENCILLER)
        val inkers = getRoles(authors, AuthorRole.INKER)
        val colorists = getRoles(authors, AuthorRole.COLORIST)
        val letterers = getRoles(authors, AuthorRole.LETTERER)
        val editors = getRoles(authors, AuthorRole.EDITOR)
        val translators = getRoles(authors, AuthorRole.TRANSLATOR)

        val genres = genres?.let { deduplicate(it) }?.map { KavitaGenre(id = 0, title = it) }?.toSet() ?: emptySet()
        val ageRating = ageRating
            ?.let { metadataRating ->
                KavitaAgeRating.entries
                    .filter { it.ageRating != null }
                    .sortedBy { it.ageRating }
                    .firstOrNull { it.ageRating == it.ageRating!!.coerceAtLeast(metadataRating) }
                    ?: KavitaAgeRating.ADULTS_ONLY
            }

        return KavitaChapterMetadataUpdateRequest(
            id = currentChapter.id,
            summary = summary ?: currentChapter.summary,
            genres = genres.ifEmpty { currentChapter.genres },
            tags = tags ?: currentChapter.tags,
            writers = writers.ifEmpty { currentChapter.writers },
            coverArtists = coverArtists.ifEmpty { currentChapter.coverArtists },
            pencillers = pencillers.ifEmpty { currentChapter.pencillers },
            inkers = inkers.ifEmpty { currentChapter.inkers },
            colorists = colorists.ifEmpty { currentChapter.colorists },
            letterers = letterers.ifEmpty { currentChapter.letterers },
            editors = editors.ifEmpty { currentChapter.editors },
            translators = translators.ifEmpty { currentChapter.translators },
            ageRating = ageRating ?: currentChapter.ageRating,
            language = language ?: currentChapter.language,
            sortOrder = numberSort ?: currentChapter.sortOrder,
            weblinks = webLinks ?: currentChapter.webLinks,
            isbn = isbn ?: currentChapter.isbn,
            releaseDate = releaseDate?.atTime(LocalTime(0, 0, 0)) ?: currentChapter.releaseDate,
            titleName = title ?: currentChapter.titleName,

            // Passthrough data not provided by metadata provider
            publishers = currentChapter.publishers,
            imprints = currentChapter.imprints,
            characters = currentChapter.characters,
            teams = currentChapter.teams,
            locations = currentChapter.locations,

            // Set locks
            ageRatingLocked = shouldLock(currentChapter.ageRatingLocked, ageRating),
            titleNameLocked = shouldLock(currentChapter.titleNameLocked, title),
            genresLocked = shouldLock(currentChapter.genresLocked, genres.ifEmpty { null }),
            tagsLocked = shouldLock(currentChapter.tagsLocked, tags),
            writerLocked = shouldLock(currentChapter.writerLocked, writers.ifEmpty { null }),
            coloristLocked = shouldLock(currentChapter.coloristLocked, colorists.ifEmpty { null }),
            editorLocked = shouldLock(currentChapter.editorLocked, editors.ifEmpty { null }),
            inkerLocked = shouldLock(currentChapter.inkerLocked, inkers.ifEmpty { null }),
            lettererLocked = shouldLock(currentChapter.lettererLocked, letterers.ifEmpty { null }),
            pencillerLocked = shouldLock(currentChapter.pencillerLocked, pencillers.ifEmpty { null }),
            translatorLocked = shouldLock(currentChapter.translatorLocked, translators.ifEmpty { null }),
            coverArtistLocked = shouldLock(currentChapter.coverArtistLocked, coverArtists.ifEmpty { null }),
            languageLocked = shouldLock(currentChapter.languageLocked, language),
            summaryLocked = shouldLock(currentChapter.summaryLocked, summary),
            isbnLocked = shouldLock(false, isbn),
            releaseDateLocked = shouldLock(currentChapter.releaseDateLocked, releaseDate),
            sortOrderLocked = shouldLock(currentChapter.sortOrderLocked, numberSort),

            // Passthrough lock status for data not provided by metadata provider
            publisherLocked = currentChapter.publisherLocked,
            characterLocked = currentChapter.characterLocked,
            imprintLocked = currentChapter.imprintLocked,
            teamLocked = currentChapter.teamLocked,
            locationLocked = currentChapter.locationLocked,
        )
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
    val fileName = filePath.fileName.nameWithoutExtension

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

        titleLock = titleNameLocked,
        summaryLock = summaryLocked,
        numberLock = false,
        numberSortLock = sortOrderLocked,
        releaseDateLock = releaseDateLocked,
        authorsLock = authorsLock,
        tagsLock = tagsLocked,
        isbnLock = false,
        linksLock = false,
        ageRatingLock = ageRatingLocked,
        languageLock = languageLocked,
        genresLock = genresLocked,
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
        titleLock = false,
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
        imprints = emptySet(),
        colorists = emptySet(),
        letterers = emptySet(),
        editors = emptySet(),
        translators = emptySet(),
        teams = emptySet(),
        locations = emptySet(),
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
        imprintLocked = false,
        lettererLocked = false,
        pencillerLocked = false,
        publisherLocked = false,
        translatorLocked = false,
        teamLocked = false,
        locationLocked = false,
        coverArtistLocked = false,
        releaseYearLocked = false,
    )
    return KavitaSeriesMetadataUpdateRequest(metadata)
}

private fun KavitaSeries.toKavitaCoverResetRequest() = KavitaSeriesUpdateRequest(
    id = id,
    localizedName = localizedName,
    sortName = sortName,
    sortNameLocked = false,
    localizedNameLocked = false,

    coverImageLocked = false
)

private fun kavitaChapterResetRequest(chapterId: KavitaChapterId): KavitaChapterMetadataUpdateRequest {
    return KavitaChapterMetadataUpdateRequest(
        id = chapterId,
        summary = null,
        genres = emptySet(),
        tags = emptySet(),
        ageRating = UNKNOWN,
        language = null,
        weblinks = "",
        isbn = null,
        releaseDate = kotlinx.datetime.LocalDateTime(1970, 1, 1, 0, 0, 0),
        titleName = null,
        sortOrder = 0.0,
        writers = emptySet(),
        coverArtists = emptySet(),
        publishers = emptySet(),
        characters = emptySet(),
        pencillers = emptySet(),
        inkers = emptySet(),
        imprints = emptySet(),
        colorists = emptySet(),
        letterers = emptySet(),
        editors = emptySet(),
        translators = emptySet(),
        teams = emptySet(),
        locations = emptySet(),
        ageRatingLocked = false,
        titleNameLocked = false,
        genresLocked = false,
        tagsLocked = false,
        writerLocked = false,
        characterLocked = false,
        coloristLocked = false,
        editorLocked = false,
        inkerLocked = false,
        imprintLocked = false,
        lettererLocked = false,
        pencillerLocked = false,
        publisherLocked = false,
        translatorLocked = false,
        teamLocked = false,
        locationLocked = false,
        coverArtistLocked = false,
        languageLocked = false,
        summaryLocked = false,
        isbnLocked = false,
        releaseDateLocked = false,
        sortOrderLocked = false,
    )
}
