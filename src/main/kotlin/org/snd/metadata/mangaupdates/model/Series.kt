package org.snd.metadata.mangaupdates.model

import org.snd.metadata.Provider.MANGA_UPDATES
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.mangaupdates.model.Status.*
import org.snd.metadata.mangaupdates.model.Status.UNKNOWN
import org.snd.metadata.mangaupdates.model.Type.*
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.Thumbnail
import java.net.URI
import java.time.Year

data class Series(
    val id: Int,
    val title: String,
    val description: String?,
    val type: Type?,
    val relatedSeries: Collection<RelatedSeries>,
    val associatedNames: Collection<String>,
    val status: Status?,
    val image: URI?,
    val genres: Collection<String>,
    val categories: Collection<Category>,
    val allCategories: Collection<Category>,
    val authors: Collection<Author>,
    val artists: Collection<Author>,
    val year: Year?,
    val originalPublisher: Publisher?,
    val englishPublishers: Collection<Publisher>
)

data class RelatedSeries(
    val id: Int,
    val name: String,
    val relation: String?,
)

data class Category(
    val name: String,
    val score: Int
)

data class Author(
    val id: Int?,
    val name: String
)

data class Publisher(
    val id: Int?,
    val name: String
)

enum class Status {
    COMPLETE,
    ONGOING,
    CANCELLED,
    HIATUS,
    UNKNOWN
}

enum class Type {
    MANGA,
    DOUJINSHI,
    MANHWA,
    MANHUA,
    UNKNOWN
}

fun Series.toSeriesMetadata(thumbnail: Thumbnail? = null): SeriesMetadata {
    val status = when (status) {
        COMPLETE -> SeriesMetadata.Status.ENDED
        ONGOING -> SeriesMetadata.Status.ONGOING
        CANCELLED -> SeriesMetadata.Status.ABANDONED
        HIATUS -> SeriesMetadata.Status.HIATUS
        UNKNOWN -> null
        else -> null
    }

    val readingDirection = when (type) {
        MANGA -> SeriesMetadata.ReadingDirection.RIGHT_TO_LEFT
        DOUJINSHI -> SeriesMetadata.ReadingDirection.RIGHT_TO_LEFT
        MANHWA -> SeriesMetadata.ReadingDirection.LEFT_TO_RIGHT
        MANHUA -> SeriesMetadata.ReadingDirection.RIGHT_TO_LEFT
        Type.UNKNOWN -> null
        else -> null
    }
    val authors = authors.map { org.snd.metadata.model.Author(it.name, "writer") } +
            artists.map { org.snd.metadata.model.Author(it.name, "artist") }

    val tags = allCategories.ifEmpty { categories }.map { it.name }

    return SeriesMetadata(
        status = status,
        title = title,
        titleSort = title,
        summary = description,
        publisher = originalPublisher?.name,
        readingDirection = readingDirection,
        genres = genres,
        tags = tags,
        authors = authors,
        thumbnail = thumbnail,

        id = ProviderSeriesId(id.toString()),
        provider = MANGA_UPDATES
    )
}
