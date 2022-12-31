package org.snd.config

import com.charleskorn.kaml.Yaml
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isReadable

private val logger = KotlinLogging.logger {}

class ConfigLoader {
    fun loadDirectory(directory: Path): AppConfig {
        val path = directory.resolve("application.yml")
        val config = Yaml.default.decodeFromString(AppConfig.serializer(), Files.readString(path))
        return postProcessConfig(config, directory)
    }

    fun loadFile(file: Path): AppConfig {
        val config = Yaml.default.decodeFromString(AppConfig.serializer(), Files.readString(file.toRealPath()))
        return postProcessConfig(config, null)
    }

    fun default(): AppConfig {
        val filePath = Path.of(".").toAbsolutePath().normalize().resolve("application.yml")
        return if (filePath.isReadable()) {
            val config = Yaml.default.decodeFromString(AppConfig.serializer(), Files.readString(filePath.toRealPath()))
            postProcessConfig(config, null)
        } else {
            postProcessConfig(AppConfig(), null)
        }
    }

    private fun postProcessConfig(config: AppConfig, configDirectory: Path?): AppConfig {
        val processedConfig = overrideDeprecatedOptions(
            overrideConfigDirAndEnvVars(config, configDirectory?.toString())
        )
        warnAboutDisabledProviders(processedConfig)
        return processedConfig
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
    private fun overrideDeprecatedOptions(config: AppConfig): AppConfig {
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
                malClientId = mal?.clientId ?: config.metadataProviders.malClientId,
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
            config.metadataProviders.libraryProviders.isEmpty()
        ) {
            logger.warn { "No metadata providers enabled. You will not be able to get new metadata" }
        }
    }
}
