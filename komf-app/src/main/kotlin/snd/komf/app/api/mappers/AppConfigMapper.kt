package snd.komf.app.api.mappers

import snd.komf.api.MangaDexLink
import snd.komf.api.config.AniListConfigDto
import snd.komf.api.config.AppriseConfigDto
import snd.komf.api.config.BookMetadataConfigDto
import snd.komf.api.config.DiscordConfigDto
import snd.komf.api.config.EventListenerConfigDto
import snd.komf.api.config.KavitaConfigDto
import snd.komf.api.config.KomfConfig
import snd.komf.api.config.KomgaConfigDto
import snd.komf.api.config.MangaBakaConfigDto
import snd.komf.api.config.MangaDexConfigDto
import snd.komf.api.config.MetadataPostProcessingConfigDto
import snd.komf.api.config.MetadataProcessingConfigDto
import snd.komf.api.config.MetadataProvidersConfigDto
import snd.komf.api.config.MetadataUpdateConfigDto
import snd.komf.api.config.NotificationConfigDto
import snd.komf.api.config.ProviderConfigDto
import snd.komf.api.config.ProvidersConfigDto
import snd.komf.api.config.PublisherTagNameConfigDto
import snd.komf.api.config.SeriesMetadataConfigDto
import snd.komf.app.config.AppConfig
import snd.komf.mediaserver.config.EventListenerConfig
import snd.komf.mediaserver.config.KavitaConfig
import snd.komf.mediaserver.config.KomgaConfig
import snd.komf.mediaserver.config.MetadataPostProcessingConfig
import snd.komf.mediaserver.config.MetadataProcessingConfig
import snd.komf.mediaserver.config.MetadataUpdateConfig
import snd.komf.notifications.NotificationsConfig
import snd.komf.notifications.apprise.AppriseConfig
import snd.komf.notifications.discord.DiscordConfig
import snd.komf.providers.AniListConfig
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.MangaBakaConfig
import snd.komf.providers.MangaDexConfig
import snd.komf.providers.MetadataProvidersConfig
import snd.komf.providers.ProviderConfig
import snd.komf.providers.ProvidersConfig
import snd.komf.providers.SeriesMetadataConfig

class AppConfigMapper {
    private val maskedPlaceholder = "********"

    fun toDto(config: AppConfig): KomfConfig {
        return KomfConfig(
            metadataProviders = toDto(config.metadataProviders),
            komga = toDto(config.komga),
            kavita = toDto(config.kavita),
            notifications = toDto(config.notifications),
        )
    }

    private fun toDto(config: KomgaConfig): KomgaConfigDto {
        return KomgaConfigDto(
            baseUri = config.baseUri,
            komgaUser = config.komgaUser,
            eventListener = toDto(config.eventListener),
            metadataUpdate = toDto(config.metadataUpdate),
        )
    }

    private fun toDto(config: KavitaConfig): KavitaConfigDto {
        return KavitaConfigDto(
            baseUri = config.baseUri,
            eventListener = toDto(config.eventListener),
            metadataUpdate = toDto(config.metadataUpdate),
        )
    }

    private fun toDto(config: EventListenerConfig): EventListenerConfigDto {
        return EventListenerConfigDto(
            enabled = config.enabled,
            metadataLibraryFilter = config.metadataLibraryFilter,
            metadataSeriesExcludeFilter = config.metadataSeriesExcludeFilter,
            notificationsLibraryFilter = config.notificationsLibraryFilter
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
            libraryType = config.libraryType.fromMediaType(),
            aggregate = config.aggregate,
            mergeTags = config.mergeTags,
            mergeGenres = config.mergeGenres,
            bookCovers = config.bookCovers,
            seriesCovers = config.seriesCovers,
            overrideExistingCovers = config.overrideExistingCovers,
            lockCovers = config.lockCovers,
            updateModes = config.updateModes.map { it.fromUpdateMode() },
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
            readingDirectionValue = config.readingDirectionValue?.fromReadingDirection(),
            languageValue = config.languageValue,
            fallbackToAltTitle = config.fallbackToAltTitle,
            scoreTagName = config.scoreTagName,
            originalPublisherTagName = config.originalPublisherTagName,
            publisherTagNames = config.publisherTagNames.map { PublisherTagNameConfigDto(it.tagName, it.language) }
        )
    }

