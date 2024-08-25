package snd.komf.app.api.deprecated

import snd.komf.app.api.deprecated.dto.AniListConfigDto
import snd.komf.app.api.deprecated.dto.AniListConfigUpdateDto
import snd.komf.app.api.deprecated.dto.AppConfigDto
import snd.komf.app.api.deprecated.dto.AppConfigUpdateDto
import snd.komf.app.api.deprecated.dto.BookMetadataConfigDto
import snd.komf.app.api.deprecated.dto.BookMetadataConfigUpdateDto
import snd.komf.app.api.deprecated.dto.DiscordConfigDto
import snd.komf.app.api.deprecated.dto.DiscordConfigUpdateDto
import snd.komf.app.api.deprecated.dto.EventListenerConfigDto
import snd.komf.app.api.deprecated.dto.EventListenerConfigUpdateDto
import snd.komf.app.api.deprecated.dto.KavitaConfigDto
import snd.komf.app.api.deprecated.dto.KavitaConfigUpdateDto
import snd.komf.app.api.deprecated.dto.KomgaConfigDto
import snd.komf.app.api.deprecated.dto.KomgaConfigUpdateDto
import snd.komf.app.api.deprecated.dto.MetadataPostProcessingConfigDto
import snd.komf.app.api.deprecated.dto.MetadataPostProcessingConfigUpdateDto
import snd.komf.app.api.deprecated.dto.MetadataProcessingConfigDto
import snd.komf.app.api.deprecated.dto.MetadataProcessingConfigUpdateDto
import snd.komf.app.api.deprecated.dto.MetadataProvidersConfigDto
import snd.komf.app.api.deprecated.dto.MetadataProvidersConfigUpdateDto
import snd.komf.app.api.deprecated.dto.MetadataUpdateConfigDto
import snd.komf.app.api.deprecated.dto.MetadataUpdateConfigUpdateDto
import snd.komf.app.api.deprecated.dto.NotificationConfigDto
import snd.komf.app.api.deprecated.dto.NotificationConfigUpdateDto
import snd.komf.app.api.deprecated.dto.PatchValue
import snd.komf.app.api.deprecated.dto.ProviderConfigDto
import snd.komf.app.api.deprecated.dto.ProviderConfigUpdateDto
import snd.komf.app.api.deprecated.dto.ProvidersConfigDto
import snd.komf.app.api.deprecated.dto.ProvidersConfigUpdateDto
import snd.komf.app.api.deprecated.dto.SeriesMetadataConfigDto
import snd.komf.app.api.deprecated.dto.SeriesMetadataConfigUpdateDto
import snd.komf.app.config.AppConfig
import snd.komf.app.config.EventListenerConfig
import snd.komf.app.config.KavitaConfig
import snd.komf.app.config.KomgaConfig
import snd.komf.app.config.MetadataPostProcessingConfig
import snd.komf.app.config.MetadataProcessingConfig
import snd.komf.app.config.MetadataUpdateConfig
import snd.komf.app.config.NotificationsConfig
import snd.komf.notifications.discord.DiscordConfig
import snd.komf.providers.AniListConfig
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.MangaDexConfig
import snd.komf.providers.MetadataProvidersConfig
import snd.komf.providers.ProviderConfig
import snd.komf.providers.ProvidersConfig
import snd.komf.providers.SeriesMetadataConfig

class DeprecatedConfigUpdateMapper {
    private val maskedPlaceholder = "********"

    fun toDto(config: AppConfig): AppConfigDto {
        return AppConfigDto(
            metadataProviders = toDto(config.metadataProviders),
            komga = toDto(config.komga),
            kavita = toDto(config.kavita),
            discord = toDto(config.notifications.discord),
        )
    }

    fun patch(config: AppConfig, patch: AppConfigUpdateDto): AppConfig {
        return config.copy(
            metadataProviders = patch.metadataProviders
                ?.let { metadataProviders(config.metadataProviders, it) }
                ?: config.metadataProviders,
            komga = patch.komga?.let { komgaConfig(config.komga, it) } ?: config.komga,
            kavita = patch.kavita?.let { kavitaConfig(config.kavita, it) } ?: config.kavita,
            notifications = NotificationsConfig(
                discord = patch.discord
                    ?.let { discord(config.notifications.discord, it) }
                    ?: config.notifications.discord
            ),
        )
    }

