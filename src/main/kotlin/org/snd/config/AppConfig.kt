package org.snd.config

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val metadataProviders: MetadataProvidersConfig,
    val komga: KomgaConfig,
    val database: DatabaseConfig,
    val server: ServerConfig,
    val logLevel: String
)

@Serializable
data class KomgaConfig(
    val baseUri: String,
    val komgaUser: String,
    val komgaPassword: String,
    val eventListener: KomgaEventListenerConfig
)

@Serializable
data class DatabaseConfig(val file: String)

@Serializable
data class KomgaEventListenerConfig(
    val enabled: Boolean,
    val libraries: Collection<String>
)

@Serializable
data class MetadataProvidersConfig(
    val mal: MalConfig,
    val mangaUpdates: MangaUpdatesConfig
)


@Serializable
data class MalConfig(
    val clientId: String,
    val priority: Int,
    val enabled: Boolean,
)

@Serializable
data class MangaUpdatesConfig(
    val priority: Int,
    val enabled: Boolean,
)

@Serializable
data class ServerConfig(
    val port: Int = 8085
)
