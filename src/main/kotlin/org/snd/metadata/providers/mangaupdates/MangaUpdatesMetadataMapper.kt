package org.snd.metadata.providers.mangaupdates

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.ROMAJI
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.mangaupdates.model.Series
import org.snd.metadata.providers.mangaupdates.model.Status

class MangaUpdatesMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(series: Series, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
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

        val originalPublishers = series.publishers.filter { it.type == "Original" }.map { it.name }.toSet()
        val englishPublishers = series.publishers.filter { it.type == "English" }.map { it.name }.toSet()

        val originalPublisherTag = metadataConfig.originalPublisherTagName
            ?.let { tag -> originalPublishers.firstOrNull()?.let { "$tag: $it" } }
        val englishPublisherTag = metadataConfig.englishPublisherTagName
            ?.let { tag -> englishPublishers.firstOrNull()?.let { "$tag: $it" } }
        val tags = series.categories.sortedByDescending { it.votes }.take(15)
            .map { it.name } + listOfNotNull(originalPublisherTag, englishPublisherTag)

        val titles = listOf(SeriesTitle(series.title, ROMAJI, "ja-ro")) + series.associatedNames.map { SeriesTitle(it, null, null) }

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.description,
            publisher = if (metadataConfig.useOriginalPublisher) originalPublishers.firstOrNull() else englishPublishers.firstOrNull()
                ?: originalPublishers.firstOrNull(),
            alternativePublishers = if (metadataConfig.useOriginalPublisher) englishPublishers
            else englishPublishers,
            genres = series.genres,
            tags = tags,
            authors = authors,
            thumbnail = thumbnail,
            releaseDate = ReleaseDate(series.year?.value, null, null),
            links = listOf(WebLink("MangaUpdates", series.url))
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }
}