    private fun toDto(config: KomgaConfig): KomgaConfigDto {
        return KomgaConfigDto(
            baseUri = config.baseUri,
            komgaUser = config.komgaUser,
            eventListener = toDto(config.eventListener),
            notifications = NotificationConfigDto(config.eventListener.notificationsLibraryFilter),
            metadataUpdate = toDto(config.metadataUpdate),
        )
    }

    private fun toDto(config: KavitaConfig): KavitaConfigDto {
        return KavitaConfigDto(
            baseUri = config.baseUri,
            eventListener = toDto(config.eventListener),
            notifications = NotificationConfigDto(config.eventListener.notificationsLibraryFilter),
            metadataUpdate = toDto(config.metadataUpdate),
        )
    }

    private fun toDto(config: EventListenerConfig): EventListenerConfigDto {
        return EventListenerConfigDto(
            enabled = config.enabled,
            libraries = config.metadataLibraryFilter
        )
    }

    private fun toDto(config: MetadataUpdateConfig): MetadataUpdateConfigDto {
        return MetadataUpdateConfigDto(
            default = toDto(config.default),
            library = config.library.map { (libraryId, config) -> libraryId to toDto(config) }.toMap()
        )
    }

    private fun toDto(config: MetadataProcessingConfig): MetadataProcessingConfigDto {
        return MetadataProcessingConfigDto(
            libraryType = config.libraryType,
            aggregate = config.aggregate,
            mergeTags = config.mergeTags,
            mergeGenres = config.mergeGenres,
            bookCovers = config.bookCovers,
            seriesCovers = config.seriesCovers,
            overrideExistingCovers = config.overrideExistingCovers,
            updateModes = config.updateModes,
            postProcessing = toDto(config.postProcessing),
        )
    }

    private fun toDto(config: MetadataPostProcessingConfig): MetadataPostProcessingConfigDto {
        return MetadataPostProcessingConfigDto(
            seriesTitle = config.seriesTitle,
            seriesTitleLanguage = config.seriesTitleLanguage,
            alternativeSeriesTitles = config.alternativeSeriesTitles,
            alternativeSeriesTitleLanguages = config.alternativeSeriesTitleLanguages,
            orderBooks = config.orderBooks,
            readingDirectionValue = config.readingDirectionValue,
            languageValue = config.languageValue,
            fallbackToAltTitle = config.fallbackToAltTitle
        )
    }

    private fun toDto(config: DiscordConfig): DiscordConfigDto {
        return DiscordConfigDto(
            webhooks = config.webhooks
                ?.map {
                    if (it.length < 110) maskedPlaceholder
                    else it.replace("(?<=.{34}).(?=.{10})".toRegex(), "*")
                }
                ?.mapIndexed { index, value -> index to value }
                ?.toMap(),
            seriesCover = config.seriesCover,
        )
    }

    private fun toDto(config: MetadataProvidersConfig): MetadataProvidersConfigDto {
        val malClientId = config.malClientId?.let { clientId ->
            if (clientId.length < 32) maskedPlaceholder
            else clientId.replace("(?<=.{4}).".toRegex(), "*")
        } ?: ""

        val comicVineClientId = config.comicVineApiKey?.let { apiKey ->
            if (apiKey.length < 40) maskedPlaceholder
            else apiKey.replace("(?<=.{4}).".toRegex(), "*")
        }

        return MetadataProvidersConfigDto(
            malClientId = malClientId,
            comicVineClientId = comicVineClientId,
            nameMatchingMode = config.nameMatchingMode,
            defaultProviders = toDto(config.defaultProviders),
            libraryProviders = config.libraryProviders
                .map { (libraryId, config) -> libraryId to toDto(config) }
                .toMap()
        )
    }

    private fun toDto(config: ProvidersConfig): ProvidersConfigDto {
        return ProvidersConfigDto(
            mangaUpdates = toDto(config.mangaUpdates),
            mal = toDto(config.mal),
            nautiljon = toDto(config.nautiljon),
            aniList = toDto(config.aniList),
            yenPress = toDto(config.yenPress),
            kodansha = toDto(config.kodansha),
            viz = toDto(config.viz),
            bookWalker = toDto(config.bookWalker),
            mangaDex = toDto(config.mangaDex),
            bangumi = toDto(config.bangumi),
            comicVine = toDto(config.comicVine),
        )
    }