    private fun toDto(config: MetadataProvidersConfig): MetadataProvidersConfigDto {
        val malClientId = config.malClientId?.let { clientId ->
            if (clientId.length < 32) maskedPlaceholder
            else clientId.replace("(?<=.{4}).".toRegex(), "*")
        }

        val comicVineClientId = config.comicVineApiKey?.let { apiKey ->
            if (apiKey.length < 40) maskedPlaceholder
            else apiKey.replace("(?<=.{4}).".toRegex(), "*")
        }

        return MetadataProvidersConfigDto(
            malClientId = malClientId,
            comicVineClientId = comicVineClientId,
            nameMatchingMode = config.nameMatchingMode.fromNameMatchingMode(),
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
            hentag = toDto(config.hentag),
            mangaBaka = toDto(config.mangaBaka),
            webtoons = toDto(config.webtoons),
        )
    }

    private fun toDto(config: ProviderConfig): ProviderConfigDto {
        return ProviderConfigDto(
            nameMatchingMode = config.nameMatchingMode?.fromNameMatchingMode(),
            priority = config.priority,
            enabled = config.enabled,
            mediaType = config.mediaType.fromMediaType(),
            authorRoles = config.authorRoles.map { it.fromAuthorRole() },
            artistRoles = config.artistRoles.map { it.fromAuthorRole() },
            seriesMetadata = toDto(config.seriesMetadata),
            bookMetadata = toDto(config.bookMetadata),
        )
    }

    private fun toDto(config: AniListConfig): AniListConfigDto {
        return AniListConfigDto(
            nameMatchingMode = config.nameMatchingMode?.fromNameMatchingMode(),
            priority = config.priority,
            enabled = config.enabled,
            mediaType = config.mediaType.fromMediaType(),

            authorRoles = config.authorRoles.map { it.fromAuthorRole() },
            artistRoles = config.artistRoles.map { it.fromAuthorRole() },
            seriesMetadata = toDto(config.seriesMetadata),

            tagsScoreThreshold = config.tagsScoreThreshold,
            tagsSizeLimit = config.tagsSizeLimit,
        )
    }

    private fun toDto(config: MangaDexConfig): MangaDexConfigDto {
        return MangaDexConfigDto(
            priority = config.priority,
            enabled = config.enabled,
            seriesMetadata = toDto(config.seriesMetadata),
            bookMetadata = toDto(config.bookMetadata),
            nameMatchingMode = config.nameMatchingMode?.fromNameMatchingMode(),
            mediaType = config.mediaType.fromMediaType(),

            authorRoles = config.authorRoles.map { it.fromAuthorRole() },
            artistRoles = config.artistRoles.map { it.fromAuthorRole() },
            coverLanguages = config.coverLanguages,
            links = config.links.map { MangaDexLink.valueOf(it.name) }
        )
    }

    private fun toDto(config: MangaBakaConfig): MangaBakaConfigDto {
        return MangaBakaConfigDto(
            nameMatchingMode = config.nameMatchingMode?.fromNameMatchingMode(),
            priority = config.priority,
            enabled = config.enabled,
            mediaType = config.mediaType.fromMediaType(),
            authorRoles = config.authorRoles.map { it.fromAuthorRole() },
            artistRoles = config.artistRoles.map { it.fromAuthorRole() },
            seriesMetadata = toDto(config.seriesMetadata),
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

    private fun toDto(config: NotificationsConfig): NotificationConfigDto {
        return NotificationConfigDto(
            apprise = toDto(config.apprise),
            discord = toDto(config.discord),
        )
    }

    private fun toDto(config: DiscordConfig): DiscordConfigDto {
        return DiscordConfigDto(
            webhooks = config.webhooks
                ?.map {
                    if (it.length < 110) maskedPlaceholder
                    else it.replace("(?<=.{34}).(?=.{10})".toRegex(), "*")
                },
            seriesCover = config.seriesCover,
        )
    }

    private fun toDto(config: AppriseConfig): AppriseConfigDto {
        return AppriseConfigDto(
            urls = config.urls?.map { it.take(7) + "*".repeat(50) },
            seriesCover = config.seriesCover,
        )
    }

}
