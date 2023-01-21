package org.snd.api

import org.snd.api.dto.AppConfigDto
import org.snd.api.dto.AppConfigUpdateDto
import org.snd.api.dto.BookMetadataConfigDto
import org.snd.api.dto.BookMetadataConfigUpdateDto
import org.snd.api.dto.DiscordConfigDto
import org.snd.api.dto.DiscordConfigUpdateDto
import org.snd.api.dto.EventListenerConfigDto
import org.snd.api.dto.EventListenerConfigUpdateDto
import org.snd.api.dto.KavitaConfigDto
import org.snd.api.dto.KavitaConfigUpdateDto
import org.snd.api.dto.KomgaConfigDto
import org.snd.api.dto.KomgaConfigUpdateDto
import org.snd.api.dto.MetadataPostProcessingConfigDto
import org.snd.api.dto.MetadataPostProcessingConfigUpdateDto
import org.snd.api.dto.MetadataProcessingConfigDto
import org.snd.api.dto.MetadataProcessingConfigUpdateDto
import org.snd.api.dto.MetadataProvidersConfigDto
import org.snd.api.dto.MetadataProvidersConfigUpdateDto
import org.snd.api.dto.MetadataUpdateConfigDto
import org.snd.api.dto.MetadataUpdateConfigUpdateDto
import org.snd.api.dto.NotificationConfigDto
import org.snd.api.dto.NotificationConfigUpdateDto
import org.snd.api.dto.ProviderConfigDto
import org.snd.api.dto.ProviderConfigUpdateDto
import org.snd.api.dto.ProvidersConfigDto
import org.snd.api.dto.ProvidersConfigUpdateDto
import org.snd.api.dto.SeriesMetadataConfigDto
import org.snd.api.dto.SeriesMetadataConfigUpdateDto
import org.snd.config.AppConfig
import org.snd.config.BookMetadataConfig
import org.snd.config.DiscordConfig
import org.snd.config.EventListenerConfig
import org.snd.config.KavitaConfig
import org.snd.config.KomgaConfig
import org.snd.config.MetadataPostProcessingConfig
import org.snd.config.MetadataProcessingConfig
import org.snd.config.MetadataProvidersConfig
import org.snd.config.MetadataUpdateConfig
import org.snd.config.NotificationConfig
import org.snd.config.ProviderConfig
import org.snd.config.ProvidersConfig
import org.snd.config.SeriesMetadataConfig

class ConfigUpdateMapper {
    private val maskedPlaceholder = "********"

    fun toDto(config: AppConfig): AppConfigDto {
        return AppConfigDto(
            metadataProviders = toDto(config.metadataProviders),
            komga = toDto(config.komga),
            kavita = toDto(config.kavita),
            discord = toDto(config.discord),
        )
    }

    fun patch(config: AppConfig, patch: AppConfigUpdateDto): AppConfig {
        return config.copy(
            metadataProviders = patch.metadataProviders
                ?.let { metadataProviders(config.metadataProviders, it) }
                ?: config.metadataProviders,
            komga = patch.komga?.let { komgaConfig(config.komga, it) } ?: config.komga,
            kavita = patch.kavita?.let { kavitaConfig(config.kavita, it) } ?: config.kavita,
            discord = patch.discord?.let { discord(config.discord, it) } ?: config.discord
        )
    }


    private fun toDto(config: KomgaConfig): KomgaConfigDto {
        return KomgaConfigDto(
            baseUri = config.baseUri,
            komgaUser = config.komgaUser,
            eventListener = toDto(config.eventListener),
            notifications = toDto(config.notifications),
            metadataUpdate = toDto(config.metadataUpdate),
        )
    }

    private fun toDto(config: KavitaConfig): KavitaConfigDto {
        return KavitaConfigDto(
            baseUri = config.baseUri,
            eventListener = toDto(config.eventListener),
            notifications = toDto(config.notifications),
            metadataUpdate = toDto(config.metadataUpdate),
        )
    }

    private fun toDto(config: EventListenerConfig): EventListenerConfigDto {
        return EventListenerConfigDto(
            enabled = config.enabled,
            libraries = config.libraries
        )
    }

