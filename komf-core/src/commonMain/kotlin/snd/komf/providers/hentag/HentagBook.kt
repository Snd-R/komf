package snd.komf.providers.hentag

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class HentagBook(
    val title: String,
    val coverImageUrl: String? = null,
    val parodies: List<String>? = null,
    val circles: List<String>? = null,
    val artists: List<String>? = null,
    val characters: List<String>? = null,
    val maleTags: List<String>? = null,
    val femaleTags: List<String>? = null,
    val otherTags: List<String>? = null,
    val language: String,
    val category: String,
    @Serializable(with = InstantEpochMillisSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantEpochMillisSerializer::class)
    val lastModified: Instant,
    @Serializable(with = InstantEpochMillisSerializer::class)
    val publishedOn: Instant? = null,
    val locations: List<String>? = null,
    val favorite: Boolean,
)

object InstantEpochMillisSerializer : KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlinx.datetime.Instant", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant =
        Instant.fromEpochMilliseconds(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeLong(value.toEpochMilliseconds())
}
