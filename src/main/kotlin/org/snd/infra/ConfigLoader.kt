package org.snd.infra

import com.charleskorn.kaml.Yaml
import mu.KotlinLogging
import org.snd.config.AppConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isReadable

private val logger = KotlinLogging.logger {}

class ConfigLoader {

    fun loadConfig(configPath: Path?, configDirectory: Path?): AppConfig {
        val configRaw = loadFromDirectory(configDirectory) ?: loadFromFile(configPath) ?: loadDefault()

        val config = configRaw?.let { Yaml.default.decodeFromString(AppConfig.serializer(), it) }
            ?: AppConfig()

        return checkDeprecatedOptions(
            overrideConfigDirAndEnvVars(config, configDirectory?.toString())
        )
    }

    private fun loadFromDirectory(configDirectory: Path?): String? {
        return configDirectory?.let {
            val path = configDirectory.resolve("application.yml")
            if (path.isReadable()) Files.readString(path)
            else null
        }
    }

    private fun loadFromFile(path: Path?): String? {
        return path?.let {
            Files.readString(it.toRealPath())
        }
    }

    private fun loadDefault(): String? {
        return AppConfig::class.java.getResource("/application.yml")?.readText()
    }

    private fun overrideConfigDirAndEnvVars(config: AppConfig, configDirectory: String?): AppConfig {
        val databaseConfig = config.database
        val databaseFile = configDirectory?.let { "$it/database.sqlite" } ?: databaseConfig.file
        val komgaConfig = config.komga
        val komgaBaseUri = System.getenv("KOMF_KOMGA_BASE_URI") ?: komgaConfig.baseUri
        val komgaUser = System.getenv("KOMF_KOMGA_USER") ?: komgaConfig.komgaUser
        val komgaPassword = System.getenv("KOMF_KOMGA_PASSWORD") ?: komgaConfig.komgaPassword
        val discordTemplatesDirectory = configDirectory ?: config.discord.templatesDirectory


        val kavitaConfig = config.kavita
        val kavitaBaseUri = System.getenv("KOMF_KAVITA_BASE_URI") ?: kavitaConfig.baseUri
        val kavitaApiKey = System.getenv("KOMF_KAVITA_API_KEY") ?: kavitaConfig.apiKey

        val serverConfig = config.server
        val serverPort = System.getenv("KOMF_SERVER_PORT")?.toInt() ?: serverConfig.port
        val logLevel = System.getenv("KOMF_LOG_LEVEL") ?: config.logLevel

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
            discord = config.discord.copy(
                templatesDirectory = discordTemplatesDirectory
            ),
            server = serverConfig.copy(port = serverPort),
            logLevel = logLevel
        )
    }

    @Suppress("DEPRECATION")
    private fun checkDeprecatedOptions(config: AppConfig): AppConfig {
        val discordConfig = config.discord
        val discordWebhooks = discordConfig.webhooks ?: config.komga.webhooks

        val nautiljon = config.metadataProviders.nautiljon
        val nautiljonSeriesMetadataConfig = with(nautiljon.seriesMetadata) {
            this.copy(
                useOriginalPublisher = nautiljon.useOriginalPublisher ?: useOriginalPublisher,
                originalPublisherTagName = nautiljon.originalPublisherTag ?: originalPublisherTagName,
                frenchPublisherTagName = nautiljon.frenchPublisherTag ?: frenchPublisherTagName
            )
        }
        val komgaUpdateModes = config.komga.metadataUpdate.mode?.let { setOf(it) } ?: config.komga.metadataUpdate.modes
        warnAboutDeprecatedOptions(config)

        return config.copy(
            discord = discordConfig.copy(
                webhooks = discordWebhooks
            ),
            metadataProviders = config.metadataProviders.copy(
                nautiljon = nautiljon.copy(
                    seriesMetadata = nautiljonSeriesMetadataConfig
                )
            ),
            komga = config.komga.copy(
                metadataUpdate = config.komga.metadataUpdate.copy(
                    modes = komgaUpdateModes
                )
            )
        )
    }

    @Suppress("DEPRECATION")
    private fun warnAboutDeprecatedOptions(config: AppConfig) {
        val deprecatedOptions = listOfNotNull(
            config.komga.webhooks?.let { "komga.webhooks" },
            config.metadataProviders.nautiljon.fetchBookMetadata?.let { "metadataProviders.nautiljon.fetchBookMetadata" },
            config.metadataProviders.nautiljon.useOriginalPublisher?.let { "metadataProviders.nautiljon.useOriginalPublisher" },
            config.metadataProviders.nautiljon.originalPublisherTag?.let { "metadataProviders.nautiljon.originalPublisherTag" },
            config.metadataProviders.nautiljon.frenchPublisherTag?.let { "metadataProviders.nautiljon.frenchPublisherTag" },
            config.komga.metadataUpdate.mode?.let { "komga.metadataUpdate.mode" },
            config.kavita.metadataUpdate.mode?.let { "kavita.metadataUpdate.mode" },
        )
        if (deprecatedOptions.isNotEmpty()) {
            logger.warn {
                "DETECTED DEPRECATED CONFIG OPTIONS $deprecatedOptions\n" +
                        "These config options will eventually be deleted in future releases"
            }
        }
    }
}