    private fun toDto(config: NotificationConfig): NotificationConfigDto {
        return NotificationConfigDto(libraries = config.libraries)
    }

    private fun toDto(config: MetadataUpdateConfig): MetadataUpdateConfigDto {
        return MetadataUpdateConfigDto(
            default = toDto(config.default),
            library = config.library.map { (libraryId, config) -> libraryId to toDto(config) }.toMap()
        )
    }

    private fun toDto(config: MetadataProcessingConfig): MetadataProcessingConfigDto {
        return MetadataProcessingConfigDto(
            aggregate = config.aggregate,
            bookCovers = config.bookCovers,
            seriesCovers = config.seriesCovers,
            updateModes = config.updateModes,
            postProcessing = toDto(config.postProcessing)
        )
    }

    private fun toDto(config: MetadataPostProcessingConfig): MetadataPostProcessingConfigDto {
        return MetadataPostProcessingConfigDto(
            seriesTitle = config.seriesTitle,
            titleType = config.titleType,
            alternativeSeriesTitles = config.alternativeSeriesTitles,
            orderBooks = config.orderBooks,
            readingDirectionValue = config.readingDirectionValue,
            languageValue = config.languageValue
        )
    }

    private fun toDto(config: DiscordConfig): DiscordConfigDto {
        val imgurClientId = config.imgurClientId?.let {
            if (it.length < 15) maskedPlaceholder
            else it.replace("(?<=.{4}).".toRegex(), "*")
        }
        return DiscordConfigDto(
            webhooks = config.webhooks
                ?.map {
                    if (it.length < 110) maskedPlaceholder
                    else it.replace("(?<=.{34}).(?=.{10})".toRegex(), "*")
                }
                ?.mapIndexed { index, value -> index to value }
                ?.toMap(),
            seriesCover = config.seriesCover,
            imgurClientId = imgurClientId,
        )
    }

    private fun toDto(config: MetadataProvidersConfig): MetadataProvidersConfigDto {
        val malClientId = if (config.malClientId.length < 32) maskedPlaceholder
        else config.malClientId.replace("(?<=.{4}).".toRegex(), "*")
        return MetadataProvidersConfigDto(
            malClientId = malClientId,
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
        )
    }

    private fun toDto(config: ProviderConfig): ProviderConfigDto {
        return ProviderConfigDto(
            nameMatchingMode = config.nameMatchingMode,
            priority = config.priority,
            enabled = config.enabled,
            seriesMetadata = toDto(config.seriesMetadata),
            bookMetadata = toDto(config.bookMetadata)
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
            books = config.books,
            useOriginalPublisher = config.useOriginalPublisher,
            originalPublisherTagName = config.originalPublisherTagName ?: "",
            englishPublisherTagName = config.englishPublisherTagName ?: "",
            frenchPublisherTagName = config.frenchPublisherTagName ?: "",
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
            nameMatchingMode = patch.nameMatchingMode ?: config.nameMatchingMode,
            defaultProviders = patch.defaultProviders
                ?.let { providersConfig(config.defaultProviders, it) } ?: config.defaultProviders,
            libraryProviders = patch.libraryProviders
                ?.let { libraryProviders(config.libraryProviders, patch.libraryProviders) }
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
            aniList = patch.aniList?.let { providerConfig(config.aniList, it) } ?: config.aniList,
            yenPress = patch.yenPress?.let { providerConfig(config.yenPress, it) } ?: config.yenPress,
            kodansha = patch.kodansha?.let { providerConfig(config.kodansha, it) } ?: config.kodansha,
            viz = patch.viz?.let { providerConfig(config.viz, it) } ?: config.viz,
            bookWalker = patch.bookWalker?.let { providerConfig(config.bookWalker, it) } ?: config.bookWalker,
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
            seriesMetadata = patch.seriesMetadata
                ?.let { seriesMetadataConfig(config.seriesMetadata, it) }
                ?: config.seriesMetadata,
            bookMetadata = patch.bookMetadata
                ?.let { bookMetadataConfig(config.bookMetadata, it) }
                ?: config.bookMetadata,
            nameMatchingMode = if (patch.isSet("nameMatchingMode")) patch.nameMatchingMode else config.nameMatchingMode
        )
    }

