package snd.komf.api.config

import kotlinx.serialization.Serializable
import snd.komf.api.KomfAuthorRole
import snd.komf.api.KomfMediaType
import snd.komf.api.KomfNameMatchingMode
import snd.komf.api.KomfReadingDirection
import snd.komf.api.KomfUpdateMode
import snd.komf.api.PatchValue

@Serializable
data class KomfConfigUpdateRequest(
    val komga: PatchValue<KomgaConfigUpdateRequest> = PatchValue.Unset,
    val kavita: PatchValue<KavitaConfigUpdateRequest> = PatchValue.Unset,
    val notifications: PatchValue<NotificationConfigUpdateRequest> = PatchValue.Unset,
    val metadataProviders: PatchValue<MetadataProvidersConfigUpdateRequest> = PatchValue.Unset,
)

@Serializable
data class KomgaConfigUpdateRequest(
    val baseUri: PatchValue<String> = PatchValue.Unset,
    val komgaUser: PatchValue<String> = PatchValue.Unset,
    val komgaPassword: PatchValue<String> = PatchValue.Unset,
    val eventListener: PatchValue<EventListenerConfigUpdateRequest> = PatchValue.Unset,
    val metadataUpdate: PatchValue<MetadataUpdateConfigUpdateRequest> = PatchValue.Unset,
)

@Serializable
data class KavitaConfigUpdateRequest(
    val baseUri: PatchValue<String> = PatchValue.Unset,
    val apiKey: PatchValue<String> = PatchValue.Unset,
    val eventListener: PatchValue<EventListenerConfigUpdateRequest> = PatchValue.Unset,
    val metadataUpdate: PatchValue<MetadataUpdateConfigUpdateRequest> = PatchValue.Unset,
)

@Serializable
data class MetadataUpdateConfigUpdateRequest(
    val default: PatchValue<MetadataProcessingConfigUpdateRequest> = PatchValue.Unset,
    val library: PatchValue<Map<String, MetadataProcessingConfigUpdateRequest?>> = PatchValue.Unset
)

@Serializable
data class MetadataProcessingConfigUpdateRequest(
    val libraryType: PatchValue<KomfMediaType> = PatchValue.Unset,
    val aggregate: PatchValue<Boolean> = PatchValue.Unset,
    val mergeTags: PatchValue<Boolean> = PatchValue.Unset,
    val mergeGenres: PatchValue<Boolean> = PatchValue.Unset,

    val bookCovers: PatchValue<Boolean> = PatchValue.Unset,
    val seriesCovers: PatchValue<Boolean> = PatchValue.Unset,
    val overrideExistingCovers: PatchValue<Boolean> = PatchValue.Unset,
    val updateModes: PatchValue<Collection<KomfUpdateMode>> = PatchValue.Unset,
    val overrideComicInfo: PatchValue<Boolean> = PatchValue.Unset,

    val postProcessing: PatchValue<MetadataPostProcessingConfigUpdateRequest> = PatchValue.Unset
)

@Serializable
class MetadataPostProcessingConfigUpdateRequest(
    val seriesTitle: PatchValue<Boolean> = PatchValue.Unset,
    val seriesTitleLanguage: PatchValue<String> = PatchValue.Unset,
    val alternativeSeriesTitles: PatchValue<Boolean> = PatchValue.Unset,
    val alternativeSeriesTitleLanguages: PatchValue<List<String>> = PatchValue.Unset,
    val fallbackToAltTitle: PatchValue<Boolean> = PatchValue.Unset,

    val orderBooks: PatchValue<Boolean> = PatchValue.Unset,
    val readingDirectionValue: PatchValue<KomfReadingDirection> = PatchValue.Unset,
    val languageValue: PatchValue<String> = PatchValue.Unset,

    val scoreTagName: PatchValue<String> = PatchValue.Unset,
    val originalPublisherTagName: PatchValue<String> = PatchValue.Unset,
    val publisherTagNames: PatchValue<List<PublisherTagNameConfigDto>> = PatchValue.Unset,
)

@Serializable
data class EventListenerConfigUpdateRequest(
    val enabled: PatchValue<Boolean> = PatchValue.Unset,
    val metadataLibraryFilter: PatchValue<Collection<String>> = PatchValue.Unset,
    val metadataExcludeSeriesFilter: PatchValue<Collection<String>> = PatchValue.Unset,
    val notificationsLibraryFilter: PatchValue<Collection<String>> = PatchValue.Unset,
)

@Serializable
class MetadataProvidersConfigUpdateRequest(
    val comicVineClientId: PatchValue<String> = PatchValue.Unset,
    val malClientId: PatchValue<String> = PatchValue.Unset,
    val nameMatchingMode: PatchValue<KomfNameMatchingMode> = PatchValue.Unset,
    val defaultProviders: PatchValue<ProvidersConfigUpdateRequest> = PatchValue.Unset,
    val libraryProviders: PatchValue<Map<String, ProvidersConfigUpdateRequest?>> = PatchValue.Unset,
)

@Serializable
data class ProvidersConfigUpdateRequest(
    val mangaUpdates: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val mal: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val nautiljon: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val aniList: PatchValue<AniListConfigUpdateRequest> = PatchValue.Unset,
    val yenPress: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val kodansha: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val viz: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val bookWalker: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val mangaDex: PatchValue<MangaDexConfigUpdateRequest> = PatchValue.Unset,
    val bangumi: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
    val comicVine: PatchValue<ProviderConfigUpdateRequest> = PatchValue.Unset,
)

