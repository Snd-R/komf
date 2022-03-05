package org.snd.metadata.mal.model

import org.snd.metadata.Provider.MAL
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.SeriesMetadata.ReadingDirection.LEFT_TO_RIGHT
import org.snd.metadata.model.SeriesMetadata.ReadingDirection.RIGHT_TO_LEFT
import org.snd.metadata.model.SeriesMetadata.Status.*
import org.snd.metadata.model.Thumbnail
import java.time.ZonedDateTime

data class Series(
    val id: Int,
    val title: String,
    val alternativeTitles: AlternativeTitles? = null,
    val mainPicture: Picture? = null,
    val synopsis: String? = null,
    val status: Status,
    val genres: Set<String> = emptySet(),
    val authors: List<Author> = emptyList(),
    val serialization: List<Serialization> = emptyList(),
    val pictures: List<Picture> = emptyList(),
    val background: String? = null,
    val nsfw: NSFW? = null,

    val startDate: String? = null,
    val endDate: String? = null,

    val mediaType: MediaType,
    val numVolumes: Int,
    val numChapters: Int,

    val mean: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val numListUsers: Int,
    val numScoringUsers: Int,

    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    enum class MediaType {
        UNKNOWN,
        MANGA,
        NOVEL,
        ONE_SHOT,
        DOUJINSHI,
        MANHWA,
        MANHUA,
        OEL,
        LIGHT_NOVEL

    }

    enum class Status {
        FINISHED,
        CURRENTLY_PUBLISHING,
        NOT_YET_PUBLISHED,
        ON_HIATUS,
        DISCONTINUED
    }

    enum class NSFW {
        WHITE,
        GRAY,
        BLACK
    }
}

data class Picture(
    val large: String?,
    val medium: String
)

data class Author(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val role: String
)

data class Serialization(
    val id: Int,
    val name: String
)

fun Series.toSeriesMetadata(thumbnail: Thumbnail? = null): SeriesMetadata {
    val status = when (status) {
        Series.Status.FINISHED -> ENDED
        Series.Status.CURRENTLY_PUBLISHING -> ONGOING
        Series.Status.NOT_YET_PUBLISHED -> ONGOING
        Series.Status.ON_HIATUS -> HIATUS
        Series.Status.DISCONTINUED -> ABANDONED
    }

    val readingDirection = when (mediaType) {
        Series.MediaType.UNKNOWN -> null
        Series.MediaType.MANGA -> RIGHT_TO_LEFT
        Series.MediaType.NOVEL -> null
        Series.MediaType.ONE_SHOT -> null
        Series.MediaType.DOUJINSHI -> RIGHT_TO_LEFT
        Series.MediaType.MANHWA -> LEFT_TO_RIGHT
        Series.MediaType.MANHUA -> RIGHT_TO_LEFT
        Series.MediaType.OEL -> RIGHT_TO_LEFT
        Series.MediaType.LIGHT_NOVEL -> null
    }
    val authors = authors.map { org.snd.metadata.model.Author("${it.firstName} ${it.lastName}", it.role) }

    return SeriesMetadata(
        status = status,
        title = title,
        titleSort = title,
        summary = synopsis ?: "",
        publisher = serialization.joinToString { it.name },
        readingDirection = readingDirection,
        genres = genres,
        authors = authors,
        thumbnail = thumbnail,
        tags = emptyList(),

        id = ProviderSeriesId(id.toString()),
        provider = MAL
    )
}


