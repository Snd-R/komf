package org.snd.metadata.mal.model.json

import com.squareup.moshi.JsonClass
import java.time.ZonedDateTime

@JsonClass(generateAdapter = true)
data class SeriesJson(
    val id: Int,
    val title: String,
    val main_picture: PictureJson? = null,
    val alternative_titles: AlternativeTitlesJson? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val synopsis: String? = null,
    val mean: Double? = null,
    val rank: Int? = null,
    val popularity: Int? = null,
    val num_list_users: Int,
    val num_scoring_users: Int,
    val nsfw: String? = null,
    val genres: Set<GenreJson> = emptySet(),
    val created_at: ZonedDateTime,
    val updated_at: ZonedDateTime,
    val media_type: String,
    val status: String,
    val num_volumes: Int,
    val num_chapters: Int,
    val authors: List<AuthorJson> = emptyList(),
    val pictures: List<PictureJson> = emptyList(),
    val background: String? = null,
    val serialization: List<SerializationJson> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GenreJson(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class PictureJson(
    val large: String?,
    val medium: String
)

@JsonClass(generateAdapter = true)
data class AuthorJson(
    val node: AuthorNodeJson,
    val role: String
)

@JsonClass(generateAdapter = true)
data class AuthorNodeJson(
    val id: Int,
    val first_name: String,
    val last_name: String,
)

@JsonClass(generateAdapter = true)
data class SerializationJson(
    val node: SerializationNodeJson
)

@JsonClass(generateAdapter = true)
data class SerializationNodeJson(
    val id: Int,
    val name: String
)
