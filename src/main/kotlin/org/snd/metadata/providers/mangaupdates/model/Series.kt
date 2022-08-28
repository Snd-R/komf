package org.snd.metadata.providers.mangaupdates.model

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
