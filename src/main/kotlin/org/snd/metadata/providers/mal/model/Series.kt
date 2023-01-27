package org.snd.metadata.providers.mal.model

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

    val mediaType: MalMediaType,
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
    enum class MalMediaType {
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


