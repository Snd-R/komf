package snd.komf.mediaserver.komga

import io.github.oshai.kotlinlogging.KotlinLogging
import snd.komf.mediaserver.MediaServerClient
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
import snd.komf.model.Image
import snd.komf.model.ReadingDirection
import snd.komf.model.SeriesStatus
import snd.komf.model.TitleType
import snd.komf.model.WebLink
import snd.komga.client.book.KomgaBook
import snd.komga.client.book.KomgaBookClient
import snd.komga.client.book.KomgaBookId
import snd.komga.client.book.KomgaBookMetadata
import snd.komga.client.book.KomgaBookMetadataUpdateRequest
import snd.komga.client.book.KomgaBookThumbnail
import snd.komga.client.common.KomgaAuthor
import snd.komga.client.common.KomgaPageRequest
import snd.komga.client.common.KomgaReadingDirection
import snd.komga.client.common.KomgaThumbnailId
import snd.komga.client.common.KomgaWebLink
import snd.komga.client.common.PatchValue
import snd.komga.client.library.KomgaLibrary
import snd.komga.client.library.KomgaLibraryClient
import snd.komga.client.library.KomgaLibraryId
import snd.komga.client.search.allOfBooks
import snd.komga.client.search.allOfSeries
import snd.komga.client.series.KomgaAlternativeTitle
import snd.komga.client.series.KomgaSeries
import snd.komga.client.series.KomgaSeriesClient
import snd.komga.client.series.KomgaSeriesId
import snd.komga.client.series.KomgaSeriesMetadata
import snd.komga.client.series.KomgaSeriesMetadataUpdateRequest
import snd.komga.client.series.KomgaSeriesStatus
import snd.komga.client.series.KomgaSeriesThumbnail

private val logger = KotlinLogging.logger {}

