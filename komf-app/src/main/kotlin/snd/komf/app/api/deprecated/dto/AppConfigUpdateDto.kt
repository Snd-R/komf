package snd.komf.app.api.deprecated.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import snd.komf.model.AuthorRole
import snd.komf.model.MediaType
import snd.komf.model.ReadingDirection
import snd.komf.model.UpdateMode
import snd.komf.util.NameSimilarityMatcher.NameMatchingMode

@Serializable
data class AppConfigUpdateDto(
    val komga: KomgaConfigUpdateDto? = null,
    val kavita: KavitaConfigUpdateDto? = null,
    val discord: DiscordConfigUpdateDto? = null,
    val metadataProviders: MetadataProvidersConfigUpdateDto? = null,
)

@Serializable
data class KomgaConfigUpdateDto(
    val baseUri: String? = null,
    val komgaUser: String? = null,
    val komgaPassword: String? = null,
    val eventListener: EventListenerConfigUpdateDto? = null,
    val notifications: NotificationConfigUpdateDto? = null,
    val metadataUpdate: MetadataUpdateConfigUpdateDto? = null,
)

@Serializable
data class KavitaConfigUpdateDto(
    val baseUri: String? = null,
    val apiKey: String? = null,
    val eventListener: EventListenerConfigUpdateDto? = null,
    val notifications: NotificationConfigUpdateDto? = null,
    val metadataUpdate: MetadataUpdateConfigUpdateDto? = null,
)

@Serializable
data class NotificationConfigUpdateDto(
    val libraries: Collection<String>? = null
)

@Serializable
data class MetadataUpdateConfigUpdateDto(
    val default: MetadataProcessingConfigUpdateDto? = null,
    val library: Map<String, MetadataProcessingConfigUpdateDto>? = null
)

@Serializable
data class MetadataProcessingConfigUpdateDto(
    val libraryType: MediaType? = null,
    val aggregate: Boolean? = null,
    val bookCovers: Boolean? = null,
    val mergeTags: Boolean? = null,
    val mergeGenres: Boolean? = null,
    val seriesCovers: Boolean? = null,
    val overrideExistingCovers: Boolean? = null,
    var lockCovers: Boolean? = null,
    val updateModes: List<UpdateMode>? = null,
    val postProcessing: MetadataPostProcessingConfigUpdateDto? = null
)

@Serializable
class MetadataPostProcessingConfigUpdateDto(
    val seriesTitle: Boolean? = null,
    val seriesTitleLanguage: PatchValue<String> = PatchValue.Unset,

    val orderBooks: Boolean? = null,
    val alternativeSeriesTitles: Boolean? = null,
    val alternativeSeriesTitleLanguages: List<String>? = null,

    val readingDirectionValue: PatchValue<ReadingDirection> = PatchValue.Unset,
    val languageValue: PatchValue<String> = PatchValue.Unset,
    val fallbackToAltTitle: Boolean? = null,
)

@Serializable
class DiscordConfigUpdateDto(
    val webhooks: PatchValue<Map<Int, String?>> = PatchValue.Unset,
    val seriesCover: Boolean? = null,
)

@Serializable
data class EventListenerConfigUpdateDto(
    val enabled: Boolean? = null,
    val libraries: Collection<String>? = null
)

@Serializable
class MetadataProvidersConfigUpdateDto(
    val comicVineClientId: PatchValue<String> = PatchValue.Unset,
    val malClientId: String? = null,
    val bangumiToken: String? = null,
    val nameMatchingMode: NameMatchingMode? = null,
    val defaultProviders: ProvidersConfigUpdateDto? = null,
    val libraryProviders: Map<String, ProvidersConfigUpdateDto?>? = null,
)

@Serializable
data class ProvidersConfigUpdateDto(
    val mangaUpdates: ProviderConfigUpdateDto? = null,
    val mal: ProviderConfigUpdateDto? = null,
    val nautiljon: ProviderConfigUpdateDto? = null,
    val aniList: AniListConfigUpdateDto? = null,
    val yenPress: ProviderConfigUpdateDto? = null,
    val kodansha: ProviderConfigUpdateDto? = null,
    val viz: ProviderConfigUpdateDto? = null,
    val bookWalker: ProviderConfigUpdateDto? = null,
    val mangaDex: ProviderConfigUpdateDto? = null,
    val bangumi: ProviderConfigUpdateDto? = null,
    val comicVine: ProviderConfigUpdateDto? = null,
)

@Serializable
class ProviderConfigUpdateDto(
    val nameMatchingMode: PatchValue<NameMatchingMode> = PatchValue.Unset,
    val priority: Int? = null,
    val enabled: Boolean? = null,
    val mediaType: MediaType? = null,
    val authorRoles: Collection<AuthorRole>? = null,
    val artistRoles: Collection<AuthorRole>? = null,
    val seriesMetadata: SeriesMetadataConfigUpdateDto? = null,
    val bookMetadata: BookMetadataConfigUpdateDto? = null,
)

@Serializable
class AniListConfigUpdateDto(
    val nameMatchingMode: PatchValue<NameMatchingMode> = PatchValue.Unset,
    val priority: Int? = null,
    val enabled: Boolean? = null,
    val mediaType: MediaType? = null,
    val authorRoles: Collection<AuthorRole>? = null,
    val artistRoles: Collection<AuthorRole>? = null,
    val tagsScoreThreshold: Int? = null,
    val tagsSizeLimit: Int? = null,
    val seriesMetadata: SeriesMetadataConfigUpdateDto? = null,
)

@Serializable
class SeriesMetadataConfigUpdateDto(
    val status: Boolean? = null,
    val title: Boolean? = null,
    val summary: Boolean? = null,
    val publisher: Boolean? = null,
    val readingDirection: Boolean? = null,
    val ageRating: Boolean? = null,
    val language: Boolean? = null,
    val genres: Boolean? = null,
    val tags: Boolean? = null,
    val totalBookCount: Boolean? = null,
    val authors: Boolean? = null,
    val releaseDate: Boolean? = null,
    val thumbnail: Boolean? = null,
    val links: Boolean? = null,
    val books: Boolean? = null,
    val useOriginalPublisher: Boolean? = null,

    val originalPublisherTagName: PatchValue<String> = PatchValue.Unset,
    val englishPublisherTagName: PatchValue<String> = PatchValue.Unset,
    val frenchPublisherTagName: PatchValue<String> = PatchValue.Unset,
)

@Serializable
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


@Serializable(PatchValueSerializer::class)
sealed class PatchValue<out T> {
    data object Unset : PatchValue<Nothing>()
    data object None : PatchValue<Nothing>()
    class Some<T>(val value: T) : PatchValue<T>()

}

class PatchValueSerializer<T : Any>(
    private val valueSerializer: KSerializer<T>
) : KSerializer<PatchValue<T>> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): PatchValue<T> {
        return when (val value = decoder.decodeNullableSerializableValue(valueSerializer)) {
            null -> PatchValue.None
            else -> PatchValue.Some(value)
        }
    }

    override fun serialize(encoder: Encoder, value: PatchValue<T>) {
        throw SerializationException("Serialization is unsupported")
    }
}
