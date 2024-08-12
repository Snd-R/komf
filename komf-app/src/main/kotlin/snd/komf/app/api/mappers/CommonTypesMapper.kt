package snd.komf.app.api.mappers

import snd.komf.api.KomfAuthorRole
import snd.komf.api.KomfMediaType
import snd.komf.api.KomfNameMatchingMode
import snd.komf.api.KomfProviders
import snd.komf.api.KomfReadingDirection
import snd.komf.api.KomfUpdateMode
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
    CoreProviders.ANILIST -> KomfProviders.ANILIST
    CoreProviders.BANGUMI -> KomfProviders.BANGUMI
    CoreProviders.BOOK_WALKER -> KomfProviders.BOOK_WALKER
    CoreProviders.COMIC_VINE -> KomfProviders.COMIC_VINE
    CoreProviders.KODANSHA -> KomfProviders.KODANSHA
    CoreProviders.MAL -> KomfProviders.MAL
    CoreProviders.MANGA_UPDATES -> KomfProviders.MANGA_UPDATES
    CoreProviders.MANGADEX -> KomfProviders.MANGADEX
    CoreProviders.NAUTILJON -> KomfProviders.NAUTILJON
    CoreProviders.YEN_PRESS -> KomfProviders.YEN_PRESS
    CoreProviders.VIZ -> KomfProviders.VIZ
}

fun KomfProviders.toProvider() = when (this) {
    KomfProviders.ANILIST -> CoreProviders.ANILIST
    KomfProviders.BANGUMI -> CoreProviders.BANGUMI
    KomfProviders.BOOK_WALKER -> CoreProviders.BOOK_WALKER
    KomfProviders.COMIC_VINE -> CoreProviders.COMIC_VINE
    KomfProviders.KODANSHA -> CoreProviders.KODANSHA
    KomfProviders.MAL -> CoreProviders.MAL
    KomfProviders.MANGA_UPDATES -> CoreProviders.MANGA_UPDATES
    KomfProviders.MANGADEX -> CoreProviders.MANGADEX
    KomfProviders.NAUTILJON -> CoreProviders.NAUTILJON
    KomfProviders.YEN_PRESS -> CoreProviders.YEN_PRESS
    KomfProviders.VIZ -> CoreProviders.VIZ
}