@Serializable
class ProviderConfigUpdateRequest(
    val priority: PatchValue<Int> = PatchValue.Unset,
    val enabled: PatchValue<Boolean> = PatchValue.Unset,
    val seriesMetadata: PatchValue<SeriesMetadataConfigUpdateRequest> = PatchValue.Unset,
    val bookMetadata: PatchValue<BookMetadataConfigUpdateRequest> = PatchValue.Unset,
    val nameMatchingMode: PatchValue<KomfNameMatchingMode> = PatchValue.Unset,
    val mediaType: PatchValue<KomfMediaType> = PatchValue.Unset,

    val authorRoles: PatchValue<Collection<KomfAuthorRole>> = PatchValue.Unset,
    val artistRoles: PatchValue<Collection<KomfAuthorRole>> = PatchValue.Unset,
)

@Serializable
class AniListConfigUpdateRequest(
    val priority: PatchValue<Int> = PatchValue.Unset,
    val enabled: PatchValue<Boolean> = PatchValue.Unset,
    val seriesMetadata: PatchValue<SeriesMetadataConfigUpdateRequest> = PatchValue.Unset,
    val nameMatchingMode: PatchValue<KomfNameMatchingMode> = PatchValue.Unset,
    val mediaType: PatchValue<KomfMediaType> = PatchValue.Unset,

    val authorRoles: PatchValue<Collection<KomfAuthorRole>> = PatchValue.Unset,
    val artistRoles: PatchValue<Collection<KomfAuthorRole>> = PatchValue.Unset,
    val tagsScoreThreshold: PatchValue<Int> = PatchValue.Unset,
    val tagsSizeLimit: PatchValue<Int> = PatchValue.Unset,
)

@Serializable
class MangaDexConfigUpdateRequest(
    val priority: PatchValue<Int> = PatchValue.Unset,
    val enabled: PatchValue<Boolean> = PatchValue.Unset,
    val seriesMetadata: PatchValue<SeriesMetadataConfigUpdateRequest> = PatchValue.Unset,
    val bookMetadata: PatchValue<BookMetadataConfigUpdateRequest> = PatchValue.Unset,
    val nameMatchingMode: PatchValue<KomfNameMatchingMode> = PatchValue.Unset,
    val mediaType: PatchValue<KomfMediaType> = PatchValue.Unset,

    val authorRoles: PatchValue<Collection<KomfAuthorRole>> = PatchValue.Unset,
    val artistRoles: PatchValue<Collection<KomfAuthorRole>> = PatchValue.Unset,

    val coverLanguages: PatchValue<List<String>> = PatchValue.Unset,
)

@Serializable
class SeriesMetadataConfigUpdateRequest(
    val status: PatchValue<Boolean> = PatchValue.Unset,
    val title: PatchValue<Boolean> = PatchValue.Unset,
    val summary: PatchValue<Boolean> = PatchValue.Unset,
    val publisher: PatchValue<Boolean> = PatchValue.Unset,
    val readingDirection: PatchValue<Boolean> = PatchValue.Unset,
    val ageRating: PatchValue<Boolean> = PatchValue.Unset,
    val language: PatchValue<Boolean> = PatchValue.Unset,
    val genres: PatchValue<Boolean> = PatchValue.Unset,
    val tags: PatchValue<Boolean> = PatchValue.Unset,
    val totalBookCount: PatchValue<Boolean> = PatchValue.Unset,
    val authors: PatchValue<Boolean> = PatchValue.Unset,
    val releaseDate: PatchValue<Boolean> = PatchValue.Unset,
    val thumbnail: PatchValue<Boolean> = PatchValue.Unset,
    val links: PatchValue<Boolean> = PatchValue.Unset,
    val books: PatchValue<Boolean> = PatchValue.Unset,
    val useOriginalPublisher: PatchValue<Boolean> = PatchValue.Unset,

    val originalPublisherTagName: PatchValue<String> = PatchValue.Unset,
    val englishPublisherTagName: PatchValue<String> = PatchValue.Unset,
    val frenchPublisherTagName: PatchValue<String> = PatchValue.Unset,
)

@Serializable
data class BookMetadataConfigUpdateRequest(
    val title: PatchValue<Boolean> = PatchValue.Unset,
    val summary: PatchValue<Boolean> = PatchValue.Unset,
    val number: PatchValue<Boolean> = PatchValue.Unset,
    val numberSort: PatchValue<Boolean> = PatchValue.Unset,
    val releaseDate: PatchValue<Boolean> = PatchValue.Unset,
    val authors: PatchValue<Boolean> = PatchValue.Unset,
    val tags: PatchValue<Boolean> = PatchValue.Unset,
    val isbn: PatchValue<Boolean> = PatchValue.Unset,
    val links: PatchValue<Boolean> = PatchValue.Unset,
    val thumbnail: PatchValue<Boolean> = PatchValue.Unset,
)

@Serializable
data class NotificationConfigUpdateRequest(
    val apprise: PatchValue<AppriseConfigUpdateRequest> = PatchValue.Unset,
    val discord: PatchValue<DiscordConfigUpdateRequest> = PatchValue.Unset,
)

@Serializable
class DiscordConfigUpdateRequest(
    val webhooks: PatchValue<Map<Int, String?>> = PatchValue.Unset,
    val seriesCover: PatchValue<Boolean> = PatchValue.Unset,
)

@Serializable
class AppriseConfigUpdateRequest(
    val urls: PatchValue<Map<Int, String?>> = PatchValue.Unset,
)


