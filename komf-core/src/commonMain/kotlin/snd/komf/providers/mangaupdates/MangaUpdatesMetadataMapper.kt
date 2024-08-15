package snd.komf.providers.mangaupdates

import com.fleeksoft.ksoup.parser.Parser.Companion.unescapeEntities
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.Publisher
import snd.komf.model.PublisherType
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType.ROMAJI
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.mangaupdates.model.MangaUpdatesSeries
import snd.komf.providers.mangaupdates.model.SearchResult

class MangaUpdatesMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(series: MangaUpdatesSeries, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (parseStatus(series.status)) {
            Status.COMPLETE -> SeriesStatus.ENDED
            Status.ONGOING -> SeriesStatus.ONGOING
            Status.CANCELLED -> SeriesStatus.ABANDONED
            Status.HIATUS -> SeriesStatus.HIATUS
            else -> null
        }

        val authors = series.authors.flatMap {
            when (it.type) {
                "Author" -> authorRoles.map { role -> Author(it.name, role) }
                else -> artistRoles.map { role -> Author(it.name, role) }
            }
        }

        val originalPublishers = series.publishers.filter { it.type == "Original" }
            .map { Publisher(it.name, PublisherType.ORIGINAL) }.toSet()
        val englishPublishers = series.publishers.filter { it.type == "English" }
            .map { Publisher(it.name, PublisherType.LOCALIZED, "en") }.toSet()

        val tags = series.categories.sortedByDescending { it.votes }.take(15)
            .map { it.category }

        val titles = listOf(SeriesTitle(series.title, ROMAJI, "ja-ro")) +
                series.associated.map { SeriesTitle(it.title, null, null) }

        val publisher = if (metadataConfig.useOriginalPublisher) originalPublishers.firstOrNull()
        else englishPublishers.firstOrNull() ?: originalPublishers.firstOrNull()
        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.description,
            publisher = publisher,
            alternativePublishers = (originalPublishers + englishPublishers) - setOfNotNull(publisher),
            genres = series.genres.map { it.genre },
            tags = tags,
            authors = authors,
            thumbnail = thumbnail,
            releaseDate = ReleaseDate(series.year?.toIntOrNull(), null, null),
            links = listOf(WebLink("MangaUpdates", series.url)),
            score = series.bayesianRating
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }

    fun toSeriesSearchResult(result: SearchResult): SeriesSearchResult {
        return SeriesSearchResult(
            url = result.url,
            imageUrl = result.image?.url?.original,
            title = unescapeEntities(result.title, false),
            provider = CoreProviders.MANGA_UPDATES,
            resultId = result.id.toString()
        )
    }

    private fun parseStatus(status: String?): Status? {
        return status?.let {
            val groups = "\\((.*?)\\)".toRegex().findAll(status)
                .mapNotNull { it.groups[1]?.value }
                .toList()

            if (groups.isNotEmpty() && groups.all { it.contains(groups[0]) })
                runCatching { Status.valueOf(groups[0].uppercase()) }.getOrNull()
            else null

        }
    }

    enum class Status {
        COMPLETE,
        ONGOING,
        CANCELLED,
        HIATUS,
    }
}
