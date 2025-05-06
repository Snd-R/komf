package snd.komf.providers.mangabaka.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

private const val baseUrl = "https://mangabaka.dev"

@JvmInline
@Serializable
value class MangaBakaSeriesId(val value: Int) {
    override fun toString() = value.toString()
}

@Serializable
data class MangaBakaSeries(
    val id: MangaBakaSeriesId,
    val title: String,
    @SerialName("native_title")
    val nativeTitle: String? = null,
    @SerialName("secondary_titles")
    val secondaryTitles: Map<String, List<String>>,
    val cover: String? = null,
    val authors: List<String>? = null,
    val artists: List<String>? = null,
    val description: String? = null,
    val year: Int? = null,
    val status: MangaBakaStatus? = null,

    @SerialName("is_licensed")
    val isLicensed: Boolean? = null,
    @SerialName("has_anime")
    val hasAnime: Boolean? = null,
    val anime: MangaBakaAnimeInfo? = null,

    @SerialName("is_nsfw")
    val isNsfw: Boolean? = null,
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
    val relationships: MangaBakaRelationships? = null,
    val source: MangaBakaSources? = null
) {
    fun url() = "$baseUrl/${id.value}"
}

@Serializable(with = MangaBakaStatusSerializer::class)
enum class MangaBakaStatus {
    RELEASING,
    UPCOMING,
    COMPLETED,
    CANCELLED,
    HIATUS,
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

@Serializable(with = MangaBakaTypeSerializer::class)
enum class MangaBakaType {
    MANGA,
    NOVEL,
    MANHWA,
    MANHUA,
    OEL,
    OTHER,
}

class MangaBakaTypeSerializer : KSerializer<MangaBakaType> {
    override val descriptor = PrimitiveSerialDescriptor("MangaBakaType", PrimitiveKind.STRING).nullable

    override fun serialize(encoder: Encoder, value: MangaBakaType) = encoder.encodeString(value.name.lowercase())

    override fun deserialize(decoder: Decoder): MangaBakaType {
        return runCatching {
            MangaBakaType.valueOf(decoder.decodeString().uppercase())
        }.getOrElse { MangaBakaType.OTHER }
    }
}

@Serializable
data class MangaBakaPublisher(
    val name: String,
    val note: String,
    // Original, English
    val type: String
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
    val mangadex: MangaBakaMangadexSource? = null,
    val anilist: MangaBakaAnilistSource? = null,

    @SerialName("manga_updates")
    val mangaUpdates: MangaBakaMangaUpdatesSource? = null,

    @SerialName("my_anime_list")
    val myAnimeList: MangaBakaMyAnimeListSource? = null,
    val kitsu: MangaBakaKitsuSource? = null,
)

@Serializable
data class MangaBakaMangadexSource(
    val id: String? = null,
    val rating: Double? = null,
    val response: JsonElement? = null,
    val statistics: JsonElement? = null,
)

@Serializable
data class MangaBakaAnilistSource(
    val id: Int? = null,
    val rating: Double? = null,
    val response: JsonElement? = null,
)

@Serializable
data class MangaBakaMangaUpdatesSource(
    val id: String? = null,
    val rating: Double? = null,
    val response: JsonElement? = null,
)

@Serializable
data class MangaBakaMyAnimeListSource(
    val id: Int? = null,
    val rating: Double? = null,
    val response: JsonElement? = null,
)

@Serializable
data class MangaBakaKitsuSource(
    val id: Int? = null,
    val rating: Double? = null,
    val response: JsonElement? = null,
)
