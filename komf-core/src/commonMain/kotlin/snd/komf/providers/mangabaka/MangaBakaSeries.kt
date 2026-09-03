package snd.komf.providers.mangabaka

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

private const val baseUrl = "https://mangabaka.org"

@JvmInline
@Serializable
value class MangaBakaSeriesId(val value: Int) {
    override fun toString() = value.toString()
}

@Serializable
data class MangaBakaSeries(
    val id: MangaBakaSeriesId,
    @SerialName("has_anime")
    val hasAnime: Boolean,
    val anime: MangaBakaAnimeInfo? = null,
    val artists: List<String>? = null,
    val authors: List<String>? = null,
    @SerialName("canonical_url")
    val canonicalUrl: String,
    @SerialName("content_rating")
    val contentRating: MangaBakaContentRating,
    val cover: MangaBakaCover,
    val description: String? = null,
    @SerialName("final_volume")
    val finalVolume: String? = null,
    @SerialName("is_licensed")
    val isLicensed: Boolean,
    @SerialName("last_updated_at")
    val lastUpdatedAt: Instant? = null,
    val mergedWith: Int? = null,
    val publishers: List<MangaBakaPublisher>? = null,
    val rating: Double? = null,
    val state: MangaBakaSeriesState,
    val status: MangaBakaStatus,
    @SerialName("total_chapters")
    val totalChapters: String? = null,
    val type: MangaBakaType,
    @SerialName("links_v2")
    val linksV2: List<MangaBakaLink>? = null,
    val published: MangaBakaPublishedDate? = null,
    @SerialName("relationships_v2")
    val relationshipsV2: List<MangaBakaRelationship>? = null,
    @SerialName("tags_v2")
    val tagsV2: List<MangaBakaTags>? = null,
    val titles: List<MangaBakaTitle>? = null,
    val source: MangaBakaSource

) {
    fun url() = "$baseUrl/${id.value}"
}

@JvmInline
@Serializable
value class MangaBakaTagId(val value: Int) {
    override fun toString() = value.toString()
}

@Serializable
data class MangaBakaTags(
    val id: MangaBakaTagId,
    @SerialName("content_rating")
    val contentRating: MangaBakaContentRating,
    val description: String? = null,
    @SerialName("is_spoiler")
    val isSpoiler: Boolean? = null,
    val level: Int,
    val name: String,
    @SerialName("name_path")
    val namePath: String,
    @SerialName("parent_id")
    val parentId: MangaBakaTagId? = null,
    @SerialName("series_count")
    val seriesCount: Int,
    val impliedByTagIds: List<MangaBakaTagId> = emptyList(),
    @SerialName("is_explicit")
    val isExplicit: Boolean = false,
    @SerialName("is_genre")
    val isGenre: Boolean = false,
    val mergedWith: Int? = null,
    val weight: MangaBakaTagWeight = MangaBakaTagWeight.UNWEIGHTED,
)

enum class MangaBakaTagWeight {
    @SerialName("core")
    CORE,

    @SerialName("defining")
    DEFINING,

    @SerialName("recurrent")
    RECURRENT,

    @SerialName("incidental")
    INCIDENTAL,

    @SerialName("unweighted")
    UNWEIGHTED

}

@Serializable
data class MangaBakaTitle(
    val language: String,
    val title: String,
    val traits: List<MangaBakaTitleTrait>,
    @SerialName("is_primary")
    val isPrimary: Boolean? = null,
    val note: String? = null
)

enum class MangaBakaTitleTrait {
    @SerialName("official")
    OFFICIAL,

    @SerialName("native")
    NATIVE,

    @SerialName("alternative")
    ALTERNATIVE
}

@JvmInline
@Serializable
value class MangaBakaLinkId(val value: String) {
    override fun toString() = value
}

@Serializable
data class MangaBakaLink(
    val id: MangaBakaLinkId,
    val language: String,
    val name: String,
    @SerialName("name_display")
    val nameDisplay: String,
    val type: MangaBakaLinkType,
    val url: String,
)

enum class MangaBakaLinkType {
    @SerialName("retailer")
    RETAILER,

    @SerialName("publisher")
    PUBLISHER,

    @SerialName("webplatform")
    WEBPLATFORM,

    @SerialName("info")
    INFO,

    @SerialName("social")
    SOCIAL,

    @SerialName("news")
    NEWS,

    @SerialName("piracy")
    PIRACY,

    @SerialName("other")
    OTHER,
}