    private fun toDto(config: ProviderConfig): ProviderConfigDto {
        return ProviderConfigDto(
            nameMatchingMode = config.nameMatchingMode,
            priority = config.priority,
            enabled = config.enabled,
            mediaType = config.mediaType,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
            seriesMetadata = toDto(config.seriesMetadata),
            bookMetadata = toDto(config.bookMetadata),
        )
    }

    private fun toDto(config: AniListConfig): AniListConfigDto {
        return AniListConfigDto(
            nameMatchingMode = config.nameMatchingMode,
            priority = config.priority,
            enabled = config.enabled,
            mediaType = config.mediaType,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
            seriesMetadata = toDto(config.seriesMetadata),
            tagsScoreThreshold = config.tagsScoreThreshold,
            tagsSizeLimit = config.tagsSizeLimit,

            bookMetadata = BookMetadataConfigDto(
                title = true,
                summary = true,
                number = true,
                numberSort = true,
                releaseDate = true,
                authors = true,
                tags = true,
                isbn = true,
                links = true,
                thumbnail = true
            ),
        )
    }

    private fun toDto(config: MangaDexConfig): ProviderConfigDto {
        return ProviderConfigDto(
            nameMatchingMode = config.nameMatchingMode,
            priority = config.priority,
            enabled = config.enabled,
            mediaType = config.mediaType,
            authorRoles = config.authorRoles,
            artistRoles = config.artistRoles,
            seriesMetadata = toDto(config.seriesMetadata),
            bookMetadata = BookMetadataConfigDto(
                title = true,
                summary = true,
                number = true,
                numberSort = true,
                releaseDate = true,
                authors = true,
                tags = true,
                isbn = true,
                links = true,
                thumbnail = true
            ),
        )
    }

    private fun toDto(config: SeriesMetadataConfig): SeriesMetadataConfigDto {
        return SeriesMetadataConfigDto(
            status = config.status,
            title = config.title,
            summary = config.summary,
            publisher = config.publisher,
            readingDirection = config.readingDirection,
            ageRating = config.ageRating,
            language = config.language,
            genres = config.genres,
            tags = config.tags,
            totalBookCount = config.totalBookCount,
            authors = config.authors,
            releaseDate = config.releaseDate,
            thumbnail = config.thumbnail,
            links = config.links,
            books = config.books,
            useOriginalPublisher = config.useOriginalPublisher,
            originalPublisherTagName = "",
            englishPublisherTagName = "",
            frenchPublisherTagName = "",
        )
    }

    private fun toDto(config: BookMetadataConfig): BookMetadataConfigDto {
        return BookMetadataConfigDto(
            title = config.title,
            summary = config.summary,
            number = config.number,
            numberSort = config.numberSort,
            releaseDate = config.releaseDate,
            authors = config.authors,
            tags = config.tags,
            isbn = config.isbn,
            links = config.links,
            thumbnail = config.thumbnail
        )
    }

    private fun metadataProviders(
        config: MetadataProvidersConfig,
        patch: MetadataProvidersConfigUpdateDto
    ): MetadataProvidersConfig {
        return config.copy(
            malClientId = patch.malClientId ?: config.malClientId,
            comicVineApiKey = when (val apiKey = patch.comicVineClientId) {
                PatchValue.None -> null
                is PatchValue.Some -> apiKey.value
                PatchValue.Unset -> config.comicVineApiKey
            },
            nameMatchingMode = patch.nameMatchingMode ?: config.nameMatchingMode,
            defaultProviders = patch.defaultProviders
                ?.let { providersConfig(config.defaultProviders, it) } ?: config.defaultProviders,
            libraryProviders = patch.libraryProviders
                ?.let { libraryProviders(config.libraryProviders, it) }
                ?: config.libraryProviders,
        )
    }

    private fun libraryProviders(
        config: Map<String, ProvidersConfig>,
        patch: Map<String, ProvidersConfigUpdateDto?>
    ): Map<String, ProvidersConfig> {
        val removeConfig = mutableSetOf<String>()
        val addConfigDto = mutableMapOf<String, ProvidersConfigUpdateDto>()
        val updateConfigDto = mutableMapOf<String, ProvidersConfigUpdateDto>()

        patch.forEach { (libraryId, configDto) ->
            if (configDto == null) removeConfig.add(libraryId)
            else if (config.containsKey(libraryId)) updateConfigDto[libraryId] = configDto
            else addConfigDto[libraryId] = configDto
        }

        val addConfig = addConfigDto
            .map { (libraryId, configDto) ->
                libraryId to providersConfig(configDto)
            }.toMap()

        return config.filterKeys { !removeConfig.contains(it) }
            .map { (libraryId, config) ->
                libraryId to (updateConfigDto[libraryId]?.let { providersConfig(config, it) } ?: config)
            }.toMap() + addConfig
    }

