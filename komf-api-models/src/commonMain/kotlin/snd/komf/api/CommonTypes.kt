package snd.komf.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline

enum class KomfAuthorRole {
    WRITER,
    PENCILLER,
    INKER,
    COLORIST,
    LETTERER,
    COVER,
    EDITOR,
    TRANSLATOR
}

enum class KomfMediaType {
    MANGA,
    NOVEL,
    COMIC,
}

enum class KomfNameMatchingMode {
    EXACT,
    CLOSEST_MATCH,
}

enum class KomfReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    VERTICAL,
    WEBTOON
}

enum class KomfUpdateMode {
    API,
    COMIC_INFO,
}

enum class MediaServer {
    KOMGA,
    KAVITA
}


@Serializable(with = KomfProvidersSerializer::class)
sealed interface KomfProviders
enum class KomfCoreProviders : KomfProviders {
    ANILIST,
    BANGUMI,
    BOOK_WALKER,
    COMIC_VINE,
    HENTAG,
    KODANSHA,
    MAL,
    MANGA_BAKA,
    MANGA_UPDATES,
    MANGADEX,
    NAUTILJON,
    WEBTOONS,
    YEN_PRESS,
    VIZ,
}

data class UnknownKomfProvider(val name: String) : KomfProviders

class KomfProvidersSerializer : KSerializer<KomfProviders> {
    override val descriptor = PrimitiveSerialDescriptor("KomfProviders", PrimitiveKind.STRING).nullable

    override fun serialize(encoder: Encoder, value: KomfProviders) {
        when (value) {
            is KomfCoreProviders -> encoder.encodeString(value.name)
            is UnknownKomfProvider -> encoder.encodeString(value.name)
        }
    }

    override fun deserialize(decoder: Decoder): KomfProviders {
        val name = decoder.decodeString()
        return runCatching { KomfCoreProviders.valueOf(name) }
            .getOrElse { UnknownKomfProvider(name) }
    }
}

enum class MangaDexLink {
    MANGA_DEX,
    ANILIST,
    ANIME_PLANET,
    BOOKWALKER_JP,
    MANGA_UPDATES,
    NOVEL_UPDATES,
    KITSU,
    AMAZON,
    EBOOK_JAPAN,
    MY_ANIME_LIST,
    CD_JAPAN,
    RAW,
    ENGLISH_TL,
}


@JvmInline
@Serializable
value class KomfServerSeriesId(val value: String) {
    override fun toString() = value
}

@JvmInline
@Serializable
value class KomfServerLibraryId(val value: String) {
    override fun toString() = value
}

@JvmInline
@Serializable
value class KomfProviderSeriesId(val value: String) {
    override fun toString() = value
}
