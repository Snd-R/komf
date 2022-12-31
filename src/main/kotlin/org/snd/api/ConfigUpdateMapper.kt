package org.snd.api

import org.snd.config.AppConfig
import org.snd.config.BookMetadataConfig
import org.snd.config.DiscordConfig
import org.snd.config.EventListenerConfig
import org.snd.config.KavitaConfig
import org.snd.config.KomgaConfig
import org.snd.config.MetadataProvidersConfig
import org.snd.config.MetadataUpdateConfig
import org.snd.config.NotificationConfig
import org.snd.config.ProviderConfig
import org.snd.config.ProvidersConfig
import org.snd.config.SeriesMetadataConfig

class ConfigUpdateMapper {

    fun patch(config: AppConfig, patch: ConfigUpdateRequest): AppConfig {
        return config.copy(
            metadataProviders = patch.metadataProviders
                ?.let { metadataProviders(config.metadataProviders, it) }
                ?: config.metadataProviders,
            komga = patch.komga?.let { komgaConfig(config.komga, it) } ?: config.komga,
            kavita = patch.kavita?.let { kavitaConfig(config.kavita, it) } ?: config.kavita,
            discord = patch.discord?.let { discord(config.discord, it) } ?: config.discord
        )
    }

    private fun metadataProviders(
        config: MetadataProvidersConfig,
        patch: MetadataProvidersConfigDto
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
        patch: Map<String, ProvidersConfigDto?>
    ): Map<String, ProvidersConfig> {
        val removeConfig = mutableSetOf<String>()
        val addConfigDto = mutableMapOf<String, ProvidersConfigDto>()
        val updateConfigDto = mutableMapOf<String, ProvidersConfigDto>()

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

    private fun providersConfig(config: ProvidersConfig, patch: ProvidersConfigDto): ProvidersConfig {
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

    private fun providersConfig(patch: ProvidersConfigDto): ProvidersConfig {
        val config = ProvidersConfig()
        return providersConfig(config, patch)
    }

    private fun providerConfig(config: ProviderConfig, patch: ProviderConfigDto): ProviderConfig {
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

    private fun seriesMetadataConfig(config: SeriesMetadataConfig, patch: SeriesMetadataConfigDto): SeriesMetadataConfig {
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

    private fun bookMetadataConfig(config: BookMetadataConfig, patch: BookMetadataConfigDto): BookMetadataConfig {
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

    private fun komgaConfig(config: KomgaConfig, patch: KomgaConfigDto): KomgaConfig {
        return config.copy(
            eventListener = patch.eventListener?.let { eventListener(config.eventListener, it) } ?: config.eventListener,
            notifications = patch.notifications?.let { notifications(config.notifications, it) } ?: config.notifications,
            metadataUpdate = patch.metadataUpdate?.let { metadataUpdate(config.metadataUpdate, it) } ?: config.metadataUpdate,
            aggregateMetadata = patch.aggregateMetadata ?: config.aggregateMetadata
        )
    }

    private fun kavitaConfig(config: KavitaConfig, patch: KavitaConfigDto): KavitaConfig {
        return config.copy(
            notifications = patch.notifications?.let { notifications(config.notifications, it) } ?: config.notifications,
            eventListener = patch.eventListener?.let { eventListener(config.eventListener, it) } ?: config.eventListener,
            metadataUpdate = patch.metadataUpdate?.let { metadataUpdate(config.metadataUpdate, it) } ?: config.metadataUpdate,
            aggregateMetadata = patch.aggregateMetadata ?: config.aggregateMetadata
        )
    }

    private fun eventListener(config: EventListenerConfig, patch: EventListenerConfigDto): EventListenerConfig {
        return config.copy(
            enabled = patch.enabled ?: config.enabled,
            libraries = patch.libraries ?: config.libraries
        )
    }

    private fun notifications(config: NotificationConfig, patch: NotificationConfigDto): NotificationConfig {
        return config.copy(libraries = patch.libraries ?: config.libraries)
    }

    private fun metadataUpdate(config: MetadataUpdateConfig, patch: MetadataUpdateConfigDto): MetadataUpdateConfig {
        return config.copy(
            readingDirectionValue = if (patch.isSet("readingDirectionValue")) patch.readingDirectionValue
            else config.readingDirectionValue,
            languageValue = if (patch.isSet("languageValue")) patch.languageValue
            else config.languageValue,
            modes = patch.modes ?: config.modes,
            bookThumbnails = patch.bookThumbnails ?: config.bookThumbnails,
            seriesThumbnails = patch.seriesThumbnails ?: config.seriesThumbnails,
            seriesTitle = patch.seriesTitle ?: config.seriesTitle,
            titleType = patch.titleType ?: config.titleType,
            orderBooks = patch.orderBooks ?: config.orderBooks
        )
    }

    private fun discord(config: DiscordConfig, patch: DiscordConfigDto): DiscordConfig {
        return config.copy(
            webhooks = if (patch.isSet("webhooks")) patch.webhooks
            else config.webhooks
        )
    }
}