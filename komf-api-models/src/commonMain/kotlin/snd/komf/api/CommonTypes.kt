package snd.komf.api

import kotlinx.serialization.Serializable
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


enum class KomfProviders {
    ANILIST,
    BANGUMI,
    BOOK_WALKER,
    COMIC_VINE,
    HENTAG,
    KODANSHA,
    MAL,
    MANGA_UPDATES,
    MANGADEX,
    NAUTILJON,
    YEN_PRESS,
    VIZ,
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
