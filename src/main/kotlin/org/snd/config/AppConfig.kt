package org.snd.config

import kotlinx.serialization.Serializable
import org.snd.mediaserver.UpdateMode
import org.snd.mediaserver.UpdateMode.API
import org.snd.metadata.NameMatchingMode
import org.snd.metadata.NameMatchingMode.CLOSEST_MATCH
import org.snd.metadata.model.ReadingDirection

@Serializable
data class AppConfig(
    val metadataProviders: MetadataProvidersConfig = MetadataProvidersConfig(),
    val komga: KomgaConfig = KomgaConfig(),
    val kavita: KavitaConfig = KavitaConfig(),
    val discord: DiscordConfig = DiscordConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val server: ServerConfig = ServerConfig(),
    val logLevel: String = "INFO"
)

@Serializable
data class KomgaConfig(
    val baseUri: String = "http://localhost:8080",
    val komgaUser: String = "admin@example.org",
    val komgaPassword: String = "admin",
    val eventListener: EventListenerConfig = EventListenerConfig(),
    val notifications: NotificationConfig = NotificationConfig(),
    val metadataUpdate: MetadataUpdateConfig = MetadataUpdateConfig(),
    val aggregateMetadata: Boolean = false,
)

@Serializable
data class KavitaConfig(
    val baseUri: String = "http://localhost:5000",
    val apiKey: String = "",
    val eventListener: EventListenerConfig = EventListenerConfig(enabled = false),
    val notifications: NotificationConfig = NotificationConfig(),
    val metadataUpdate: MetadataUpdateConfig = MetadataUpdateConfig(),
    val aggregateMetadata: Boolean = false,
)

@Serializable
data class NotificationConfig(
    val libraries: Collection<String> = emptyList()
)

@Serializable
data class MetadataUpdateConfig(
    val modes: Set<UpdateMode> = setOf(API),
    val bookThumbnails: Boolean = false,
    val seriesThumbnails: Boolean = true,
    val seriesTitle: Boolean = false,
    val readingDirectionValue: ReadingDirection? = null
)

@Serializable
data class DatabaseConfig(
    val file: String = "./database.sqlite"
)

@Serializable
data class EventListenerConfig(
    val enabled: Boolean = false,
    val libraries: Collection<String> = emptyList()
)

@Serializable
data class MetadataProvidersConfig(
    val malClientId: String = "",
    val nameMatchingMode: NameMatchingMode = CLOSEST_MATCH,
    val defaultProviders: ProvidersConfig = ProvidersConfig(),
    val libraryProviders: Map<String, ProvidersConfig> = emptyMap(),

    @Deprecated("moved to default providers config")
    val mangaUpdates: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val mal: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val nautiljon: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val aniList: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val yenPress: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val kodansha: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val viz: ProviderConfig? = null,

    @Deprecated("moved to default providers config")
    val bookWalker: ProviderConfig? = null,
)

@Serializable
data class ProvidersConfig(
    val mangaUpdates: ProviderConfig = ProviderConfig(),
    val mal: ProviderConfig = ProviderConfig(),
    val nautiljon: ProviderConfig = ProviderConfig(),
    val aniList: ProviderConfig = ProviderConfig(),
    val yenPress: ProviderConfig = ProviderConfig(),
    val kodansha: ProviderConfig = ProviderConfig(),
    val viz: ProviderConfig = ProviderConfig(),
    val bookWalker: ProviderConfig = ProviderConfig(),
)

@Serializable
data class ProviderConfig(
    @Deprecated("moved to separate config")
    val clientId: String = "",
    val priority: Int = 10,
    val enabled: Boolean = false,
    val seriesMetadata: SeriesMetadataConfig = SeriesMetadataConfig(),
    val bookMetadata: BookMetadataConfig = BookMetadataConfig(),
    val nameMatchingMode: NameMatchingMode? = null,
)

@Serializable
data class DiscordConfig(
    val webhooks: Collection<String>? = null,
    val templatesDirectory: String = "./",
)

@Serializable
data class ServerConfig(
    val port: Int = 8085
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

    val useOriginalPublisher: Boolean = false,
    val originalPublisherTagName: String? = null,
    val englishPublisherTagName: String? = null,
    val frenchPublisherTagName: String? = null,
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
