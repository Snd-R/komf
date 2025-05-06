package snd.komf.app.api.mappers

import snd.komf.api.KomfAuthorRole
import snd.komf.api.KomfCoreProviders
import snd.komf.api.KomfMediaType
import snd.komf.api.KomfNameMatchingMode
import snd.komf.api.KomfProviders
import snd.komf.api.KomfReadingDirection
import snd.komf.api.KomfUpdateMode
import snd.komf.api.UnknownKomfProvider
import snd.komf.model.AuthorRole
import snd.komf.model.MediaType
import snd.komf.model.ReadingDirection
import snd.komf.model.UpdateMode
import snd.komf.providers.CoreProviders
import snd.komf.util.NameSimilarityMatcher.NameMatchingMode


fun KomfAuthorRole.toAuthorRole() = when (this) {
    KomfAuthorRole.WRITER -> AuthorRole.WRITER
    KomfAuthorRole.PENCILLER -> AuthorRole.PENCILLER
    KomfAuthorRole.INKER -> AuthorRole.INKER
    KomfAuthorRole.COLORIST -> AuthorRole.COLORIST
    KomfAuthorRole.LETTERER -> AuthorRole.LETTERER
    KomfAuthorRole.COVER -> AuthorRole.COVER
    KomfAuthorRole.EDITOR -> AuthorRole.EDITOR
    KomfAuthorRole.TRANSLATOR -> AuthorRole.TRANSLATOR
}

fun AuthorRole.fromAuthorRole() = when (this) {
    AuthorRole.WRITER -> KomfAuthorRole.WRITER
    AuthorRole.PENCILLER -> KomfAuthorRole.PENCILLER
    AuthorRole.INKER -> KomfAuthorRole.INKER
    AuthorRole.COLORIST -> KomfAuthorRole.COLORIST
    AuthorRole.LETTERER -> KomfAuthorRole.LETTERER
    AuthorRole.COVER -> KomfAuthorRole.COVER
    AuthorRole.EDITOR -> KomfAuthorRole.EDITOR
    AuthorRole.TRANSLATOR -> KomfAuthorRole.TRANSLATOR
}

fun KomfMediaType.toMediaType() = when (this) {
    KomfMediaType.MANGA -> MediaType.MANGA
    KomfMediaType.NOVEL -> MediaType.NOVEL
    KomfMediaType.COMIC -> MediaType.COMIC
}

fun MediaType.fromMediaType() = when (this) {
    MediaType.MANGA -> KomfMediaType.MANGA
    MediaType.NOVEL -> KomfMediaType.NOVEL
    MediaType.COMIC -> KomfMediaType.COMIC
}

fun KomfNameMatchingMode.toNameMatchingMode() = when (this) {
    KomfNameMatchingMode.EXACT -> NameMatchingMode.EXACT
    KomfNameMatchingMode.CLOSEST_MATCH -> NameMatchingMode.CLOSEST_MATCH
}

fun NameMatchingMode.fromNameMatchingMode() = when (this) {
    NameMatchingMode.EXACT -> KomfNameMatchingMode.EXACT
    NameMatchingMode.CLOSEST_MATCH -> KomfNameMatchingMode.CLOSEST_MATCH
}

fun KomfUpdateMode.toUpdateMode() = when (this) {
    KomfUpdateMode.API -> UpdateMode.API
    KomfUpdateMode.COMIC_INFO -> UpdateMode.COMIC_INFO
}

fun UpdateMode.fromUpdateMode() = when (this) {
    UpdateMode.API -> KomfUpdateMode.API
    UpdateMode.COMIC_INFO -> KomfUpdateMode.COMIC_INFO
}

fun KomfReadingDirection.toReadingDirection() = when (this) {
    KomfReadingDirection.LEFT_TO_RIGHT -> ReadingDirection.LEFT_TO_RIGHT
    KomfReadingDirection.RIGHT_TO_LEFT -> ReadingDirection.RIGHT_TO_LEFT
    KomfReadingDirection.VERTICAL -> ReadingDirection.VERTICAL
    KomfReadingDirection.WEBTOON -> ReadingDirection.WEBTOON
}

fun ReadingDirection.fromReadingDirection() = when (this) {
    ReadingDirection.LEFT_TO_RIGHT -> KomfReadingDirection.LEFT_TO_RIGHT
    ReadingDirection.RIGHT_TO_LEFT -> KomfReadingDirection.RIGHT_TO_LEFT
    ReadingDirection.VERTICAL -> KomfReadingDirection.VERTICAL
    ReadingDirection.WEBTOON -> KomfReadingDirection.WEBTOON
}

fun CoreProviders.fromProvider() = when (this) {
    CoreProviders.ANILIST -> KomfCoreProviders.ANILIST
    CoreProviders.BANGUMI -> KomfCoreProviders.BANGUMI
    CoreProviders.BOOK_WALKER -> KomfCoreProviders.BOOK_WALKER
    CoreProviders.COMIC_VINE -> KomfCoreProviders.COMIC_VINE
    CoreProviders.HENTAG -> KomfCoreProviders.HENTAG
    CoreProviders.KODANSHA -> KomfCoreProviders.KODANSHA
    CoreProviders.MAL -> KomfCoreProviders.MAL
    CoreProviders.MANGA_BAKA -> KomfCoreProviders.MANGA_BAKA
    CoreProviders.MANGA_UPDATES -> KomfCoreProviders.MANGA_UPDATES
    CoreProviders.MANGADEX -> KomfCoreProviders.MANGADEX
    CoreProviders.NAUTILJON -> KomfCoreProviders.NAUTILJON
    CoreProviders.YEN_PRESS -> KomfCoreProviders.YEN_PRESS
    CoreProviders.VIZ -> KomfCoreProviders.VIZ
}

fun KomfProviders.toProvider() = when (this) {
    KomfCoreProviders.ANILIST -> CoreProviders.ANILIST
    KomfCoreProviders.BANGUMI -> CoreProviders.BANGUMI
    KomfCoreProviders.BOOK_WALKER -> CoreProviders.BOOK_WALKER
    KomfCoreProviders.COMIC_VINE -> CoreProviders.COMIC_VINE
    KomfCoreProviders.HENTAG -> CoreProviders.HENTAG
    KomfCoreProviders.KODANSHA -> CoreProviders.KODANSHA
    KomfCoreProviders.MAL -> CoreProviders.MAL
    KomfCoreProviders.MANGA_BAKA -> CoreProviders.MANGA_BAKA
    KomfCoreProviders.MANGA_UPDATES -> CoreProviders.MANGA_UPDATES
    KomfCoreProviders.MANGADEX -> CoreProviders.MANGADEX
    KomfCoreProviders.NAUTILJON -> CoreProviders.NAUTILJON
    KomfCoreProviders.YEN_PRESS -> CoreProviders.YEN_PRESS
    KomfCoreProviders.VIZ -> CoreProviders.VIZ
    is UnknownKomfProvider -> CoreProviders.valueOf(this.name)
}
