package snd.komf.mediaserver.kavita.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import snd.komf.mediaserver.kavita.model.KavitaAgeRating.*

@Serializable(with = KavitaAgeRatingSerializer::class)
enum class KavitaAgeRating(val id: Int, val ageRating: Int? = null) {
    NOT_APPLICABLE(-1),
    UNKNOWN(0),
    RATING_PENDING(1, 0),
    EARLY_CHILDHOOD(2, 3),
    EVERYONE(3, 0),
    G(4, 0),
    EVERYONE_10PLUS(5, 10),
    PG(6, 8),
    KIDS_TO_ADULTS(7, 6),
    TEEN(8, 13),
    MATURE_15PLUS(9, 15),
    MATURE_17PLUS(10, 17),
    MATURE(11, 17),
    R_18PLUS(12, 18),
    ADULTS_ONLY(13, 18),
    X_18PLUS(14, 18)
}

class KavitaAgeRatingSerializer : KSerializer<KavitaAgeRating> {
    override val descriptor = PrimitiveSerialDescriptor("KavitaAgeRating", PrimitiveKind.INT).nullable
    override fun serialize(encoder: Encoder, value: KavitaAgeRating) = encoder.encodeInt(value.id)
    override fun deserialize(decoder: Decoder): KavitaAgeRating = when (decoder.decodeInt()) {
        -1 -> NOT_APPLICABLE
        0 -> UNKNOWN
        1 -> RATING_PENDING
        2 -> EARLY_CHILDHOOD
        3 -> EVERYONE
        4 -> G
        5 -> EVERYONE_10PLUS
        6 -> PG
        7 -> KIDS_TO_ADULTS
        8 -> TEEN
        9 -> MATURE_15PLUS
        10 -> MATURE_17PLUS
        11 -> MATURE
        12 -> R_18PLUS
        13 -> ADULTS_ONLY
        14 -> X_18PLUS
        else -> error("Unsupported status code")
    }
}
