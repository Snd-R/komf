package org.snd.config

import kotlinx.serialization.Serializable
import org.snd.mediaserver.model.UpdateMode
import org.snd.mediaserver.model.UpdateMode.API
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.MediaType.MANGA
import org.snd.metadata.model.metadata.ReadingDirection
import org.snd.metadata.model.metadata.TitleType

@Serializable
data class AppConfig(
    val komga: KomgaConfig = KomgaConfig(),
    val kavita: KavitaConfig = KavitaConfig(),
    val discord: DiscordConfig = DiscordConfig(),
    val calibre: CalibreConfig = CalibreConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val metadataProviders: MetadataProvidersConfig = MetadataProvidersConfig(),
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
    @Deprecated("moved to MetadataUpdateConfig")
    val aggregateMetadata: Boolean? = null,
)

@Serializable
data class KavitaConfig(
    val baseUri: String = "http://localhost:5000",
    val apiKey: String = "",
    val eventListener: EventListenerConfig = EventListenerConfig(enabled = false),
    val notifications: NotificationConfig = NotificationConfig(),
    val metadataUpdate: MetadataUpdateConfig = MetadataUpdateConfig(),
    @Deprecated("moved to MetadataUpdateConfig")
    val aggregateMetadata: Boolean? = null,
)

@Serializable
data class NotificationConfig(
    val libraries: Collection<String> = emptyList()
)

@Serializable
data class MetadataUpdateConfig(
    val default: MetadataProcessingConfig = MetadataProcessingConfig(),
    val library: Map<String, MetadataProcessingConfig> = emptyMap(),

    @Deprecated("moved to default processing config")
    val bookThumbnails: Boolean? = null,
    @Deprecated("moved to default processing config")
    val seriesThumbnails: Boolean? = null,
    @Deprecated("moved to default processing config")
    val seriesTitle: Boolean? = null,
    @Deprecated("moved to default processing config")
    val titleType: TitleType? = null,
    @Deprecated("moved to default processing config")
    val readingDirectionValue: ReadingDirection? = null,
    @Deprecated("moved to default processing config")
    val languageValue: String? = null,
    @Deprecated("moved to default processing config")
    val orderBooks: Boolean? = null,
    @Deprecated("moved to default processing config")
    val modes: Set<UpdateMode>? = null,
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
    val updateModes: Set<UpdateMode> = setOf(API),

    val postProcessing: MetadataPostProcessingConfig = MetadataPostProcessingConfig()
)

@Serializable
data class MetadataPostProcessingConfig(
    val seriesTitle: Boolean = false,
    val seriesTitleLanguage: String? = "en",
    @Deprecated("replaced with seriesTitleLanguage")
    val titleType: TitleType? = null,
    val alternativeSeriesTitles: Boolean = false,
    val alternativeSeriesTitleLanguages: List<String> = listOf("en", "ja", "ja-ro"),
    val orderBooks: Boolean = false,
    val scoreTag: Boolean = false,
    val readingDirectionValue: ReadingDirection? = null,
    val languageValue: String? = null,
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
data class DiscordConfig(
    val title: String? = null,
    val titleUrl: String? = null,
    val descriptionTemplate: String? = "discordWebhook.vm",
    val fieldTemplates: List<DiscordFieldTemplateConfig> = emptyList(),
    val footerTemplate: String? = null,
    val seriesCover: Boolean = false,
    val colorCode: String = "1F8B4C",
    val webhooks: Collection<String>? = null,
    val templatesDirectory: String = "./",

    @Deprecated("no longer used")
    val imgurClientId: String? = null,
)

@Serializable
data class DiscordFieldTemplateConfig(
    val name: String,
    val templateName: String,
    val inline: Boolean = false
)

@Serializable
data class CalibreConfig(
    val ebookMetaPath: String? = null,
)

@Serializable
data class ServerConfig(
    val port: Int = 8085
)