@Serializable
data class MangaBakaPublishedDate(
    @SerialName("end_date")
    val endDate: LocalDate? = null,
    @SerialName("end_date_is_esitmated")
    val endDateIsEstimated: Boolean? = null,
    @SerialName("start_date")
    val startDate: LocalDate? = null,
    @SerialName("start_date_is_estimated")
    val startDateIsEstimated: Boolean? = null,
)

@Serializable
data class MangaBakaCover(
    val raw: MangaBakaCoverRaw? = null,
    val x150: MangaBakaCoverDpi? = null,
    val x250: MangaBakaCoverDpi? = null,
    val x350: MangaBakaCoverDpi? = null,
)

@Serializable
data class MangaBakaCoverRaw(
    val url: String? = null,
    val size: Long? = null,
    val height: Int? = null,
    val width: Int? = null,
    val blurhash: String? = null,
    val thumbhash: String? = null,
    val format: String? = null,
)

@Serializable
data class MangaBakaCoverDpi(
    val x1: String? = null,
    val x2: String? = null,
    val x3: String? = null,
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

@Serializable
data class MangaBakaAnimeInfo(
    val start: String? = null,
    val end: String? = null
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

@JvmInline
@Serializable
value class MangaBakaRelationshipId(val value: String) {
    override fun toString() = value
}

@Serializable
data class MangaBakaRelationship(
    val id: MangaBakaRelationshipId,
    val chronology: MangaBakaRelationshipChronology,
    @SerialName("is_manual")
    val isManual: Boolean,
    val note: String? = null,
    @SerialName("relation_type")
    val relationType: MangaBakaRelationType,
    @SerialName("to_series_id")
    val toSeriesId: MangaBakaSeriesId
)

enum class MangaBakaRelationType {
    @SerialName("adaptation")
    ADAPTATION,

    @SerialName("alternative")
    ALTERNATIVE,

    @SerialName("cameo")
    CAMEO,

    @SerialName("character_focus")
    CHARACTER_FOCUS,

    @SerialName("compilation")
    COMPILATION,

    @SerialName("contains")
    CONTAINS,

    @SerialName("crossover")
    CROSSOVER,

    @SerialName("expansion")
    EXPANSION,

    @SerialName("main")
    MAIN,

    @SerialName("other")
    OTHER,

    @SerialName("parent")
    PARENT,

    @SerialName("parody")
    PARODY,

    @SerialName("prequel")
    PREQUEL,

    @SerialName("reboot")
    REBOOT,

    @SerialName("remake")
    REMAKE,

    @SerialName("sequel")
    SEQUEL,

    @SerialName("series")
    SERIES,

    @SerialName("side_story")
    SIDE_STORY,

    @SerialName("source")
    SOURCE,

    @SerialName("spin_off")
    SPIN_OFF,

    @SerialName("summary")
    SUMMARY,

    @SerialName("uncollected")
    UNCOLLECTED,
}

enum class MangaBakaRelationshipChronology {
    @SerialName("narrative")
    NARRATIVE,

    @SerialName("release")
    RELEASE,

    @SerialName("unknown")
    UNKNOWN
}

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

@Serializable
data class MangaBakaSource(
    val anilist: MangaBakaAniListSource,
    @SerialName("anime_news_network")
    val animeNewsNetwork: MangaBakaAnimeNewsNetworkSource,
    @SerialName("anime_planet")
    val animePlanet: MangaBakaAnimePlanetSource,
    val kitsu: MangaBakaKitsuSource,
    @SerialName("manga_updates")
    val mangaUpdates: MangaBakaMangaUpdatesSource,
    @SerialName("my_anime_list")
    val myAnimeList: MangaBakaMyAnimeListSource,
    val shikimori: MangaBakaShikimoriSource,
)

@Serializable
data class MangaBakaAniListSource(
    val id: Int?,
    val rating: Double?,
    @SerialName("rating_normalized")
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaAnimeNewsNetworkSource(
    val id: Int?,
    val rating: Double?,
    @SerialName("rating_normalized")
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaAnimePlanetSource(
    val id: String?,
    val rating: Double?,
    @SerialName("rating_normalized")
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaKitsuSource(
    val id: Int?,
    val rating: Double?,
    @SerialName("rating_normalized")
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaMangaUpdatesSource(
    val id: String?,
    val rating: Double?,
    @SerialName("rating_normalized")
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaMyAnimeListSource(
    val id: Int?,
    val rating: Double?,
    @SerialName("rating_normalized")
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaShikimoriSource(
    val id: Int?,
    val rating: Double?,
    @SerialName("rating_normalized")
    val ratingNormalized: Int?
)
