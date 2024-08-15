package snd.komf.app.config

import kotlinx.serialization.Serializable
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import snd.komf.mediaserver.metadata.PublisherTagNameConfig
import snd.komf.model.MediaType
import snd.komf.model.MediaType.MANGA
import snd.komf.model.ReadingDirection
import snd.komf.model.UpdateMode
import snd.komf.model.UpdateMode.API
import snd.komf.notifications.discord.DiscordConfig
import snd.komf.providers.MetadataProvidersConfig

@Serializable
data class AppConfig(
    val komga: KomgaConfig = KomgaConfig(),
    val kavita: KavitaConfig = KavitaConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val metadataProviders: MetadataProvidersConfig = MetadataProvidersConfig(),
    val notifications: NotificationsConfig = NotificationsConfig(),

    @Deprecated("moved to NotificationsConfig object")
    val discord: DiscordConfig? = null,
    val server: ServerConfig = ServerConfig(),
    val logLevel: String = "INFO",
    val httpLogLevel: HttpLoggingInterceptor.Level = BASIC
)

@Serializable
data class KomgaConfig(
    val baseUri: String = "http://localhost:25600",
    val komgaUser: String = "admin@example.org",
    val komgaPassword: String = "admin",
    val thumbnailSizeLimit: Long = 1048575,
    val eventListener: EventListenerConfig = EventListenerConfig(),

    @Deprecated("Moved to EventListenerConfig object")
    val notifications: NotificationConfig = NotificationConfig(),
    val metadataUpdate: MetadataUpdateConfig = MetadataUpdateConfig(),
)

@Serializable
data class KavitaConfig(
    val baseUri: String = "http://localhost:5000",
    val apiKey: String = "",
    val eventListener: EventListenerConfig = EventListenerConfig(enabled = false),

    @Deprecated("Moved to EventListenerConfig object")
    val notifications: NotificationConfig = NotificationConfig(),
    val metadataUpdate: MetadataUpdateConfig = MetadataUpdateConfig(),
)

@Serializable
@Deprecated("Moved to EventListenerConfig object")
data class NotificationConfig(
    val libraries: Collection<String>? = null
)

@Serializable
data class MetadataUpdateConfig(
    val default: MetadataProcessingConfig = MetadataProcessingConfig(),
    val library: Map<String, MetadataProcessingConfig> = emptyMap(),
)

@Serializable
data class MetadataProcessingConfig(
    val libraryType: MediaType = MANGA,
    val aggregate: Boolean = false,
    val mergeTags: Boolean = false,
    val mergeGenres: Boolean = false,

    val bookCovers: Boolean = false,
    val seriesCovers: Boolean = false,
    val overrideExistingCovers: Boolean = true,
    val updateModes: List<UpdateMode> = listOf(API),
    val overrideComicInfo: Boolean = false,

    val postProcessing: MetadataPostProcessingConfig = MetadataPostProcessingConfig()
)

@Serializable
data class MetadataPostProcessingConfig(
    val seriesTitle: Boolean = false,
    val seriesTitleLanguage: String? = "en",
    val alternativeSeriesTitles: Boolean = false,
    val alternativeSeriesTitleLanguages: List<String> = listOf("en", "ja", "ja-ro"),
    val fallbackToAltTitle: Boolean = false,

    val orderBooks: Boolean = false,
    val readingDirectionValue: ReadingDirection? = null,
    val languageValue: String? = null,

    @Deprecated("use scoreTagName instead")
    val scoreTag: Boolean = false,
    val scoreTagName: String? = null,
    val originalPublisherTagName: String? = null,
    val publisherTagNames: List<PublisherTagNameConfig> = emptyList(),
)

@Serializable
data class DatabaseConfig(
    val file: String = "./database.sqlite"
)

@Serializable
data class EventListenerConfig(
    val enabled: Boolean = false,
    val metadataLibraryFilter: Collection<String> = emptyList(),
    val metadataSeriesExcludeFilter: Collection<String> = emptySet(),
    val notificationsLibraryFilter: Collection<String> = emptyList(),

    @Deprecated("Renamed to metadataLibraryFilter")
    val libraries: Collection<String>? = null,
    @Deprecated("Renamed to metadataSeriesExcludeFilter")
    val excludeSeries: Collection<String>? = null
)

@Serializable
data class NotificationsConfig(
    val discord: DiscordConfig = DiscordConfig(),
)

@Serializable
data class ServerConfig(
    val port: Int = 8085
)