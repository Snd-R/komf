package org.snd.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val metadataProviders: MetadataProvidersConfig = MetadataProvidersConfig(),
    val komga: KomgaConfig = KomgaConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val server: ServerConfig = ServerConfig(),
    val logLevel: String = "INFO"
)

@Serializable
data class KomgaConfig(
    val baseUri: String = "http://localhost:8080",
    val komgaUser: String = "admin@example.org",
    val komgaPassword: String = "admin",
    val eventListener: KomgaEventListenerConfig = KomgaEventListenerConfig(),
    val metadataUpdate: MetadataUpdateConfig = MetadataUpdateConfig()
)

@Serializable
data class MetadataUpdateConfig(
    val bookThumbnails: Boolean = false,
    val seriesThumbnails: Boolean = true
)

@Serializable
data class DatabaseConfig(
    val file: String = "./database.sqlite"
)

@Serializable
data class KomgaEventListenerConfig(
    val enabled: Boolean = true,
    val libraries: Collection<String> = emptyList()
)

@Serializable
data class MetadataProvidersConfig(
    val mal: MalConfig = MalConfig(),
    val mangaUpdates: MangaUpdatesConfig = MangaUpdatesConfig(),
    val nautiljon: NautiljonConfig = NautiljonConfig()
)

@Serializable
data class MangaUpdatesConfig(
    val priority: Int = 10,
    val enabled: Boolean = true,
)

@Serializable
data class MalConfig(
    val clientId: String = "",
    val priority: Int = 20,
    val enabled: Boolean = false,
)

@Serializable
data class NautiljonConfig(
    val priority: Int = 30,
    val enabled: Boolean = false,
    val fetchBookMetadata: Boolean = false,
    val useOriginalPublisher: Boolean = false,
    val originalPublisherTag: String? = null,
    val frenchPublisherTag: String? = null
)

@Serializable
data class ServerConfig(
    val port: Int = 8085
)