    private fun seriesMetadataConfig(config: SeriesMetadataConfig, patch: SeriesMetadataConfigUpdateDto): SeriesMetadataConfig {
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
            books = patch.books ?: config.books,
            useOriginalPublisher = patch.useOriginalPublisher ?: config.useOriginalPublisher,
            originalPublisherTagName = if (patch.isSet("originalPublisherTagName")) patch.originalPublisherTagName
            else config.originalPublisherTagName,
            englishPublisherTagName = if (patch.isSet("englishPublisherTagName")) patch.englishPublisherTagName
            else config.englishPublisherTagName,
            frenchPublisherTagName = if (patch.isSet("frenchPublisherTagName")) patch.frenchPublisherTagName
            else config.frenchPublisherTagName,
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
            eventListener = patch.eventListener?.let { eventListener(config.eventListener, it) } ?: config.eventListener,
            notifications = patch.notifications?.let { notifications(config.notifications, it) } ?: config.notifications,
            metadataUpdate = patch.metadataUpdate?.let { metadataUpdate(config.metadataUpdate, it) } ?: config.metadataUpdate,
        )
    }

    private fun kavitaConfig(config: KavitaConfig, patch: KavitaConfigUpdateDto): KavitaConfig {
        return config.copy(
            baseUri = patch.baseUri ?: config.baseUri,
            apiKey = patch.apiKey ?: config.apiKey,
            notifications = patch.notifications?.let { notifications(config.notifications, it) } ?: config.notifications,
            eventListener = patch.eventListener?.let { eventListener(config.eventListener, it) } ?: config.eventListener,
            metadataUpdate = patch.metadataUpdate?.let { metadataUpdate(config.metadataUpdate, it) } ?: config.metadataUpdate,
        )
    }

    private fun eventListener(config: EventListenerConfig, patch: EventListenerConfigUpdateDto): EventListenerConfig {
        return config.copy(
            enabled = patch.enabled ?: config.enabled,
            libraries = patch.libraries ?: config.libraries
        )
    }

    private fun notifications(config: NotificationConfig, patch: NotificationConfigUpdateDto): NotificationConfig {
        return config.copy(libraries = patch.libraries ?: config.libraries)
    }

    private fun metadataUpdate(config: MetadataUpdateConfig, patch: MetadataUpdateConfigUpdateDto): MetadataUpdateConfig {
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
            aggregate = patch.aggregate ?: config.aggregate,
            bookCovers = patch.bookCovers ?: config.bookCovers,
            seriesCovers = patch.seriesCovers ?: config.seriesCovers,
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
            titleType = patch.titleType ?: config.titleType,
            alternativeSeriesTitles = patch.alternativeSeriesTitles ?: config.alternativeSeriesTitles,
            orderBooks = patch.orderBooks ?: config.orderBooks,
            readingDirectionValue = if (patch.isSet("readingDirectionValue")) patch.readingDirectionValue
            else config.readingDirectionValue,
            languageValue = if (patch.isSet("languageValue")) patch.languageValue
            else config.languageValue
        )
    }

    private fun discord(config: DiscordConfig, patch: DiscordConfigUpdateDto): DiscordConfig {
        val oldWebhooks = config.webhooks?.mapIndexed { index, value -> index to value }?.toMap()
        val patchWebhooks = patch.webhooks
        val newWebhooks = if (patch.isSet("webhooks")) {
            if (oldWebhooks != null && patchWebhooks != null) oldWebhooks + patchWebhooks
            else patchWebhooks ?: oldWebhooks
        } else oldWebhooks

        return config.copy(
            webhooks = newWebhooks?.values?.filterNotNull(),
            imgurClientId = if (patch.isSet("imgurClientId")) patch.imgurClientId else config.imgurClientId,
            seriesCover = patch.seriesCover ?: config.seriesCover,
        )
    }
}