class KomgaMediaServerClientAdapter(
    private val komgaBookClient: KomgaBookClient,
    private val komgaSeriesClient: KomgaSeriesClient,
    private val komgaLibraryClient: KomgaLibraryClient,
    private val komgaCoverUploadLimit: Long
) : MediaServerClient {

    override suspend fun getSeries(seriesId: MediaServerSeriesId): MediaServerSeries {
        return komgaSeriesClient.getOneSeries(KomgaSeriesId(seriesId.value)).toMediaServerSeries()
    }

    override suspend fun getSeries(libraryId: MediaServerLibraryId, pageNumber: Int): Page<MediaServerSeries> {
        val komgaPage = komgaSeriesClient.getSeriesList(
            conditionBuilder = allOfSeries {
                library {
                    isEqualTo(KomgaLibraryId(libraryId.value))
                }
            },
            fulltextSearch = null,
            pageRequest = KomgaPageRequest(pageIndex = pageNumber - 1, size = 500)
        )
        return Page(
            content = komgaPage.content.map { it.toMediaServerSeries() },
            pageNumber = komgaPage.number,
            totalElements = komgaPage.totalElements,
            totalPages = komgaPage.totalPages
        )
    }

    override suspend fun getSeriesThumbnail(seriesId: MediaServerSeriesId): Image? {
        return runCatching {
            Image(komgaSeriesClient.getSeriesDefaultThumbnail(KomgaSeriesId(seriesId.value)))
        }.getOrNull()
    }

    override suspend fun getSeriesThumbnails(seriesId: MediaServerSeriesId): Collection<MediaServerSeriesThumbnail> {
        return komgaSeriesClient.getSeriesThumbnails(KomgaSeriesId(seriesId.value))
            .map { it.toMediaServerSeriesThumbnail() }

    }

    override suspend fun getBook(bookId: MediaServerBookId): MediaServerBook {
        return komgaBookClient.getBook(KomgaBookId(bookId.value)).toMediaServerBook()
    }

    override suspend fun getBooks(seriesId: MediaServerSeriesId): Collection<MediaServerBook> {
        return komgaBookClient.getBookList(
            conditionBuilder = allOfBooks {
                seriesId { isEqualTo(KomgaSeriesId(seriesId.value)) }
            },
            pageRequest = KomgaPageRequest(unpaged = true)
        ).content.map { it.toMediaServerBook() }
    }

    override suspend fun getBookThumbnails(bookId: MediaServerBookId): Collection<MediaServerBookThumbnail> {
        return komgaBookClient.getBookThumbnails(KomgaBookId(bookId.value))
            .map { it.toMediaServerBookThumbnail() }
    }

    override suspend fun getBookThumbnail(bookId: MediaServerBookId): Image? {
        return runCatching {
            Image(komgaBookClient.getBookThumbnail(KomgaBookId(bookId.value)))
        }.getOrNull()
    }

    override suspend fun getLibrary(libraryId: MediaServerLibraryId): MediaServerLibrary {
        return komgaLibraryClient.getLibrary(KomgaLibraryId(libraryId.value)).toMediaServerLibrary()
    }

    override suspend fun getLibraries(): List<MediaServerLibrary> {
        return komgaLibraryClient.getLibraries().map { it.toMediaServerLibrary() }
    }

    override suspend fun updateSeriesMetadata(
        seriesId: MediaServerSeriesId,
        metadata: MediaServerSeriesMetadataUpdate
    ) {
        komgaSeriesClient.updateSeries(
            seriesId = KomgaSeriesId(seriesId.value),
            request = metadata.toMetadataUpdateRequest()
        )
    }

    override suspend fun deleteSeriesThumbnail(seriesId: MediaServerSeriesId, thumbnailId: MediaServerThumbnailId) {
        komgaSeriesClient.deleteSeriesThumbnail(
            KomgaSeriesId(seriesId.value),
            KomgaThumbnailId(thumbnailId.value)
        )
    }

    override suspend fun updateBookMetadata(bookId: MediaServerBookId, metadata: MediaServerBookMetadataUpdate) {
        komgaBookClient.updateMetadata(
            KomgaBookId(bookId.value),
            metadata.toKomgaMetadataUpdate()
        )
    }

    override suspend fun deleteBookThumbnail(bookId: MediaServerBookId, thumbnailId: MediaServerThumbnailId) {
        komgaBookClient.deleteBookThumbnail(
            KomgaBookId(bookId.value),
            KomgaThumbnailId(thumbnailId.value)
        )
    }

    override suspend fun resetBookMetadata(bookId: MediaServerBookId, bookName: String, bookNumber: Int?) {
        komgaBookClient.updateMetadata(
            KomgaBookId(bookId.value),
            bookMetadataResetRequest(bookName, bookNumber)
        )
    }

    override suspend fun resetSeriesMetadata(seriesId: MediaServerSeriesId, seriesName: String) {
        komgaSeriesClient.updateSeries(
            KomgaSeriesId(seriesId.value),
            seriesMetadataResetRequest(seriesName)
        )
    }

    override suspend fun uploadSeriesThumbnail(
        seriesId: MediaServerSeriesId,
        thumbnail: Image,
        selected: Boolean,
        lock: Boolean
    ): MediaServerSeriesThumbnail? {
        if (thumbnail.bytes.size > komgaCoverUploadLimit) {
            logger.warn { "Thumbnail size is bigger than $komgaCoverUploadLimit bytes. Skipping thumbnail upload" }
            return null
        }

        val uploadedThumbnail = komgaSeriesClient.uploadSeriesThumbnail(
            seriesId = KomgaSeriesId(seriesId.value),
            file = thumbnail.bytes,
            selected = selected
        )
        return MediaServerSeriesThumbnail(
            id = MediaServerThumbnailId(uploadedThumbnail.id.value),
            seriesId = MediaServerSeriesId(uploadedThumbnail.seriesId.value),
            type = uploadedThumbnail.type,
            selected = uploadedThumbnail.selected
        )
    }

    override suspend fun uploadBookThumbnail(
        bookId: MediaServerBookId,
        thumbnail: Image, selected: Boolean, lock: Boolean
    ): MediaServerBookThumbnail? {
        if (thumbnail.bytes.size > komgaCoverUploadLimit) {
            logger.warn { "Thumbnail size is bigger than $komgaCoverUploadLimit bytes. Skipping thumbnail upload" }
            return null
        }

        val uploadedThumbnail = komgaBookClient.uploadBookThumbnail(
            bookId = KomgaBookId(bookId.value),
            file = thumbnail.bytes,
            selected = selected
        )

        return MediaServerBookThumbnail(
            id = MediaServerThumbnailId(uploadedThumbnail.id.value),
            bookId = MediaServerBookId(uploadedThumbnail.bookId.value),
            type = uploadedThumbnail.type,
            selected = uploadedThumbnail.selected
        )
    }

    override suspend fun refreshMetadata(libraryId: MediaServerLibraryId, seriesId: MediaServerSeriesId) {
        komgaSeriesClient.analyze(KomgaSeriesId(seriesId.value))
    }

    private fun KomgaSeries.toMediaServerSeries(): MediaServerSeries {
        return MediaServerSeries(
            id = MediaServerSeriesId(id.value),
            libraryId = MediaServerLibraryId(libraryId.value),
            name = name,
            booksCount = booksCount,
            metadata = metadata.toMediaServerSeriesMetadata(),
            url = url,
            deleted = deleted,
        )
    }

    private fun KomgaSeriesMetadata.toMediaServerSeriesMetadata() = MediaServerSeriesMetadata(
        status = when (status) {
            KomgaSeriesStatus.ENDED -> SeriesStatus.ENDED
            KomgaSeriesStatus.ONGOING -> SeriesStatus.ONGOING
            KomgaSeriesStatus.ABANDONED -> SeriesStatus.ABANDONED
            KomgaSeriesStatus.HIATUS -> SeriesStatus.HIATUS
        },
        title = title,
        titleSort = titleSort,
        alternativeTitles = alternateTitles.map { (label, title) -> MediaServerAlternativeTitle(label, title) },
        summary = summary,
        readingDirection = when (readingDirection) {
            KomgaReadingDirection.LEFT_TO_RIGHT -> ReadingDirection.LEFT_TO_RIGHT
            KomgaReadingDirection.RIGHT_TO_LEFT -> ReadingDirection.RIGHT_TO_LEFT
            KomgaReadingDirection.VERTICAL -> ReadingDirection.VERTICAL
            KomgaReadingDirection.WEBTOON -> ReadingDirection.WEBTOON
            null -> null
        },
        publisher = publisher,
        alternativePublishers = emptySet(),
        ageRating = ageRating,
        language = language,
        genres = genres,
        tags = tags,
        totalBookCount = totalBookCount,
        authors = emptyList(), //TODO take authors from book metadata?,
        releaseYear = null, //TODO take from book metadata?,
        links = links.map { WebLink(it.label, it.url) },

        statusLock = statusLock,
        titleLock = titleLock,
        titleSortLock = titleSortLock,
        alternativeTitlesLock = alternateTitlesLock,
        summaryLock = summaryLock,
        readingDirectionLock = readingDirectionLock,
        publisherLock = publisherLock,
        ageRatingLock = ageRatingLock,
        languageLock = languageLock,
        genresLock = genresLock,
        tagsLock = tagsLock,
        totalBookCountLock = totalBookCountLock,
        authorsLock = false,
        releaseYearLock = false,
        linksLock = linksLock
    )

    private fun KomgaSeriesThumbnail.toMediaServerSeriesThumbnail() = MediaServerSeriesThumbnail(
        id = MediaServerThumbnailId(id.value),
        seriesId = MediaServerSeriesId(seriesId.value),
        type = type,
        selected = selected
    )

    private fun KomgaBook.toMediaServerBook() = MediaServerBook(
        id = MediaServerBookId(id.value),
        seriesId = MediaServerSeriesId(seriesId.value),
        libraryId = MediaServerLibraryId(libraryId.value),
        seriesTitle = seriesTitle,
        name = name,
        url = url,
        number = number,
        oneshot = oneshot,
        metadata = metadata.toMediaServerBookMetadata(),
        deleted = deleted,
    )

    private fun KomgaBookMetadata.toMediaServerBookMetadata() = MediaServerBookMetadata(
        title = title,
        summary = summary,
        number = number,
        numberSort = numberSort.toString(),
        releaseDate = releaseDate,
        authors = authors.map { MediaServerAuthor(it.name, it.role) },
        tags = tags,
        isbn = isbn,
        links = links.map { WebLink(it.label, it.url) },

        titleLock = titleLock,
        summaryLock = summaryLock,
        numberLock = numberLock,
        numberSortLock = numberSortLock,
        releaseDateLock = releaseDateLock,
        authorsLock = authorsLock,
        tagsLock = tagsLock,
        isbnLock = isbnLock,
        linksLock = linksLock
    )

    private fun KomgaBookThumbnail.toMediaServerBookThumbnail() = MediaServerBookThumbnail(
        id = MediaServerThumbnailId(id.value),
        bookId = MediaServerBookId(bookId.value),
        type = type,
        selected = selected,
    )

    private fun KomgaLibrary.toMediaServerLibrary() = MediaServerLibrary(
        id = MediaServerLibraryId(id.value),
        name = name,
        roots = listOf(root),
    )

    private fun seriesMetadataResetRequest(name: String) = KomgaSeriesMetadataUpdateRequest(
        status = PatchValue.Some(KomgaSeriesStatus.ONGOING),
        title = PatchValue.Some(name),
        titleSort = PatchValue.Some(name),
        alternateTitles = PatchValue.None,
        summary = PatchValue.Some(""),
        publisher = PatchValue.Some(""),
        readingDirection = PatchValue.None,
        ageRating = PatchValue.None,
        language = PatchValue.Some(""),
        genres = PatchValue.Some(emptyList()),
        tags = PatchValue.Some(emptyList()),
        totalBookCount = PatchValue.None,
        links = PatchValue.None,
        statusLock = PatchValue.Some(false),
        titleLock = PatchValue.Some(false),
        titleSortLock = PatchValue.Some(false),
        summaryLock = PatchValue.Some(false),
        publisherLock = PatchValue.Some(false),
        readingDirectionLock = PatchValue.Some(false),
        ageRatingLock = PatchValue.Some(false),
        languageLock = PatchValue.Some(false),
        genresLock = PatchValue.Some(false),
        tagsLock = PatchValue.Some(false),
        totalBookCountLock = PatchValue.Some(false),
        linksLock = PatchValue.Some(false),
    )

    private fun MediaServerSeriesMetadataUpdate.toMetadataUpdateRequest() = KomgaSeriesMetadataUpdateRequest(
        status = patchIfNotNull(
            when (status) {
                SeriesStatus.COMPLETED, SeriesStatus.ENDED -> KomgaSeriesStatus.ENDED
                SeriesStatus.ONGOING -> KomgaSeriesStatus.ONGOING
                SeriesStatus.ABANDONED -> KomgaSeriesStatus.ABANDONED
                SeriesStatus.HIATUS -> KomgaSeriesStatus.HIATUS
                null -> null
            }
        ),
        title = patchIfNotNull(title?.name),
        titleSort = patchIfNotNull(titleSort?.name),
        alternateTitles = patchIfNotNull(
            alternativeTitles?.mapNotNull { (name, type, language) ->
                when (type) {
                    TitleType.ROMAJI -> KomgaAlternativeTitle(type.label, name)
                    TitleType.NATIVE -> KomgaAlternativeTitle(type.label, name)
                    TitleType.LOCALIZED -> KomgaAlternativeTitle((language ?: type.label), name)
                    null -> language?.let { KomgaAlternativeTitle(it, name) }
                }
            }?.distinctBy { it.title }
        ),
        summary = patchIfNotNull(summary),
        publisher = patchIfNotNull(publisher),
        readingDirection = patchIfNotNull(
            when (readingDirection) {
                ReadingDirection.LEFT_TO_RIGHT -> KomgaReadingDirection.LEFT_TO_RIGHT
                ReadingDirection.RIGHT_TO_LEFT -> KomgaReadingDirection.RIGHT_TO_LEFT
                ReadingDirection.VERTICAL -> KomgaReadingDirection.VERTICAL
                ReadingDirection.WEBTOON -> KomgaReadingDirection.WEBTOON
                null -> null
            }
        ),
        ageRating = patchIfNotNull(ageRating),
        language = patchIfNotNull(language),
        genres = patchIfNotNull(genres),
        tags = patchIfNotNull(tags),
        // Komga rejects totalBookCount <= 0, so omit invalid values
        totalBookCount = patchIfNotNull(totalBookCount?.takeIf { it > 0 }),
        links = patchIfNotNull(links?.map { KomgaWebLink(it.label, it.url) }),

        statusLock = patchIfNotNull(statusLock),
        titleLock = patchIfNotNull(titleLock),
        titleSortLock = patchIfNotNull(titleSortLock),
        alternateTitlesLock = patchIfNotNull(alternativeTitlesLock),
        summaryLock = patchIfNotNull(summaryLock),
        publisherLock = patchIfNotNull(publisherLock),
        readingDirectionLock = patchIfNotNull(readingDirectionLock),
        ageRatingLock = patchIfNotNull(ageRatingLock),
        languageLock = patchIfNotNull(languageLock),
        genresLock = patchIfNotNull(genresLock),
        tagsLock = patchIfNotNull(tagsLock),
        totalBookCountLock = patchIfNotNull(totalBookCountLock),
        linksLock = patchIfNotNull(linksLock),
    )

    private fun bookMetadataResetRequest(name: String, bookNumber: Int?) = KomgaBookMetadataUpdateRequest(
        title = PatchValue.Some(name),
        summary = PatchValue.Some(""),
        number = bookNumber?.toString()?.let { PatchValue.Some(it) } ?: PatchValue.None,
        numberSort = bookNumber?.toFloat()?.let { PatchValue.Some(it) } ?: PatchValue.None,
        releaseDate = PatchValue.None,
        authors = PatchValue.Some(emptyList()),
        tags = PatchValue.Some(emptyList()),
        isbn = PatchValue.None,
        links = PatchValue.Some(emptyList()),

        titleLock = PatchValue.Some(false),
        summaryLock = PatchValue.Some(false),
        numberLock = PatchValue.Some(false),
        numberSortLock = PatchValue.Some(false),
        releaseDateLock = PatchValue.Some(false),
        authorsLock = PatchValue.Some(false),
        tagsLock = PatchValue.Some(false),
        isbnLock = PatchValue.Some(false),
        linksLock = PatchValue.Some(false)
    )

    private fun MediaServerBookMetadataUpdate.toKomgaMetadataUpdate() = KomgaBookMetadataUpdateRequest(
        title = patchIfNotNull(title),
        summary = patchIfNotNull(summary),
        number = patchIfNotNull(number),
        numberSort = patchIfNotNull(numberSort?.toFloat()),
        releaseDate = patchIfNotNull(releaseDate),
        authors = patchIfNotNull(authors?.map { KomgaAuthor(it.name, it.role) }),
        tags = patchIfNotNull(tags),
        isbn = patchIfNotNull(isbn),
        links = patchIfNotNull(links?.map { KomgaWebLink(it.label, it.url) }),

        titleLock = patchIfNotNull(titleLock),
        summaryLock = patchIfNotNull(summaryLock),
        numberLock = patchIfNotNull(numberLock),
        numberSortLock = patchIfNotNull(numberSortLock),
        releaseDateLock = patchIfNotNull(releaseDateLock),
        authorsLock = patchIfNotNull(authorsLock),
        tagsLock = patchIfNotNull(tagsLock),
        isbnLock = patchIfNotNull(isbnLock),
        linksLock = patchIfNotNull(linksLock),
    )

    private fun <T> patchIfNotNull(value: T?) = value?.let { PatchValue.Some(it) } ?: PatchValue.Unset
}