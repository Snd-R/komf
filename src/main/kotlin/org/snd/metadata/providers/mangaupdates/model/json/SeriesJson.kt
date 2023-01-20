package org.snd.metadata.providers.mangaupdates.model.json

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeriesJson(
    val series_id: Long,
    val title: String,
    val associated: Collection<AssociatedJson>,
    val description: String?,
    val image: ImageJson?,
    val type: String?,
    val year: String?,
    val genres: Collection<GenreJson>?,
    val categories: Collection<CategoryJson>?,
    val status: String?,
    val authors: Collection<AuthorJson>?,
    val publishers: Collection<PublisherJson>?,
    val url: String
)

@JsonClass(generateAdapter = true)
data class AssociatedJson(val title: String)

@JsonClass(generateAdapter = true)
data class CategoryJson(
    val series_id: Long,
    val category: String,
    val votes: Int,
    val votes_plus: Int,
    val votes_minus: Int,
    val added_by: Long?
)

@JsonClass(generateAdapter = true)
data class AuthorJson(
    val name: String,
    val type: String,
    val author_id: Long?,
)

@JsonClass(generateAdapter = true)
data class PublisherJson(
    val publisher_name: String,
    val publisher_id: Long?,
    val type: String,
    val notes: String?,
)
