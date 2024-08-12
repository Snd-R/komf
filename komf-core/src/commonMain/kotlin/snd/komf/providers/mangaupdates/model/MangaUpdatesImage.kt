package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaUpdatesImage(val url: MangaUpdatesImageUrl) {

    @Serializable
    data class MangaUpdatesImageUrl(val original: String?, val thumb: String?)
}
