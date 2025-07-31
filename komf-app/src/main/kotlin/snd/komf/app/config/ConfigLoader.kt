package snd.komf.app.config

import com.charleskorn.kaml.Yaml
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isReadable

private val logger = KotlinLogging.logger {}

class ConfigLoader(private val yaml: Yaml) {

    fun loadDirectory(directory: Path): AppConfig {
        val path = directory.resolve("application.yml")
        val config = yaml.decodeFromString(AppConfig.serializer(), Files.readString(path))
        return postProcessConfig(config, directory)
    }

    fun loadFile(file: Path): AppConfig {
        val config = yaml.decodeFromString(AppConfig.serializer(), Files.readString(file.toRealPath()))
        return postProcessConfig(config, null)
    }

    fun default(): AppConfig {
        val filePath = Path.of(".").toAbsolutePath().normalize().resolve("application.yml")
        return if (filePath.isReadable()) {
            val config = yaml.decodeFromString(AppConfig.serializer(), Files.readString(filePath.toRealPath()))
            postProcessConfig(config, null)
        } else {
            postProcessConfig(AppConfig(), null)
        }
    }

    private fun postProcessConfig(config: AppConfig, configDirectory: Path?): AppConfig {
        val processedConfig = overrideConfigDirAndEnvVars(config, configDirectory?.toString())

        warnAboutDisabledProviders(processedConfig)
        return processedConfig
    }

    private fun overrideConfigDirAndEnvVars(config: AppConfig, configDirectory: String?): AppConfig {
        val databaseConfig = config.database
        val databaseFile = configDirectory?.let { "$it/database.sqlite" } ?: databaseConfig.file
        val notificationConfig = config.notifications
        val appriseConfig = config.notifications.apprise
        val discordConfig = config.notifications.discord
        val templatesDirectory = configDirectory ?: notificationConfig.templatesDirectory
        val mangaBakaDirectory = configDirectory?.let { "$it/mangabaka" }
            ?: config.metadataProviders.mangabakaDatabaseDir

        val appriseUrls = System.getenv("KOMF_APPRISE_URLS")?.ifBlank { null }
            ?.split(",")?.toList()
            ?: appriseConfig.urls
        val discordWebhooks = System.getenv("KOMF_DISCORD_WEBHOOKS")?.ifBlank { null }
            ?.split(",")?.toList()
            ?: discordConfig.webhooks

        val komgaConfig = config.komga
        val komgaBaseUri = System.getenv("KOMF_KOMGA_BASE_URI")?.ifBlank { null } ?: komgaConfig.baseUri
        val komgaUser = System.getenv("KOMF_KOMGA_USER")?.ifBlank { null } ?: komgaConfig.komgaUser
        val komgaPassword = System.getenv("KOMF_KOMGA_PASSWORD")?.ifBlank { null } ?: komgaConfig.komgaPassword

        val kavitaConfig = config.kavita
        val kavitaBaseUri = System.getenv("KOMF_KAVITA_BASE_URI")?.ifBlank { null } ?: kavitaConfig.baseUri
        val kavitaApiKey = System.getenv("KOMF_KAVITA_API_KEY")?.ifBlank { null } ?: kavitaConfig.apiKey

        val serverConfig = config.server
        val serverPort = System.getenv("KOMF_SERVER_PORT")?.toIntOrNull() ?: serverConfig.port
        val logLevel = System.getenv("KOMF_LOG_LEVEL")?.ifBlank { null } ?: config.logLevel

        val metadataProvidersConfig = config.metadataProviders
        val malClientId = System.getenv("KOMF_METADATA_PROVIDERS_MAL_CLIENT_ID")?.ifBlank { null }
            ?: metadataProvidersConfig.malClientId
        val comicVineApiKey = System.getenv("KOMF_METADATA_PROVIDERS_COMIC_VINE_API_KEY")?.ifBlank { null }
            ?: metadataProvidersConfig.comicVineApiKey
        val comicVineSearchLimit = System.getenv("KOMF_METADATA_PROVIDERS_COMIC_VINE_SEARCH_LIMIT")?.ifBlank { null }
            ?: metadataProvidersConfig.comicVineSearchLimit
        val bangumiToken = System.getenv("KOMF_METADATA_PROVIDERS_BANGUMI_TOKEN")?.ifBlank { null }
            ?: metadataProvidersConfig.bangumiToken

        return config.copy(
            komga = komgaConfig.copy(
                baseUri = komgaBaseUri,
                komgaUser = komgaUser,
                komgaPassword = komgaPassword
            ),
            kavita = kavitaConfig.copy(
                baseUri = kavitaBaseUri,
                apiKey = kavitaApiKey,
            ),

            database = databaseConfig.copy(
                file = databaseFile
            ),
            metadataProviders = metadataProvidersConfig.copy(
                malClientId = malClientId,
                comicVineApiKey = comicVineApiKey,
                bangumiToken = bangumiToken,
                mangabakaDatabaseDir = mangaBakaDirectory
            ),
            notifications = config.notifications.copy(
                templatesDirectory = templatesDirectory,
                apprise = config.notifications.apprise.copy(
                    urls = appriseUrls
                ),
                discord = config.notifications.discord.copy(
                    webhooks = discordWebhooks
                )
            ),
            server = serverConfig.copy(port = serverPort),
            logLevel = logLevel
        )
    }

    private fun warnAboutDisabledProviders(config: AppConfig) {
        if (
            config.metadataProviders.defaultProviders.mangaUpdates.enabled.not() &&
            config.metadataProviders.defaultProviders.mal.enabled.not() &&
            config.metadataProviders.defaultProviders.nautiljon.enabled.not() &&
            config.metadataProviders.defaultProviders.aniList.enabled.not() &&
            config.metadataProviders.defaultProviders.yenPress.enabled.not() &&
            config.metadataProviders.defaultProviders.kodansha.enabled.not() &&
            config.metadataProviders.defaultProviders.viz.enabled.not() &&
            config.metadataProviders.defaultProviders.bookWalker.enabled.not() &&
            config.metadataProviders.defaultProviders.mangaDex.enabled.not() &&
            config.metadataProviders.defaultProviders.bangumi.enabled.not() &&
            config.metadataProviders.defaultProviders.comicVine.enabled.not() &&
            config.metadataProviders.defaultProviders.hentag.enabled.not() &&
            config.metadataProviders.defaultProviders.mangaBaka.enabled.not() &&
            config.metadataProviders.defaultProviders.webtoons.enabled.not() &&
            config.metadataProviders.libraryProviders.isEmpty()
        ) {
            logger.warn { "No metadata providers enabled. You will not be able to get new metadata" }
        }
    }
}
