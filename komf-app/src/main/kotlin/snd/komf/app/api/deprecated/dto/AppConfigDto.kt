package snd.komf.app.api.deprecated.dto

import kotlinx.serialization.Serializable
import snd.komf.model.AuthorRole
import snd.komf.model.MediaType
import snd.komf.model.ReadingDirection
import snd.komf.model.UpdateMode
import snd.komf.util.NameSimilarityMatcher.NameMatchingMode

@Serializable
data class AppConfigDto(
    val komga: KomgaConfigDto,
    val kavita: KavitaConfigDto,
    val discord: DiscordConfigDto,
    val metadataProviders: MetadataProvidersConfigDto,
)

@Serializable
data class KomgaConfigDto(
    val baseUri: String,
    val komgaUser: String,
    val eventListener: EventListenerConfigDto,
    val notifications: NotificationConfigDto,
    val metadataUpdate: MetadataUpdateConfigDto,
)

@Serializable
data class KavitaConfigDto(
    val baseUri: String,
    val eventListener: EventListenerConfigDto,
    val notifications: NotificationConfigDto,
    val metadataUpdate: MetadataUpdateConfigDto,
)

@Serializable
data class NotificationConfigDto(
    val libraries: Collection<String>
)

@Serializable
data class MetadataUpdateConfigDto(
    val default: MetadataProcessingConfigDto,
    val library: Map<String, MetadataProcessingConfigDto>
)

@Serializable
data class MetadataProcessingConfigDto(
    val libraryType: MediaType,
    val aggregate: Boolean,
    val mergeTags: Boolean,
    val mergeGenres: Boolean,
    val bookCovers: Boolean,
    val seriesCovers: Boolean,
    val overrideExistingCovers: Boolean,
    var lockCovers: Boolean,
    val updateModes: List<UpdateMode>,
    val postProcessing: MetadataPostProcessingConfigDto

)

@Serializable
data class MetadataPostProcessingConfigDto(
    val seriesTitle: Boolean,
    val seriesTitleLanguage: String?,
    val alternativeSeriesTitles: Boolean?,
    val alternativeSeriesTitleLanguages: List<String>,
    val orderBooks: Boolean,
    val readingDirectionValue: ReadingDirection?,
    val languageValue: String?,
    val fallbackToAltTitle: Boolean,
)

@Serializable
data class DiscordConfigDto(
    val webhooks: Map<Int, String>?,
    val seriesCover: Boolean,
)

@Serializable
data class EventListenerConfigDto(
    val enabled: Boolean,
    val libraries: Collection<String>
)

@Serializable
data class MetadataProvidersConfigDto(
    val malClientId: String,
    val comicVineClientId: String?,
    val nameMatchingMode: NameMatchingMode,
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
    val mangaDex: ProviderConfigDto,
    val bangumi: ProviderConfigDto,
    val comicVine: ProviderConfigDto,
)

@Serializable
data class ProviderConfigDto(
    val nameMatchingMode: NameMatchingMode?,
    val priority: Int,
    val enabled: Boolean,
    val mediaType: MediaType,
    val authorRoles: Collection<AuthorRole>,
    val artistRoles: Collection<AuthorRole>,
    val seriesMetadata: SeriesMetadataConfigDto,
    val bookMetadata: BookMetadataConfigDto,
)

@Serializable
data class AniListConfigDto(
    val nameMatchingMode: NameMatchingMode?,
    val priority: Int,
    val enabled: Boolean,
    val mediaType: MediaType,
    val authorRoles: Collection<AuthorRole>,
    val artistRoles: Collection<AuthorRole>,
    val tagsScoreThreshold: Int,
    val tagsSizeLimit: Int,
    val seriesMetadata: SeriesMetadataConfigDto,

    @Deprecated("added for backwards compatibility")
    val bookMetadata: BookMetadataConfigDto,
)

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
