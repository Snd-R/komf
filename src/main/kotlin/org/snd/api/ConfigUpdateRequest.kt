package org.snd.api

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.UpdateMode
import org.snd.metadata.NameMatchingMode
import org.snd.metadata.model.ReadingDirection
import org.snd.metadata.model.TitleType
import kotlin.properties.Delegates

@JsonClass(generateAdapter = true)
data class ConfigUpdateRequest(
    val metadataProviders: MetadataProvidersConfigDto? = null,
    val komga: KomgaConfigDto? = null,
    val kavita: KavitaConfigDto? = null,
    val discord: DiscordConfigDto? = null,
)

@JsonClass(generateAdapter = true)
data class KomgaConfigDto(
    val eventListener: EventListenerConfigDto? = null,
    val notifications: NotificationConfigDto? = null,
    val metadataUpdate: MetadataUpdateConfigDto? = null,
    val aggregateMetadata: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class KavitaConfigDto(
    val eventListener: EventListenerConfigDto? = null,
    val notifications: NotificationConfigDto? = null,
    val metadataUpdate: MetadataUpdateConfigDto? = null,
    val aggregateMetadata: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class NotificationConfigDto(
    val libraries: Collection<String>? = null
)

@JsonClass(generateAdapter = true)
class MetadataUpdateConfigDto {
    @Transient
    private val isSet = mutableMapOf<String, Boolean>()
    fun isSet(prop: String) = isSet.getOrDefault(prop, false)
    var readingDirectionValue: ReadingDirection? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
    var languageValue: String? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }

    var modes: Set<UpdateMode>? = null
    var bookThumbnails: Boolean? = null
    var seriesThumbnails: Boolean? = null
    var seriesTitle: Boolean? = null
    var titleType: TitleType? = null
    var orderBooks: Boolean? = null
}

@JsonClass(generateAdapter = true)
class DiscordConfigDto {
    @Transient
    private val isSet = mutableMapOf<String, Boolean>()
    fun isSet(prop: String) = isSet.getOrDefault(prop, false)

    var webhooks: Collection<String>? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
}

@JsonClass(generateAdapter = true)
data class EventListenerConfigDto(
    val enabled: Boolean? = null,
    val libraries: Collection<String>? = null
)

@JsonClass(generateAdapter = true)
data class MetadataProvidersConfigDto(
    val malClientId: String? = null,
    val nameMatchingMode: NameMatchingMode? = null,
    val defaultProviders: ProvidersConfigDto? = null,
    val libraryProviders: Map<String, ProvidersConfigDto?>? = null,
)

@JsonClass(generateAdapter = true)
data class ProvidersConfigDto(
    val mangaUpdates: ProviderConfigDto? = null,
    val mal: ProviderConfigDto? = null,
    val nautiljon: ProviderConfigDto? = null,
    val aniList: ProviderConfigDto? = null,
    val yenPress: ProviderConfigDto? = null,
    val kodansha: ProviderConfigDto? = null,
    val viz: ProviderConfigDto? = null,
    val bookWalker: ProviderConfigDto? = null,
)

@JsonClass(generateAdapter = true)
class ProviderConfigDto {
    @Transient
    private val isSet = mutableMapOf<String, Boolean>()
    fun isSet(prop: String) = isSet.getOrDefault(prop, false)
    var nameMatchingMode: NameMatchingMode? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }

    var priority: Int? = null
    var enabled: Boolean? = null
    var seriesMetadata: SeriesMetadataConfigDto? = null
    var bookMetadata: BookMetadataConfigDto? = null
}

@JsonClass(generateAdapter = true)
class SeriesMetadataConfigDto {
    @Transient
    private val isSet = mutableMapOf<String, Boolean>()
    fun isSet(prop: String) = isSet.getOrDefault(prop, false)

    var status: Boolean? = null
    var title: Boolean? = null
    var summary: Boolean? = null
    var publisher: Boolean? = null
    var readingDirection: Boolean? = null
    var ageRating: Boolean? = null
    var language: Boolean? = null
    var genres: Boolean? = null
    var tags: Boolean? = null
    var totalBookCount: Boolean? = null
    var authors: Boolean? = null
    var releaseDate: Boolean? = null
    var thumbnail: Boolean? = null
    var books: Boolean? = null
    var useOriginalPublisher: Boolean? = null

    var originalPublisherTagName: String? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
    var englishPublisherTagName: String? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
    var frenchPublisherTagName: String? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
}

@JsonClass(generateAdapter = true)
data class BookMetadataConfigDto(
    val title: Boolean? = null,
    val summary: Boolean? = null,
    val number: Boolean? = null,
    val numberSort: Boolean? = null,
    val releaseDate: Boolean? = null,
    val authors: Boolean? = null,
    val tags: Boolean? = null,
    val isbn: Boolean? = null,
    val links: Boolean? = null,
    val thumbnail: Boolean? = null,
)
