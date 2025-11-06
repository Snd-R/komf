package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(SeriesTypeSerializer::class)
enum class SeriesType(val value: String) {
    ARTBOOK("Artbook"),
    DOUJINSHI("Doujinshi"),
    FILIPINO("Filipino"),
    INDONESIAN("Indonesian"),
    MANGA("Manga"),
    MANHWA("Manhwa"),
    MANHUA("Manhua"),
    OEL("OEL"),
    THAI("Thai"),
    VIETNAMESE("Vietnamese"),
    MALAYSIAN("Malaysian"),
    NORDIC("Nordic"),
    FRENCH("French"),
    SPANISH("Spanish"),
    NOVEL("Novel")
}

@Serializer(forClass = SeriesType::class)
object SeriesTypeSerializer : KSerializer<SeriesType> {
    override val descriptor = PrimitiveSerialDescriptor("SeriesType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SeriesType) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): SeriesType {
        val value = decoder.decodeString()
        return SeriesType.values().first { it.value == value }
    }
}