    private fun providersConfig(config: ProvidersConfig, patch: ProvidersConfigUpdateDto): ProvidersConfig {
        return config.copy(
            mangaUpdates = patch.mangaUpdates?.let { providerConfig(config.mangaUpdates, it) } ?: config.mangaUpdates,
            mal = patch.mal?.let { providerConfig(config.mal, it) } ?: config.mal,
            nautiljon = patch.nautiljon?.let { providerConfig(config.nautiljon, it) } ?: config.nautiljon,
            aniList = patch.aniList?.let { aniListProviderConfig(config.aniList, it) } ?: config.aniList,
            yenPress = patch.yenPress?.let { providerConfig(config.yenPress, it) } ?: config.yenPress,
            kodansha = patch.kodansha?.let { providerConfig(config.kodansha, it) } ?: config.kodansha,
            viz = patch.viz?.let { providerConfig(config.viz, it) } ?: config.viz,
            bookWalker = patch.bookWalker?.let { providerConfig(config.bookWalker, it) } ?: config.bookWalker,
            mangaDex = patch.mangaDex?.let { mangaDexProviderConfig(config.mangaDex, it) } ?: config.mangaDex,
            bangumi = patch.bangumi?.let { providerConfig(config.bangumi, it) } ?: config.bangumi,
            comicVine = patch.comicVine?.let { providerConfig(config.comicVine, it) } ?: config.comicVine,
        )
    }

    private fun providersConfig(patch: ProvidersConfigUpdateDto): ProvidersConfig {
        val config = ProvidersConfig()
        return providersConfig(config, patch)
    }

    private fun providerConfig(config: ProviderConfig, patch: ProviderConfigUpdateDto): ProviderConfig {
        return config.copy(
            priority = patch.priority ?: config.priority,
            enabled = patch.enabled ?: config.enabled,
            mediaType = patch.mediaType ?: config.mediaType,
            authorRoles = patch.authorRoles ?: config.authorRoles,
            artistRoles = patch.artistRoles ?: config.artistRoles,
            seriesMetadata = patch.seriesMetadata
                ?.let { seriesMetadataConfig(config.seriesMetadata, it) }
                ?: config.seriesMetadata,
            bookMetadata = patch.bookMetadata
                ?.let { bookMetadataConfig(config.bookMetadata, it) }
                ?: config.bookMetadata,
            nameMatchingMode = when (val mode = patch.nameMatchingMode) {
                PatchValue.None -> null
                is PatchValue.Some -> mode.value
                PatchValue.Unset -> config.nameMatchingMode
            }
        )
    }

    private fun aniListProviderConfig(config: AniListConfig, patch: AniListConfigUpdateDto): AniListConfig {
        return config.copy(
            priority = patch.priority ?: config.priority,
            enabled = patch.enabled ?: config.enabled,
            mediaType = patch.mediaType ?: config.mediaType,
            authorRoles = patch.authorRoles ?: config.authorRoles,
            artistRoles = patch.artistRoles ?: config.artistRoles,
            tagsScoreThreshold = patch.tagsScoreThreshold ?: config.tagsScoreThreshold,
            tagsSizeLimit = patch.tagsSizeLimit ?: config.tagsSizeLimit,
            seriesMetadata = patch.seriesMetadata
                ?.let { seriesMetadataConfig(config.seriesMetadata, it) }
                ?: config.seriesMetadata,
            nameMatchingMode = when (val mode = patch.nameMatchingMode) {
                PatchValue.None -> null
                is PatchValue.Some -> mode.value
                PatchValue.Unset -> config.nameMatchingMode
            }
        )
    }

