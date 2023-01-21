package org.snd.api.dto

import com.squareup.moshi.JsonClass
import org.snd.mediaserver.UpdateMode
import org.snd.metadata.NameMatchingMode
import org.snd.metadata.model.ReadingDirection
import org.snd.metadata.model.TitleType
import kotlin.properties.Delegates

@JsonClass(generateAdapter = true)
data class AppConfigUpdateDto(
    val komga: KomgaConfigUpdateDto? = null,
    val kavita: KavitaConfigUpdateDto? = null,
    val discord: DiscordConfigUpdateDto? = null,
    val metadataProviders: MetadataProvidersConfigUpdateDto? = null,
)

@JsonClass(generateAdapter = true)
data class KomgaConfigUpdateDto(
    val baseUri: String? = null,
    val komgaUser: String? = null,
    val komgaPassword: String? = null,
    val eventListener: EventListenerConfigUpdateDto? = null,
    val notifications: NotificationConfigUpdateDto? = null,
    val metadataUpdate: MetadataUpdateConfigUpdateDto? = null,
)

@JsonClass(generateAdapter = true)
data class KavitaConfigUpdateDto(
    val baseUri: String? = null,
    val apiKey: String? = null,
    val eventListener: EventListenerConfigUpdateDto? = null,
    val notifications: NotificationConfigUpdateDto? = null,
    val metadataUpdate: MetadataUpdateConfigUpdateDto? = null,
)

@JsonClass(generateAdapter = true)
data class NotificationConfigUpdateDto(
    val libraries: Collection<String>? = null
)

@JsonClass(generateAdapter = true)
data class MetadataUpdateConfigUpdateDto(
    val default: MetadataProcessingConfigUpdateDto? = null,
    val library: Map<String, MetadataProcessingConfigUpdateDto>? = null
)

@JsonClass(generateAdapter = true)
data class MetadataProcessingConfigUpdateDto(
    val aggregate: Boolean? = null,
    val bookCovers: Boolean? = null,
    val seriesCovers: Boolean? = null,
    val updateModes: Set<UpdateMode>? = null,
    val postProcessing: MetadataPostProcessingConfigUpdateDto? = null
)

@JsonClass(generateAdapter = true)
class MetadataPostProcessingConfigUpdateDto {
    @Transient
    private val isSet = mutableMapOf<String, Boolean>()
    fun isSet(prop: String) = isSet.getOrDefault(prop, false)

    var seriesTitle: Boolean? = null
    var titleType: TitleType? = null
    var orderBooks: Boolean? = null
    var alternativeSeriesTitles: Boolean? = null

    var readingDirectionValue: ReadingDirection? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
    var languageValue: String? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
}

@JsonClass(generateAdapter = true)
class DiscordConfigUpdateDto {
    @Transient
    private val isSet = mutableMapOf<String, Boolean>()
    fun isSet(prop: String) = isSet.getOrDefault(prop, false)

    var webhooks: Map<Int, String?>? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
    var imgurClientId: String? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }
    var seriesCover: Boolean? = null
}

@JsonClass(generateAdapter = true)
data class EventListenerConfigUpdateDto(
    val enabled: Boolean? = null,
    val libraries: Collection<String>? = null
)

@JsonClass(generateAdapter = true)
data class MetadataProvidersConfigUpdateDto(
    val malClientId: String? = null,
    val nameMatchingMode: NameMatchingMode? = null,
    val defaultProviders: ProvidersConfigUpdateDto? = null,
    val libraryProviders: Map<String, ProvidersConfigUpdateDto?>? = null,
)

@JsonClass(generateAdapter = true)
data class ProvidersConfigUpdateDto(
    val mangaUpdates: ProviderConfigUpdateDto? = null,
    val mal: ProviderConfigUpdateDto? = null,
    val nautiljon: ProviderConfigUpdateDto? = null,
    val aniList: ProviderConfigUpdateDto? = null,
    val yenPress: ProviderConfigUpdateDto? = null,
    val kodansha: ProviderConfigUpdateDto? = null,
    val viz: ProviderConfigUpdateDto? = null,
    val bookWalker: ProviderConfigUpdateDto? = null,
)

@JsonClass(generateAdapter = true)
class ProviderConfigUpdateDto {
    @Transient
    private val isSet = mutableMapOf<String, Boolean>()
    fun isSet(prop: String) = isSet.getOrDefault(prop, false)
    var nameMatchingMode: NameMatchingMode? by Delegates.observable(null) { prop, _, _ -> isSet[prop.name] = true }

    var priority: Int? = null
    var enabled: Boolean? = null
    var seriesMetadata: SeriesMetadataConfigUpdateDto? = null
    var bookMetadata: BookMetadataConfigUpdateDto? = null
}

@JsonClass(generateAdapter = true)
class SeriesMetadataConfigUpdateDto {
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
data class BookMetadataConfigUpdateDto(
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
