package org.snd.metadata.providers.nautiljon

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Author
import org.snd.metadata.model.BookMetadata
import org.snd.metadata.model.BookRange
import org.snd.metadata.model.Image
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.ReleaseDate
import org.snd.metadata.model.SeriesBook
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesStatus
import org.snd.metadata.model.SeriesTitle
import org.snd.metadata.model.TitleType
import org.snd.metadata.model.TitleType.NATIVE
import org.snd.metadata.model.TitleType.ROMAJI
import org.snd.metadata.model.WebLink
import org.snd.metadata.providers.nautiljon.model.Series
import org.snd.metadata.providers.nautiljon.model.Volume
import java.net.URLEncoder

class NautiljonSeriesMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<String>,
    private val artistRoles: Collection<String>,
) {

    fun toSeriesMetadata(series: Series, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            "En cours" -> SeriesStatus.ONGOING
            "En attente" -> SeriesStatus.ONGOING
            "Abandonné" -> SeriesStatus.ABANDONED
            "Terminé" -> SeriesStatus.ENDED
            else -> null
        }

        val authors = series.authorsStory.flatMap { author ->
            authorRoles.map { role -> Author(author, role) }
        } + series.authorsArt.flatMap { artist ->
            artistRoles.map { role -> Author(artist, role) }
        }

        val tags = series.themes + listOfNotNull(
            seriesMetadataConfig.originalPublisherTagName
                ?.let { tag -> series.originalPublisher?.let { publisher -> "$tag: $publisher" } },
            seriesMetadataConfig.frenchPublisherTagName
                ?.let { tag -> series.frenchPublisher?.let { publisher -> "$tag: $publisher" } }
        )
        val titles = listOfNotNull(
            SeriesTitle(series.title, TitleType.LOCALIZED),
            series.romajiTitle?.let { SeriesTitle(it, ROMAJI) },
            series.japaneseTitle?.let { SeriesTitle(it, NATIVE) },
        ) + series.alternativeTitles.map { SeriesTitle(it, null) }


        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.description,
            publisher = if (seriesMetadataConfig.useOriginalPublisher) series.originalPublisher
            else series.frenchPublisher ?: series.originalPublisher,
            alternativePublishers = if (seriesMetadataConfig.useOriginalPublisher) setOfNotNull(series.frenchPublisher)
            else series.frenchPublisher?.let { setOfNotNull(series.originalPublisher) } ?: emptySet(),
            genres = series.genres,
            tags = tags,
            authors = authors,
            thumbnail = thumbnail,
            totalBookCount = series.numberOfVolumes,
            ageRating = series.recommendedAge,
            releaseDate = ReleaseDate(series.startYear?.value, null, null),
            links = listOf(
                WebLink(
                    "Nautiljon",
                    nautiljonBaseUrl + "mangas/${URLEncoder.encode(series.id.id, "UTF-8")}.html"
                )
            )
        )
        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.id),
            metadata = metadata,
            books = series.volumes.map {
                SeriesBook(
                    id = ProviderBookId(it.id.id),
                    number = it.number?.let { number -> BookRange(number.toDouble(), number.toDouble()) },
                    edition = it.edition,
                    type = it.type,
                    name = it.name
                )
            }
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(volume: Volume, thumbnail: Image? = null): ProviderBookMetadata {
        val authors = volume.authorsStory.flatMap { author ->
            authorRoles.map { role -> Author(author, role) }
        } + volume.authorsArt.flatMap { artist ->
            artistRoles.map { role -> Author(artist, role) }
        }

        val metadata = BookMetadata(
            summary = volume.description,
            number = volume.number.let { number -> BookRange(number.toDouble(), number.toDouble()) },
            releaseDate = if (seriesMetadataConfig.useOriginalPublisher) volume.originalReleaseDate else volume.frenchReleaseDate,
            authors = authors,
            links = listOf(
                WebLink(
                    "Nautiljon",
                    nautiljonBaseUrl +
                            "mangas/${URLEncoder.encode(volume.seriesId.id, "UTF-8")}/" +
                            "volume-${URLEncoder.encode(volume.id.id, "UTF-8")}.html"
                )
            ),

            startChapter = null,
            endChapter = null,

            thumbnail = thumbnail
        )
        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(volume.id.id),
            metadata = metadata
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }
}
