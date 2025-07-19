package snd.komf.api.config

import kotlinx.serialization.Serializable
import snd.komf.api.KomfAuthorRole
import snd.komf.api.KomfMediaType
import snd.komf.api.KomfNameMatchingMode
import snd.komf.api.KomfReadingDirection
import snd.komf.api.KomfUpdateMode
import snd.komf.api.MangaBakaMode
import snd.komf.api.MangaDexLink

@Serializable
data class KomfConfig(
    val komga: KomgaConfigDto,
    val kavita: KavitaConfigDto,
    val notifications: NotificationConfigDto,
    val metadataProviders: MetadataProvidersConfigDto,
)

@Serializable
data class KomgaConfigDto(
    val baseUri: String,
    val komgaUser: String,
    val eventListener: EventListenerConfigDto,
    val metadataUpdate: MetadataUpdateConfigDto,
)

@Serializable
data class KavitaConfigDto(
    val baseUri: String,
    val eventListener: EventListenerConfigDto,
    val metadataUpdate: MetadataUpdateConfigDto,
)

@Serializable
data class MetadataUpdateConfigDto(
    val default: MetadataProcessingConfigDto,
    val library: Map<String, MetadataProcessingConfigDto>
)

@Serializable
data class MetadataProcessingConfigDto(
    val libraryType: KomfMediaType,
    val aggregate: Boolean,
    val mergeTags: Boolean,
    val mergeGenres: Boolean,
    val bookCovers: Boolean,
    val seriesCovers: Boolean,
    val overrideExistingCovers: Boolean,
    val lockCovers: Boolean,

    val updateModes: List<KomfUpdateMode>,
    val postProcessing: MetadataPostProcessingConfigDto

)

@Serializable
data class MetadataPostProcessingConfigDto(
    val seriesTitle: Boolean,
    val seriesTitleLanguage: String?,
    val alternativeSeriesTitles: Boolean?,
    val alternativeSeriesTitleLanguages: List<String>,
    val orderBooks: Boolean,
    val readingDirectionValue: KomfReadingDirection?,
    val languageValue: String?,
    val fallbackToAltTitle: Boolean,

    val scoreTagName: String?,
    val originalPublisherTagName: String?,
    val publisherTagNames: List<PublisherTagNameConfigDto>,
)

@Serializable
data class PublisherTagNameConfigDto(
    val tagName: String,
    val language: String
)

@Serializable
data class EventListenerConfigDto(
    val enabled: Boolean,
    val metadataLibraryFilter: Collection<String>,
    val metadataSeriesExcludeFilter: Collection<String>,
    val notificationsLibraryFilter: Collection<String>,
)

@Serializable
data class MetadataProvidersConfigDto(
    val malClientId: String?,
    val comicVineClientId: String?,
    val nameMatchingMode: KomfNameMatchingMode,
    val mangaBakaDbAvailable: Boolean,
    val defaultProviders: ProvidersConfigDto,
    val libraryProviders: Map<String, ProvidersConfigDto>,
)

@Serializable
data class ProvidersConfigDto(
    val mangaUpdates: ProviderConfigDto,
    val mal: ProviderConfigDto,
    val nautiljon: ProviderConfigDto,
    val aniList: AniListConfigDto,
    val yenPress: ProviderConfigDto,
    val kodansha: ProviderConfigDto,
    val viz: ProviderConfigDto,
    val bookWalker: ProviderConfigDto,
    val mangaDex: MangaDexConfigDto,
    val bangumi: ProviderConfigDto,
    val comicVine: ProviderConfigDto,
    val hentag: ProviderConfigDto,
    val mangaBaka: MangaBakaConfigDto,
    val webtoons: ProviderConfigDto,
)

sealed interface ProviderConf {
    val priority: Int
    val enabled: Boolean
    val seriesMetadata: SeriesMetadataConfigDto
    val bookMetadata: BookMetadataConfigDto?
    val mediaType: KomfMediaType?
    val nameMatchingMode: KomfNameMatchingMode?

    val authorRoles: Collection<KomfAuthorRole>
    val artistRoles: Collection<KomfAuthorRole>
}

@Serializable
data class ProviderConfigDto(
    override val priority: Int,
    override val enabled: Boolean,
    override val seriesMetadata: SeriesMetadataConfigDto,
    override val bookMetadata: BookMetadataConfigDto,
    override val nameMatchingMode: KomfNameMatchingMode?,
    override val mediaType: KomfMediaType,

    override val authorRoles: Collection<KomfAuthorRole>,
    override val artistRoles: Collection<KomfAuthorRole>,
) : ProviderConf

@Serializable
data class AniListConfigDto(
    override val priority: Int,
    override val enabled: Boolean,
    override val seriesMetadata: SeriesMetadataConfigDto,
    override val nameMatchingMode: KomfNameMatchingMode?,
    override val mediaType: KomfMediaType,

    override val authorRoles: Collection<KomfAuthorRole>,
    override val artistRoles: Collection<KomfAuthorRole>,

    val tagsScoreThreshold: Int,
    val tagsSizeLimit: Int,
) : ProviderConf {
    override val bookMetadata: BookMetadataConfigDto? = null
}

@Serializable
data class MangaDexConfigDto(
    override val priority: Int,
    override val enabled: Boolean,
    override val seriesMetadata: SeriesMetadataConfigDto,
    override val bookMetadata: BookMetadataConfigDto,
    override val nameMatchingMode: KomfNameMatchingMode?,
    override val mediaType: KomfMediaType,

    override val authorRoles: Collection<KomfAuthorRole>,
    override val artistRoles: Collection<KomfAuthorRole>,

    val coverLanguages: List<String>,
    val links: List<MangaDexLink>,
) : ProviderConf

@Serializable
data class MangaBakaConfigDto(
    override val priority: Int,
    override val enabled: Boolean,
    override val seriesMetadata: SeriesMetadataConfigDto,
    override val nameMatchingMode: KomfNameMatchingMode?,
    override val mediaType: KomfMediaType,

    override val authorRoles: Collection<KomfAuthorRole>,
    override val artistRoles: Collection<KomfAuthorRole>,
    val mode: MangaBakaMode,
) : ProviderConf {
    override val bookMetadata: BookMetadataConfigDto? = null
}

@Serializable
data class SeriesMetadataConfigDto(
    val status: Boolean,
    val title: Boolean,
    val summary: Boolean,
    val publisher: Boolean,
    val readingDirection: Boolean,
    val ageRating: Boolean,
    val language: Boolean,
    val genres: Boolean,
    val tags: Boolean,
    val totalBookCount: Boolean,
    val authors: Boolean,
    val releaseDate: Boolean,
    val thumbnail: Boolean,
    val links: Boolean,
    val books: Boolean,
    val useOriginalPublisher: Boolean,
    val originalPublisherTagName: String?,
    val englishPublisherTagName: String?,
    val frenchPublisherTagName: String?,
)

@Serializable
data class BookMetadataConfigDto(
    val title: Boolean,
    val summary: Boolean,
    val number: Boolean,
    val numberSort: Boolean,
    val releaseDate: Boolean,
    val authors: Boolean,
    val tags: Boolean,
    val isbn: Boolean,
    val links: Boolean,
    val thumbnail: Boolean,
)

@Serializable
data class NotificationConfigDto(
    val apprise: AppriseConfigDto,
    val discord: DiscordConfigDto,
)

@Serializable
data class DiscordConfigDto(
    val webhooks: List<String>?,
    val seriesCover: Boolean,
)

@Serializable
data class AppriseConfigDto(
    val urls: List<String>?,
    val seriesCover: Boolean,
)
