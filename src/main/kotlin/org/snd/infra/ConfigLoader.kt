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
        val defaultProviders = config.metadataProviders.defaultProviders
        val mangaUpdates = config.metadataProviders.mangaUpdates
        val mal = config.metadataProviders.mal
        val nautiljon = config.metadataProviders.nautiljon
        val aniList = config.metadataProviders.aniList
        val yenPress = config.metadataProviders.yenPress
        val kodansha = config.metadataProviders.kodansha
        val viz = config.metadataProviders.viz
        val bookWalker = config.metadataProviders.bookWalker

        warnAboutDeprecatedOptions(config)

        return config.copy(
            metadataProviders = config.metadataProviders.copy(
                defaultProviders = defaultProviders.copy(
                    mangaUpdates = mangaUpdates ?: defaultProviders.mangaUpdates,
                    mal = mal ?: defaultProviders.mal,
                    nautiljon = nautiljon ?: defaultProviders.nautiljon,
                    aniList = aniList ?: defaultProviders.aniList,
                    yenPress = yenPress ?: defaultProviders.yenPress,
                    kodansha = kodansha ?: defaultProviders.kodansha,
                    viz = viz ?: defaultProviders.viz,
                    bookWalker = bookWalker ?: defaultProviders.bookWalker,
                )
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun warnAboutDeprecatedOptions(config: AppConfig) {
        val deprecatedOptions = listOfNotNull(
            config.metadataProviders.mangaUpdates?.let { "metadataProviders.mangaUpdates" },
            config.metadataProviders.mal?.let { "metadataProviders.mal" },
            config.metadataProviders.nautiljon?.let { "metadataProviders.nautiljon" },
            config.metadataProviders.aniList?.let { "metadataProviders.aniList" },
            config.metadataProviders.yenPress?.let { "metadataProviders.yenPress" },
            config.metadataProviders.kodansha?.let { "metadataProviders.kodansha" },
            config.metadataProviders.viz?.let { "metadataProviders.viz" },
            config.metadataProviders.bookWalker?.let { "metadataProviders.bookWalker" },
        )
        if (deprecatedOptions.isNotEmpty()) {
            logger.warn {
                "DETECTED DEPRECATED CONFIG OPTIONS $deprecatedOptions\n" +
                        "These config options will be removed in future releases"
            }
        }
    }
}
