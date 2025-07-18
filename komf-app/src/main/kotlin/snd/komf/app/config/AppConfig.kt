package snd.komf.app.config

import kotlinx.serialization.Serializable
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import snd.komf.mediaserver.config.DatabaseConfig
import snd.komf.mediaserver.config.KavitaConfig
import snd.komf.mediaserver.config.KomgaConfig
import snd.komf.notifications.NotificationsConfig
import snd.komf.providers.MetadataProvidersConfig

@Serializable
data class AppConfig(
    val komga: KomgaConfig = KomgaConfig(),
    val kavita: KavitaConfig = KavitaConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val metadataProviders: MetadataProvidersConfig = MetadataProvidersConfig(),
    val notifications: NotificationsConfig = NotificationsConfig(),
    val server: ServerConfig = ServerConfig(),
    val logLevel: String = "INFO",
    val httpLogLevel: HttpLoggingInterceptor.Level = BASIC
)

@Serializable
data class ServerConfig(
    val port: Int = 8085
)