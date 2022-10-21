package org.snd.metadata.providers.mangaupdates

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.AuthorRole
import org.snd.metadata.model.Image
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.providers.mangaupdates.model.Series
import org.snd.metadata.providers.mangaupdates.model.Status

class MangaUpdatesMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig
) {

    fun toSeriesMetadata(series: Series, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            Status.COMPLETE -> SeriesMetadata.Status.ENDED
            Status.ONGOING -> SeriesMetadata.Status.ONGOING
            Status.CANCELLED -> SeriesMetadata.Status.ABANDONED
            Status.HIATUS -> SeriesMetadata.Status.HIATUS
            else -> SeriesMetadata.Status.ONGOING
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
                "Author" -> listOf(org.snd.metadata.model.Author(it.name, AuthorRole.WRITER))
                else -> artistRoles.map { role -> org.snd.metadata.model.Author(it.name, role) }
            }
        }

        val originalPublisher = series.publishers.firstOrNull { it.type == "Original" }?.name
        val englishPublisher = series.publishers.firstOrNull { it.type == "English" }?.name

        val originalPublisherTag = metadataConfig.originalPublisherTagName
            ?.let { tag -> originalPublisher?.let { "$tag: $originalPublisher" } }
        val englishPublisherTag = metadataConfig.englishPublisherTagName
            ?.let { tag -> englishPublisher?.let { "$tag: $englishPublisher" } }
        val tags = series.categories.sortedByDescending { it.votes }.take(15)
            .map { it.name } + listOfNotNull(originalPublisherTag, englishPublisherTag)

        val metadata = SeriesMetadata(
            status = status,
            title = series.title,
            titleSort = series.title,
            summary = series.description,
            publisher = if (metadataConfig.useOriginalPublisher) originalPublisher else englishPublisher ?: originalPublisher,
            genres = series.genres,
            tags = tags,
            authors = authors,
            alternativeTitles = series.associatedNames,
            thumbnail = thumbnail,
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }
}
