package snd.komf.api.mangabaka

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import snd.komf.api.KomfServerSeriesId
import kotlin.jvm.JvmInline
import kotlin.time.Instant

@JvmInline
@Serializable
value class MangaBakaSeriesId(val value: Long) {
    override fun toString() = value.toString()
}


@Serializable
data class KomfMangaBakaLinkedSeries(
    val komgaId: KomfServerSeriesId,
    val mangaBaka: KomfMangaBakaSeries
)

@Serializable
data class KomfMangaBakaSeries(
    val id: MangaBakaSeriesId,
    val hasAnime: Boolean,
    val anime: MangaBakaAnimeInfo?,
    val artists: List<String>?,
    val authors: List<String>?,
    val canonicalUrl: String,
    val contentRating: MangaBakaContentRating,
    val cover: MangaBakaCover,
    val description: String?,
    val finalVolume: String?,
    val isLicensed: Boolean,
    val lastUpdatedAt: Instant?,
    val mergedWith: Long?,
    val publishers: List<MangaBakaPublisher>?,
    val rating: Double?,
    val state: MangaBakaSeriesState,
    val status: MangaBakaStatus,
    val totalChapters: String?,
    val type: MangaBakaType,
    val links: List<MangaBakaLink>?,
    val published: MangaBakaPublishedDate?,
    val relationships: List<MangaBakaRelationship>?,
    val tags: List<MangaBakaSeriesTag>?,
    val titles: List<MangaBakaTitle>?,
    val source: MangaBakaSource
)

@JvmInline
@Serializable
value class MangaBakaTagId(val value: Long) {
    override fun toString() = value.toString()
}

@Serializable
data class MangaBakaSeriesTag(
    val id: MangaBakaTagId,
    val contentRating: MangaBakaContentRating,
    val description: String?,
    val isSpoiler: Boolean?,
    val level: Int,
    val name: String,
    val namePath: String,
    val parentId: MangaBakaTagId?,
    val seriesCount: Int,
    val impliedByTagIds: List<MangaBakaTagId>,
    val isExplicit: Boolean,
    val isGenre: Boolean,
    val mergedWith: Long?,
    val weight: MangaBakaTagWeight,
)

enum class MangaBakaTagWeight {
    CORE,
    DEFINING,
    RECURRENT,
    INCIDENTAL,
    UNWEIGHTED
}

@Serializable
data class MangaBakaTitle(
    val language: String,
    val title: String,
    val traits: List<MangaBakaTitleTrait>,
    val isPrimary: Boolean?,
    val note: String?
)

enum class MangaBakaTitleTrait {
    OFFICIAL,
    NATIVE,
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
    val nameDisplay: String,
    val type: MangaBakaLinkType,
    val url: String,
)

enum class MangaBakaLinkType {
    PUBLISHER,
    RETAILER,
    WEBPLATFORM,
    INFO,
    SOCIAL,
    NEWS,
    PIRACY,
    OTHER,
}

@Serializable
data class MangaBakaPublishedDate(
    val endDate: LocalDate?,
    val endDateIsEstimated: Boolean?,
    val startDate: LocalDate?,
    val startDateIsEstimated: Boolean?,
)

@Serializable
data class MangaBakaCover(
    val raw: MangaBakaCoverRaw?,
    val x150: MangaBakaCoverDpi?,
    val x250: MangaBakaCoverDpi?,
    val x350: MangaBakaCoverDpi?,
)

@Serializable
data class MangaBakaCoverRaw(
    val url: String?,
    val size: Long?,
    val height: Int?,
    val width: Int?,
    val blurhash: String?,
    val thumbhash: String?,
    val format: String?,
)

@Serializable
data class MangaBakaCoverDpi(
    val x1: String?,
    val x2: String?,
    val x3: String?,
)

@Serializable
enum class MangaBakaStatus {
    CANCELLED,
    COMPLETED,
    HIATUS,
    RELEASING,
    UPCOMING,
    UNKNOWN,
}

@Serializable
data class MangaBakaAnimeInfo(
    val start: String?,
    val end: String?
)

@Serializable
enum class MangaBakaType {
    MANGA,
    NOVEL,
    MANHWA,
    MANHUA,
    OEL,
    OTHER,
}

@Serializable
data class MangaBakaPublisher(
    val name: String?,
    val note: String?,
    // Original, English
    val type: String?
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
    val isManual: Boolean,
    val note: String?,
    val relationType: MangaBakaRelationType,
    val toSeriesId: MangaBakaSeriesId
)

enum class MangaBakaRelationType {
    ADAPTATION,
    ALTERNATIVE,
    CAMEO,
    CHARACTER_FOCUS,
    COMPILATION,
    CONTAINS,
    CROSSOVER,
    EXPANSION,
    MAIN,
    OTHER,
    PARENT,
    PARODY,
    PREQUEL,
    REBOOT,
    REMAKE,
    SEQUEL,
    SERIES,
    SIDE_STORY,
    SOURCE,
    SPIN_OFF,
    SUMMARY,
    UNCOLLECTED,
}

enum class MangaBakaRelationshipChronology {
    NARRATIVE,
    RELEASE,
    UNKNOWN
}

@Serializable
enum class MangaBakaSeriesState {
    ACTIVE,
    MERGED,
    DELETED
}

@Serializable
enum class MangaBakaContentRating {
    SAFE,
    SUGGESTIVE,
    EROTICA,
    PORNOGRAPHIC,
}

@Serializable
data class MangaBakaSource(
    val anilist: MangaBakaAniListSource,
    val animeNewsNetwork: MangaBakaAnimeNewsNetworkSource,
    val animePlanet: MangaBakaAnimePlanetSource,
    val kitsu: MangaBakaKitsuSource,
    val mangaUpdates: MangaBakaMangaUpdatesSource,
    val myAnimeList: MangaBakaMyAnimeListSource,
    val shikimori: MangaBakaShikimoriSource,
)

@Serializable
data class MangaBakaAniListSource(
    val id: Int?,
    val rating: Double?,
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaAnimeNewsNetworkSource(
    val id: Int?,
    val rating: Double?,
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaAnimePlanetSource(
    val id: String?,
    val rating: Double?,
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaKitsuSource(
    val id: Int?,
    val rating: Double?,
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaMangaUpdatesSource(
    val id: String?,
    val rating: Double?,
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaMyAnimeListSource(
    val id: Int?,
    val rating: Double?,
    val ratingNormalized: Int?
)

@Serializable
data class MangaBakaShikimoriSource(
    val id: Int?,
    val rating: Double?,
    val ratingNormalized: Int?
)
