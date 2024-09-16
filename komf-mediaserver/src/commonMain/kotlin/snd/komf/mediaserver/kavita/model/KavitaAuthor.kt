package snd.komf.mediaserver.kavita.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.CHARACTER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.COLORIST
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.COVER_ARTIST
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.EDITOR
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.IMPRINT
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.INKER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.LETTERER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.LOCATION
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.OTHER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.PENCILLER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.PUBLISHER
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.TEAM
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.TRANSLATOR
import snd.komf.mediaserver.kavita.model.KavitaPersonRole.WRITER


@Serializable
data class KavitaAuthor(
    val id: Int,
    val name: String,
    val role: KavitaPersonRole
)

@Serializable(with = KavitaPersonRoleSerializer::class)
enum class KavitaPersonRole(val id: Int) {
    OTHER(1),
    WRITER(3),
    PENCILLER(4),
    INKER(5),
    COLORIST(6),
    LETTERER(7),
    COVER_ARTIST(8),
    EDITOR(9),
    PUBLISHER(10),
    CHARACTER(11),
    TRANSLATOR(12),
    IMPRINT(13),
    TEAM(14),
    LOCATION(15),
}

class KavitaPersonRoleSerializer : KSerializer<KavitaPersonRole> {
    override val descriptor = PrimitiveSerialDescriptor("KavitaPersonRole", PrimitiveKind.INT).nullable
    override fun serialize(encoder: Encoder, value: KavitaPersonRole) = encoder.encodeInt(value.id)
    override fun deserialize(decoder: Decoder): KavitaPersonRole = when (decoder.decodeInt()) {
        1 -> OTHER
        3 -> WRITER
        4 -> PENCILLER
        5 -> INKER
        6 -> COLORIST
        7 -> LETTERER
        8 -> COVER_ARTIST
        9 -> EDITOR
        10 -> PUBLISHER
        11 -> CHARACTER
        12 -> TRANSLATOR
        13 -> IMPRINT
        14 -> TEAM
        15 -> LOCATION
        else -> error("Unsupported status code")
    }
}
