package snd.komf.providers.webtoons

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.webtoons.model.Episode
import snd.komf.providers.webtoons.model.Status
import snd.komf.providers.webtoons.model.Title
import snd.komf.providers.webtoons.model.WebtoonsSeries

class WebtoonsMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    fun toSeriesSearchResult(results: Pair<Collection<Title>, Collection<Title>>): List<SeriesSearchResult> {
        // This can probably be prettier
        return results.first.map { title ->
            SeriesSearchResult(
                url = title.getOriginalUrl(),
                imageUrl = "${WebtoonsClient.IMAGE_BASE_URL}${title.thumbnailMobile}",
                title = title.title,
                provider = CoreProviders.WEBTOONS,
                resultId = title.getOriginalId().value
            )
        } + results.second.map { title ->
            SeriesSearchResult(
                url = title.getCanvasUrl(),
                imageUrl = "${WebtoonsClient.IMAGE_BASE_URL}${title.thumbnailMobile}",
                title = title.title,
                provider = CoreProviders.WEBTOONS,
                resultId = title.getCanvasId().value
            )
        }
    }

    fun toSeriesMetadata(series: WebtoonsSeries, thumbnail: Image? = null): ProviderSeriesMetadata {
        val authors = listOfNotNull(
            series.author?.let { Author(it.name, AuthorRole.WRITER) },
            series.adaptedBy?.let { Author(it.name, AuthorRole.EDITOR) },
            series.artist?.let { Author(it.name, AuthorRole.COLORIST) },
        )

        val titles = listOfNotNull(SeriesTitle(series.title, TitleType.LOCALIZED, "en"))

        val metadata = SeriesMetadata(
            status = when (series.status) {
                Status.ONGOING -> SeriesStatus.ONGOING
                Status.COMPLETED -> SeriesStatus.ENDED
                Status.UNKNOWN -> null
            },
            titles = titles,
            summary = series.description,
            // https://www.webtoons.com/genreNames
            genres = series.genres,
            authors = authors,
            thumbnail = thumbnail,
            // totalBookCount = series.numberOfVolumes,
            // There's a data-title-unsuitable-for-children=bool in certain places
            // ageRating = series.recommendedAge,
            // First chapter release date? Not effective for translated works
            // releaseDate = ReleaseDate(series.startYear, null, null),
            links = listOf(WebLink("Webtoon", series.url)),
            // Removed in the UI update to the site
            // score = series.score,
            // Maybe worth overriding by default
            // readingDirection = TODO(),
            language = "en"
        )

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.value),
            metadata = metadata,
            // chapter.episodeNo has the chapter number as seen by Webtoon's API, which while more "correct" confuses
            // komf because it doesn't have that concept
            books = series.chapters?.mapIndexed { index, chapter ->
                SeriesBook(
                    id = ProviderBookId(chapter.getId().value),
                    number = BookRange(index + 1),
                    edition = null,
                    // TODO: Proper type
                    type = null,
                    name = chapter.episodeTitle
                )
            } ?: emptyList()
        )
        return MetadataConfigApplier.apply(providerMetadata, metadataConfig)
    }

    fun toBookMetadata(index: Int, chapter: Episode, seriesId: ProviderSeriesId, thumbnail: Image?): ProviderBookMetadata {
        return ProviderBookMetadata(
            id = ProviderBookId(chapter.getId().value),
            seriesId = seriesId,
            metadata = BookMetadata(
                title = chapter.episodeTitle,
                //summary = TODO(),
                // Same as above with regards to chapter.episodeNo
                number = BookRange(index + 1),
                // numberSort = TODO(),
                releaseDate = Instant.fromEpochMilliseconds(chapter.exposureDateMillis)
                    .toLocalDateTime(TimeZone.UTC).date,
                // Mirror series data?
                //authors = TODO(),
                links = listOf(WebLink("Webtoon", chapter.getUrl())),
                // chapters = TODO(),
                //storyArcs = TODO(),
                //startChapter = TODO(),
                //endChapter = TODO(),
                thumbnail = thumbnail
            )
        )
    }
}