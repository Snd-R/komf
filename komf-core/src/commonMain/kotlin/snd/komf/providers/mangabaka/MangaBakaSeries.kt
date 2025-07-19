package snd.komf.providers.mangabaka

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val baseUrl = "https://mangabaka.dev"

@JvmInline
@Serializable
value class MangaBakaSeriesId(val value: Int) {
    override fun toString() = value.toString()
}

@Serializable
data class MangaBakaSeries(
    val id: MangaBakaSeriesId,
    val state: MangaBakaSeriesState,
    val mergedWith: Int? = null,
    val title: String,
    @SerialName("native_title")
    val nativeTitle: String? = null,
    @SerialName("romanized_title")
    val romanizedTitle: String? = null,
    @SerialName("secondary_titles")
    val secondaryTitles: Map<String, List<MangaBakaSecondaryTitle>>? = null,
    val cover: MangaBakaCover,
    val authors: List<String>? = null,
    val artists: List<String>? = null,
    val description: String? = null,
    val year: Int? = null,
    val status: MangaBakaStatus,

    @SerialName("is_licensed")
    val isLicensed: Boolean,
    @SerialName("has_anime")
    val hasAnime: Boolean? = null,
    val anime: MangaBakaAnimeInfo? = null,

    @SerialName("content_rating")
    val contentRating: MangaBakaContentRating,
    val type: MangaBakaType,

    val rating: Double? = null,
    @SerialName("final_volume")
    val finalVolume: String? = null,
    @SerialName("final_chapter")
    val finalChapter: String? = null,
    @SerialName("total_chapter")
    val totalChapter: String? = null,

    val links: List<String>? = null,
    val publishers: List<MangaBakaPublisher>? = null,
    val genres: List<String>? = null,
    val tags: List<String>? = null,
    @SerialName("last_updated_at")
    val lastUpdatedAt: Instant? = null,
    val relationships: MangaBakaRelationships? = null,
    val source: MangaBakaSources
) {
    fun url() = "$baseUrl/${id.value}"
}

@Serializable
data class MangaBakaSecondaryTitle(
    val type: String,
    val title: String,
)

@Serializable
data class MangaBakaCover(
    val raw: String? = null,
    val default: String? = null,
    val small: String? = null
)

@Serializable
enum class MangaBakaStatus {
    @SerialName("cancelled")
    CANCELLED,

    @SerialName("completed")
    COMPLETED,

    @SerialName("hiatus")
    HIATUS,

    @SerialName("releasing")
    RELEASING,

    @SerialName("upcoming")
    UPCOMING,

    @SerialName("unknown")
    UNKNOWN,
}

class MangaBakaStatusSerializer : KSerializer<MangaBakaStatus> {
    override val descriptor = PrimitiveSerialDescriptor("MangaBakaStatus", PrimitiveKind.STRING).nullable

    override fun serialize(encoder: Encoder, value: MangaBakaStatus) = encoder.encodeString(value.name.lowercase())

    override fun deserialize(decoder: Decoder): MangaBakaStatus {
        return runCatching {
            MangaBakaStatus.valueOf(decoder.decodeString().uppercase())
        }.getOrElse { MangaBakaStatus.UNKNOWN }
    }
}

@Serializable
data class MangaBakaAnimeInfo(
    val start: String?,
    val end: String?
)

@Serializable
enum class MangaBakaType {
    @SerialName("manga")
    MANGA,

    @SerialName("novel")
    NOVEL,

    @SerialName("manhwa")
    MANHWA,

    @SerialName("manhua")
    MANHUA,

    @SerialName("oel")
    OEL,

    @SerialName("other")
    OTHER,
}

@Serializable
data class MangaBakaPublisher(
    val name: String? = null,
    val note: String? = null,
    // Original, English
    val type: String? = null
)


@Serializable
data class MangaBakaRelationships(
    @SerialName("main_story")
    val mainStory: List<Int>? = null,
    val adaptation: List<Int>? = null,
    val prequel: List<Int>? = null,
    val sequel: List<Int>? = null,
    @SerialName("side_story")
    val sideStory: List<Int>? = null,
    @SerialName("spin_off")
    val spinOff: List<Int>? = null,
    val alternative: List<Int>? = null,
    val other: List<Int>? = null,
)

@Serializable
data class MangaBakaSources(
    val anilist: MangaBakaAnilistSource,
    @SerialName("anime_news_network")
    val animeNewsNetwork: MangaBakaAnimeNewsNetworkSource,
    val kitsu: MangaBakaKitsuSource,
    @SerialName("manga_updates")
    val mangaUpdates: MangaBakaMangaUpdatesSource,
    val mangadex: MangaBakaMangaDexSource,
    @SerialName("my_anime_list")
    val myAnimeList: MangaBakaMyAnimeListSource,
)

@Serializable
data class MangaBakaAnilistSource(
    val id: Int? = null,
    val rating: Double? = null,
)

@Serializable
data class MangaBakaAnimeNewsNetworkSource(
    val id: Int? = null,
    val rating: Double? = null,
)

@Serializable
data class MangaBakaKitsuSource(
    val id: Int? = null,
    val rating: Double? = null,
)

@Serializable
data class MangaBakaMangaUpdatesSource(
    val id: String? = null,
    val rating: Double? = null,
)

@Serializable
data class MangaBakaMangaDexSource(
    val id: String? = null,
    val rating: Double? = null,
)

@Serializable
data class MangaBakaMyAnimeListSource(
    val id: Int? = null,
    val rating: Double? = null,
)

@Serializable
enum class MangaBakaSeriesState {
    @SerialName("active")
    ACTIVE,

    @SerialName("merged")
    MERGED,

    @SerialName("deleted")
    DELETED
}

@Serializable
enum class MangaBakaContentRating {
    @SerialName("safe")
    SAFE,

    @SerialName("suggestive")
    SUGGESTIVE,

    @SerialName("erotica")
    EROTICA,

    @SerialName("pornographic")
    PORNOGRAPHIC,
}
