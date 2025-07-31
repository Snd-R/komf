package snd.komf.providers

import kotlinx.serialization.Serializable
import snd.komf.model.AuthorRole
import snd.komf.model.AuthorRole.COLORIST
import snd.komf.model.AuthorRole.COVER
import snd.komf.model.AuthorRole.INKER
import snd.komf.model.AuthorRole.LETTERER
import snd.komf.model.AuthorRole.PENCILLER
import snd.komf.model.AuthorRole.WRITER
import snd.komf.model.MediaType
import snd.komf.model.MediaType.MANGA
import snd.komf.providers.mangadex.model.MangaDexLink
import snd.komf.util.NameSimilarityMatcher.NameMatchingMode

@Serializable
data class MetadataProvidersConfig(
    val malClientId: String? = null,
    val comicVineApiKey: String? = null,
    val comicVineSearchLimit: Int? = null,
    val bangumiToken: String? = null,
    val nameMatchingMode: NameMatchingMode = NameMatchingMode.CLOSEST_MATCH,
    val defaultProviders: ProvidersConfig = ProvidersConfig(),
    val libraryProviders: Map<String, ProvidersConfig> = emptyMap(),
    val mangabakaDatabaseDir: String = "./mangabaka",
)

@Serializable
data class ProvidersConfig(
    val mangaUpdates: ProviderConfig = ProviderConfig(),
    val mal: ProviderConfig = ProviderConfig(),
    val nautiljon: ProviderConfig = ProviderConfig(),
    val aniList: AniListConfig = AniListConfig(),
    val yenPress: ProviderConfig = ProviderConfig(),
    val kodansha: ProviderConfig = ProviderConfig(),
    val viz: ProviderConfig = ProviderConfig(),
    val bookWalker: ProviderConfig = ProviderConfig(),
    val mangaDex: MangaDexConfig = MangaDexConfig(),
    val bangumi: ProviderConfig = ProviderConfig(),
    val comicVine: ProviderConfig = ProviderConfig(),
    val hentag: ProviderConfig = ProviderConfig(),
    val mangaBaka: MangaBakaConfig = MangaBakaConfig(),
    val webtoons: ProviderConfig = ProviderConfig(),
)

@Serializable
data class ProviderConfig(
    val priority: Int = 10,
    val enabled: Boolean = false,
    val seriesMetadata: SeriesMetadataConfig = SeriesMetadataConfig(),
    val bookMetadata: BookMetadataConfig = BookMetadataConfig(),
    val nameMatchingMode: NameMatchingMode? = null,
    val mediaType: MediaType = MANGA,

    val authorRoles: Collection<AuthorRole> = listOf(WRITER),
    val artistRoles: Collection<AuthorRole> = listOf(PENCILLER, INKER, COLORIST, LETTERER, COVER),
)

@Serializable
data class MangaBakaConfig(
    val priority: Int = 10,
    val enabled: Boolean = false,
    val seriesMetadata: SeriesMetadataConfig = SeriesMetadataConfig(),
    val nameMatchingMode: NameMatchingMode? = null,
    val mediaType: MediaType = MANGA,

    val authorRoles: Collection<AuthorRole> = listOf(WRITER),
    val artistRoles: Collection<AuthorRole> = listOf(PENCILLER, INKER, COLORIST, LETTERER, COVER),

    val mode: MangaBakaMode = MangaBakaMode.API,
)

@Serializable
enum class MangaBakaMode {
    API, DATABASE
}

@Serializable
data class AniListConfig(
    val priority: Int = 10,
    val enabled: Boolean = false,
    val seriesMetadata: SeriesMetadataConfig = SeriesMetadataConfig(),
    val nameMatchingMode: NameMatchingMode? = null,
    val mediaType: MediaType = MANGA,

    val authorRoles: Collection<AuthorRole> = listOf(WRITER),
    val artistRoles: Collection<AuthorRole> = listOf(PENCILLER, INKER, COLORIST, LETTERER, COVER),
    val tagsScoreThreshold: Int = 60,
    val tagsSizeLimit: Int = 15,
)

@Serializable
data class MangaDexConfig(
    val priority: Int = 10,
    val enabled: Boolean = false,
    val seriesMetadata: SeriesMetadataConfig = SeriesMetadataConfig(),
    val bookMetadata: BookMetadataConfig = BookMetadataConfig(),
    val nameMatchingMode: NameMatchingMode? = null,
    val mediaType: MediaType = MANGA,

    val coverLanguages: List<String> = listOf("en", "ja"),
    val links: List<MangaDexLink> = emptyList(),

    val authorRoles: Collection<AuthorRole> = listOf(WRITER),
    val artistRoles: Collection<AuthorRole> = listOf(PENCILLER, INKER, COLORIST, LETTERER, COVER),
)

@Serializable
data class SeriesMetadataConfig(
    val status: Boolean = true,
    val title: Boolean = true,
    val titleSort: Boolean = true,
    val summary: Boolean = true,
    val publisher: Boolean = true,
    val readingDirection: Boolean = true,
    val ageRating: Boolean = true,
    val language: Boolean = true,
    val genres: Boolean = true,
    val tags: Boolean = true,
    val totalBookCount: Boolean = true,
    val authors: Boolean = true,
    val releaseDate: Boolean = true,
    val thumbnail: Boolean = true,
    val books: Boolean = true,
    val links: Boolean = true,
    val score: Boolean = true,

    val useOriginalPublisher: Boolean = false,
)

@Serializable
data class BookMetadataConfig(
    val title: Boolean = true,
    val summary: Boolean = true,
    val number: Boolean = true,
    val numberSort: Boolean = true,
    val releaseDate: Boolean = true,
    val authors: Boolean = true,
    val tags: Boolean = true,
    val isbn: Boolean = true,
    val links: Boolean = true,
    val thumbnail: Boolean = true,
)
