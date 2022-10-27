package org.snd.metadata.providers.mangaupdates

import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.*
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
