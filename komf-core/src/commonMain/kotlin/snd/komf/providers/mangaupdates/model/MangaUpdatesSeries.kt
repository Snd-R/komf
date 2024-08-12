package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class MangaUpdatesSeries(
    @SerialName("series_id")
    val id: Long,
    val title: String,
    val associated: Collection<MangaUpdatesAssociatedName>,
    val description: String?,
    val image: MangaUpdatesImage?,
    val year: String?,
    val genres: Collection<MangaUpdatesGenre>,
    val categories: Collection<MangaUpdatesCategory>,
    val status: String?,
    val authors: Collection<MangaUpdatesAuthor>,
    val publishers: Collection<MangaUpdatesPublisher>,
    val url: String,
    @SerialName("bayesian_rating")
    val bayesianRating: Double?
)

@Serializable
data class MangaUpdatesAssociatedName(val title: String)

@Serializable
data class MangaUpdatesCategory(
    @SerialName("series_id")
    val id: Long,
    val category: String,
    val votes: Int,
    @SerialName("votes_plus")
    val votesPlus: Int,
    @SerialName("votes_minus")
    val votesMinus: Int,
)

@Serializable
data class MangaUpdatesAuthor(
    @SerialName("author_id")
    val id: Long?,
    val name: String,
    val type: String,
)

@Serializable
data class MangaUpdatesPublisher(
    @SerialName("publisher_id")
    val id: Long?,
    @SerialName("publisher_name")
    val name: String,
    val type: String,
    val notes: String?
)