    private fun mangaDexProviderConfig(config: MangaDexConfig, patch: ProviderConfigUpdateDto): MangaDexConfig {
        return config.copy(
            priority = patch.priority ?: config.priority,
            enabled = patch.enabled ?: config.enabled,
            mediaType = patch.mediaType ?: config.mediaType,
            authorRoles = patch.authorRoles ?: config.authorRoles,
            artistRoles = patch.artistRoles ?: config.artistRoles,
            seriesMetadata = patch.seriesMetadata
                ?.let { seriesMetadataConfig(config.seriesMetadata, it) }
                ?: config.seriesMetadata,
            nameMatchingMode = when (val mode = patch.nameMatchingMode) {
                PatchValue.None -> null
                is PatchValue.Some -> mode.value
                PatchValue.Unset -> config.nameMatchingMode
            }
        )
    }

    private fun seriesMetadataConfig(
        config: SeriesMetadataConfig,
        patch: SeriesMetadataConfigUpdateDto
    ): SeriesMetadataConfig {
        return config.copy(
            status = patch.status ?: config.status,
            title = patch.title ?: config.title,
            summary = patch.summary ?: config.summary,
            publisher = patch.publisher ?: config.publisher,
            readingDirection = patch.readingDirection ?: config.readingDirection,
            ageRating = patch.ageRating ?: config.ageRating,
            language = patch.language ?: config.language,
            genres = patch.genres ?: config.genres,
            tags = patch.tags ?: config.tags,
            totalBookCount = patch.totalBookCount ?: config.totalBookCount,
            authors = patch.authors ?: config.authors,
            releaseDate = patch.releaseDate ?: config.releaseDate,
            thumbnail = patch.thumbnail ?: config.thumbnail,
            links = patch.links ?: config.links,
            books = patch.books ?: config.books,
            useOriginalPublisher = patch.useOriginalPublisher ?: config.useOriginalPublisher,
        )
    }

    private fun bookMetadataConfig(config: BookMetadataConfig, patch: BookMetadataConfigUpdateDto): BookMetadataConfig {
        return config.copy(
            title = patch.title ?: config.title,
            summary = patch.summary ?: config.summary,
            number = patch.number ?: config.number,
            numberSort = patch.numberSort ?: config.numberSort,
            releaseDate = patch.releaseDate ?: config.releaseDate,
            authors = patch.authors ?: config.authors,
            tags = patch.tags ?: config.tags,
            isbn = patch.isbn ?: config.isbn,
            links = patch.links ?: config.links,
            thumbnail = patch.thumbnail ?: config.thumbnail
        )
    }

    private fun komgaConfig(config: KomgaConfig, patch: KomgaConfigUpdateDto): KomgaConfig {
        return config.copy(
            baseUri = patch.baseUri ?: config.baseUri,
            komgaUser = patch.komgaUser ?: config.komgaUser,
            komgaPassword = patch.komgaPassword ?: config.komgaPassword,
            eventListener = eventListener(config.eventListener, patch.eventListener, patch.notifications),
            metadataUpdate = patch.metadataUpdate?.let { metadataUpdate(config.metadataUpdate, it) }
                ?: config.metadataUpdate,
        )
    }

    private fun kavitaConfig(config: KavitaConfig, patch: KavitaConfigUpdateDto): KavitaConfig {
        return config.copy(
            baseUri = patch.baseUri ?: config.baseUri,
            apiKey = patch.apiKey ?: config.apiKey,
            eventListener = eventListener(config.eventListener, patch.eventListener, patch.notifications),
            metadataUpdate = patch.metadataUpdate?.let { metadataUpdate(config.metadataUpdate, it) }
                ?: config.metadataUpdate,
        )
    }

    private fun eventListener(
        config: EventListenerConfig,
        eventListenerPatch: EventListenerConfigUpdateDto?,
        notificationsPatch: NotificationConfigUpdateDto?,
    ): EventListenerConfig {
        return config.copy(
            enabled = eventListenerPatch?.enabled ?: config.enabled,
            metadataLibraryFilter = eventListenerPatch?.libraries ?: config.metadataLibraryFilter,
            notificationsLibraryFilter = notificationsPatch?.libraries ?: config.notificationsLibraryFilter
        )
    }

    private fun metadataUpdate(
        config: MetadataUpdateConfig,
        patch: MetadataUpdateConfigUpdateDto
    ): MetadataUpdateConfig {
        return config.copy(
            default = patch.default?.let { metadataProcessingConfig(config.default, it) } ?: config.default,
            library = patch.library?.let { libraryMetadataProcessing(config.library, it) } ?: config.library,
        )
    }

