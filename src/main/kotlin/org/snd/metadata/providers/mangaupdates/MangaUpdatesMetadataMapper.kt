package org.snd.metadata.providers.mangaupdates

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Author
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.Image
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.ReleaseDate
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesStatus
import org.snd.metadata.model.SeriesTitle
import org.snd.metadata.model.TitleType.ROMAJI
import org.snd.metadata.model.WebLink
import org.snd.metadata.providers.mangaupdates.model.Series
import org.snd.metadata.providers.mangaupdates.model.Status

class MangaUpdatesMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig
) {

    fun toSeriesMetadata(series: Series, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            Status.COMPLETE -> SeriesStatus.ENDED
            Status.ONGOING -> SeriesStatus.ONGOING
            Status.CANCELLED -> SeriesStatus.ABANDONED
            Status.HIATUS -> SeriesStatus.HIATUS
            else -> SeriesStatus.ONGOING
        }

        val artistRoles = listOf(
            AuthorRole.PENCILLER,
            AuthorRole.INKER,
            AuthorRole.COLORIST,
            AuthorRole.LETTERER,
            AuthorRole.COVER
        )

        val authors = series.authors.flatMap {
            when (it.type) {
                "Author" -> listOf(Author(it.name, AuthorRole.WRITER))
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

        val titles = listOf(SeriesTitle(series.title, ROMAJI)) + series.associatedNames.map { SeriesTitle(it, null) }

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
