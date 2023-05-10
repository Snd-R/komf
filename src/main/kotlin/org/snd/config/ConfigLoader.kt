package org.snd.config

import com.charleskorn.kaml.Yaml
import mu.KotlinLogging
import org.snd.metadata.model.metadata.TitleType
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
        val discordConfig = config.discord
        val discordTemplatesDirectory = configDirectory ?: discordConfig.templatesDirectory
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
        val malClientId = System.getenv("KOMF_METADATA_PROVIDERS_MAL_CLIENT_ID")?.ifBlank { null }?.toString()
            ?: metadataProvidersConfig.malClientId
        val comicVineApiKey = System.getenv("KOMF_METADATA_PROVIDERS_COMIC_VINE_API_KEY")?.ifBlank { null }?.toString()
            ?: metadataProvidersConfig.comicVineApiKey

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
                comicVineApiKey = comicVineApiKey
            ),
            discord = config.discord.copy(
                templatesDirectory = discordTemplatesDirectory,
                webhooks = discordWebhooks
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
            komga = config.komga.copy(
                metadataUpdate = overrideDeprecatedOptions(config.komga.metadataUpdate, config.komga.aggregateMetadata),
            ),
            kavita = config.kavita.copy(
                metadataUpdate = overrideDeprecatedOptions(config.kavita.metadataUpdate, config.kavita.aggregateMetadata)
            ),
            metadataProviders = config.metadataProviders.copy(
                malClientId = mal?.clientId ?: config.metadataProviders.malClientId,
                defaultProviders = defaultProviders.copy(
                    mangaUpdates = mangaUpdates ?: defaultProviders.mangaUpdates,
                    mal = mal ?: defaultProviders.mal,
                    nautiljon = nautiljon ?: defaultProviders.nautiljon,
                    aniList = aniList?.let {
                        AniListConfig(
                            priority = it.priority,
                            enabled = it.enabled,
                            seriesMetadata = it.seriesMetadata,
                            nameMatchingMode = it.nameMatchingMode,
                            mediaType = it.mediaType,
                            authorRoles = it.authorRoles,
                            artistRoles = it.artistRoles,
                        )
                    } ?: defaultProviders.aniList,
                    yenPress = yenPress ?: defaultProviders.yenPress,
                    kodansha = kodansha ?: defaultProviders.kodansha,
                    viz = viz ?: defaultProviders.viz,
                    bookWalker = bookWalker ?: defaultProviders.bookWalker,
                )
            ),
        )
    }

    @Suppress("DEPRECATION")
    private fun overrideDeprecatedOptions(config: MetadataUpdateConfig, aggregate: Boolean?): MetadataUpdateConfig {
        return config.copy(
            default = config.default.copy(
                aggregate = aggregate ?: config.default.aggregate,
                bookCovers = config.bookThumbnails ?: config.default.bookCovers,
                seriesCovers = config.seriesThumbnails ?: config.default.seriesCovers,
                updateModes = config.modes ?: config.default.updateModes,
                postProcessing = config.default.postProcessing.copy(
                    seriesTitle = config.seriesTitle ?: config.default.postProcessing.seriesTitle,
                    seriesTitleLanguage = when (config.default.postProcessing.titleType) {
                        TitleType.ROMAJI -> "ja-ro"
                        TitleType.LOCALIZED -> "en"
                        TitleType.NATIVE -> "ja"
                        null -> null
                    } ?: config.default.postProcessing.seriesTitleLanguage,
                    orderBooks = config.orderBooks ?: config.default.postProcessing.orderBooks,
                    readingDirectionValue = config.readingDirectionValue
                        ?: config.default.postProcessing.readingDirectionValue,
                    languageValue = config.languageValue
                        ?: config.default.postProcessing.languageValue,
                )
            ),
            library = config.library.map { (libraryId, libraryConfig) ->
                libraryId to libraryConfig.copy(
                    postProcessing = libraryConfig.postProcessing.copy(
                        seriesTitleLanguage = when (libraryConfig.postProcessing.titleType) {
                            TitleType.ROMAJI -> "ja-ro"
                            TitleType.LOCALIZED -> "en"
                            TitleType.NATIVE -> "ja"
                            null -> null
                        } ?: libraryConfig.postProcessing.seriesTitleLanguage
                    )
                )
            }.toMap()
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

            config.komga.metadataUpdate.bookThumbnails?.let { "komga.metadataUpdate.bookThumbnails" },
            config.komga.metadataUpdate.seriesThumbnails?.let { "komga.metadataUpdate.seriesThumbnails" },
            config.komga.metadataUpdate.seriesTitle?.let { "komga.metadataUpdate.seriesTitle" },
            config.komga.metadataUpdate.titleType?.let { "komga.metadataUpdate.titleType" },
            config.komga.metadataUpdate.readingDirectionValue?.let { "komga.metadataUpdate.readingDirectionValue" },
            config.komga.metadataUpdate.languageValue?.let { "komga.metadataUpdate.languageValue" },
            config.komga.metadataUpdate.orderBooks?.let { "komga.metadataUpdate.orderBooks" },
            config.komga.metadataUpdate.modes?.let { "komga.metadataUpdate.modes" },
            config.komga.metadataUpdate.default.postProcessing.titleType?.let { "komga.metadataUpdate.default.postProcessing.titleType" },
            config.komga.metadataUpdate.library.mapNotNull { (libraryId, libraryConfig) ->
                if (libraryConfig.postProcessing.titleType != null) "komga.metadataUpdate.library.$libraryId.postProcessing.titleType"
                else null
            }.joinToString().ifEmpty { null },

            config.kavita.metadataUpdate.bookThumbnails?.let { "kavita.metadataUpdate.bookThumbnails" },
            config.kavita.metadataUpdate.seriesThumbnails?.let { "kavita.metadataUpdate.seriesThumbnails" },
            config.kavita.metadataUpdate.seriesTitle?.let { "kavita.metadataUpdate.seriesTitle" },
            config.kavita.metadataUpdate.titleType?.let { "kavita.metadataUpdate.titleType" },
            config.kavita.metadataUpdate.readingDirectionValue?.let { "kavita.metadataUpdate.readingDirectionValue" },
            config.kavita.metadataUpdate.languageValue?.let { "kavita.metadataUpdate.languageValue" },
            config.kavita.metadataUpdate.orderBooks?.let { "kavita.metadataUpdate.orderBooks" },
            config.kavita.metadataUpdate.modes?.let { "kavita.metadataUpdate.modes" },
            config.kavita.metadataUpdate.default.postProcessing.titleType?.let { "kavita.metadataUpdate.default.postProcessing.titleType" },
            config.kavita.metadataUpdate.library.mapNotNull { (libraryId, libraryConfig) ->
                if (libraryConfig.postProcessing.titleType != null) "kavita.metadataUpdate.library.$libraryId.postProcessing.titleType"
                else null
            }.joinToString().ifEmpty { null },
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
            config.metadataProviders.defaultProviders.mangaDex.enabled.not() &&
            config.metadataProviders.defaultProviders.bangumi.enabled.not() &&
            config.metadataProviders.defaultProviders.comicVine.enabled.not() &&
            config.metadataProviders.libraryProviders.isEmpty()
        ) {
            logger.warn { "No metadata providers enabled. You will not be able to get new metadata" }
        }
    }
}
