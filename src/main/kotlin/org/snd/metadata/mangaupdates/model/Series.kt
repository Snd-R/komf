package org.snd.metadata.mangaupdates.model

import org.snd.metadata.Provider.MANGA_UPDATES
import org.snd.metadata.ProviderSeriesId
import org.snd.metadata.mangaupdates.model.Status.*
import org.snd.metadata.model.AuthorRole.*
import org.snd.metadata.model.SeriesMetadata
import org.snd.metadata.model.Thumbnail
import java.net.URI
import java.time.Year

data class Series(
    val id: Long,
    val title: String,
    val description: String?,
    val type: Type?,
    val associatedNames: Collection<String>,
    val status: Status?,
    val image: URI?,
    val genres: Collection<String>,
    val categories: Collection<Category>,
    val authors: Collection<Author>,
    val year: Year?,
    val publishers: Collection<Publisher>,
)

data class Category(
    val id: Long,
    val name: String,
    val votes: Int,
    val votesPlus: Int,
    val votesMinus: Int,
)

data class Author(
    val id: Long?,
    val name: String,
    val type: String,
)

data class Publisher(
    val id: Long?,
    val name: String,
    val type: String,
    val notes: String?
)

enum class Status {
    COMPLETE,
    ONGOING,
    CANCELLED,
    HIATUS,
}

enum class Type {
    MANGA,
    DOUJINSHI,
    MANHWA,
    MANHUA,
}

fun Series.toSeriesMetadata(thumbnail: Thumbnail? = null): SeriesMetadata {
    val status = when (status) {
        COMPLETE -> SeriesMetadata.Status.ENDED
        ONGOING -> SeriesMetadata.Status.ONGOING
        CANCELLED -> SeriesMetadata.Status.ABANDONED
        HIATUS -> SeriesMetadata.Status.HIATUS
        else -> SeriesMetadata.Status.ONGOING
    }

    val artistRoles = listOf(
        PENCILLER,
        INKER,
        COLORIST,
        LETTERER,
        COVER
    )

    val authors = authors.flatMap {
        when (it.type) {
            "Author" -> listOf(org.snd.metadata.model.Author(it.name, WRITER.name))
            else -> artistRoles.map { role -> org.snd.metadata.model.Author(it.name, role.name) }
        }
    }


    val tags = categories.sortedByDescending { it.votes }.take(15).map { it.name }

    return SeriesMetadata(
        status = status,
        title = title,
        titleSort = title,
        summary = description ?: "",
        publisher = publishers.firstOrNull { it.type == "Original" }?.name,
        genres = genres,
        tags = tags,
        authors = authors,
        thumbnail = thumbnail,

        id = ProviderSeriesId(id.toString()),
        provider = MANGA_UPDATES
    )
}