    private fun libraryMetadataProcessing(
        config: Map<String, MetadataProcessingConfig>,
        patch: Map<String, MetadataProcessingConfigUpdateDto?>
    ): Map<String, MetadataProcessingConfig> {
        val removeConfig = mutableSetOf<String>()
        val addConfigDto = mutableMapOf<String, MetadataProcessingConfigUpdateDto>()
        val updateConfigDto = mutableMapOf<String, MetadataProcessingConfigUpdateDto>()

        patch.forEach { (libraryId, configDto) ->
            if (configDto == null) removeConfig.add(libraryId)
            else if (config.containsKey(libraryId)) updateConfigDto[libraryId] = configDto
            else addConfigDto[libraryId] = configDto
        }

        val addConfig = addConfigDto
            .map { (libraryId, configDto) ->
                libraryId to metadataProcessingConfig(configDto)
            }.toMap()

        return config.filterKeys { !removeConfig.contains(it) }
            .map { (libraryId, config) ->
                libraryId to (updateConfigDto[libraryId]?.let { metadataProcessingConfig(config, it) } ?: config)
            }.toMap() + addConfig
    }

    private fun metadataProcessingConfig(patch: MetadataProcessingConfigUpdateDto): MetadataProcessingConfig {
        val config = MetadataProcessingConfig()
        return metadataProcessingConfig(config, patch)
    }

    private fun metadataProcessingConfig(
        config: MetadataProcessingConfig,
        patch: MetadataProcessingConfigUpdateDto
    ): MetadataProcessingConfig {
        return config.copy(
            libraryType = patch.libraryType ?: config.libraryType,
            aggregate = patch.aggregate ?: config.aggregate,
            mergeTags = patch.mergeTags ?: config.mergeTags,
            mergeGenres = patch.mergeGenres ?: config.mergeGenres,
            bookCovers = patch.bookCovers ?: config.bookCovers,
            seriesCovers = patch.seriesCovers ?: config.seriesCovers,
            overrideExistingCovers = patch.overrideExistingCovers ?: config.overrideExistingCovers,
            updateModes = patch.updateModes ?: config.updateModes,
            postProcessing = patch.postProcessing
                ?.let { metadataPostProcessingConfig(config.postProcessing, it) }
                ?: config.postProcessing
        )
    }

    private fun metadataPostProcessingConfig(
        config: MetadataPostProcessingConfig,
        patch: MetadataPostProcessingConfigUpdateDto
    ): MetadataPostProcessingConfig {
        return config.copy(
            seriesTitle = patch.seriesTitle ?: config.seriesTitle,
            seriesTitleLanguage = when (val language = patch.seriesTitleLanguage) {
                PatchValue.None -> null
                is PatchValue.Some -> language.value.ifBlank { null }
                PatchValue.Unset -> config.seriesTitleLanguage
            },
            alternativeSeriesTitles = patch.alternativeSeriesTitles ?: config.alternativeSeriesTitles,
            alternativeSeriesTitleLanguages = patch.alternativeSeriesTitleLanguages
                ?: config.alternativeSeriesTitleLanguages,
            orderBooks = patch.orderBooks ?: config.orderBooks,
            readingDirectionValue = when (val readingDirection = patch.readingDirectionValue) {
                PatchValue.None -> null
                is PatchValue.Some -> readingDirection.value
                PatchValue.Unset -> config.readingDirectionValue
            },
            languageValue = when (val language = patch.languageValue) {
                PatchValue.None -> null
                is PatchValue.Some -> language.value
                PatchValue.Unset -> config.languageValue
            },
            fallbackToAltTitle = patch.fallbackToAltTitle ?: config.fallbackToAltTitle,
        )
    }

    private fun discord(config: DiscordConfig, patch: DiscordConfigUpdateDto): DiscordConfig {
        val oldWebhooks = config.webhooks?.mapIndexed { index, value -> index to value }?.toMap()
        val newWebhooks = when (val patchWebhooks = patch.webhooks) {
            is PatchValue.Some -> oldWebhooks?.let { it + patchWebhooks.value } ?: patchWebhooks.value
            PatchValue.None, PatchValue.Unset -> oldWebhooks
        }
        return config.copy(
            webhooks = newWebhooks?.values?.filterNotNull(),
            seriesCover = patch.seriesCover ?: config.seriesCover,
        )
    }
}