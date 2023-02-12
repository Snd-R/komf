package org.snd.metadata.providers.mangadex.model

import com.squareup.moshi.JsonClass
import org.snd.metadata.model.Provider.MANGADEX
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.providers.mangadex.filesUrl
import java.time.ZonedDateTime

data class MangaDexManga(
    val id: MangaDexMangaId,
    val type: String,
    val attributes: MangaDexAttributes,
    val authors: List<MangaDexAuthor>,
    val artists: List<MangaDexAuthor>,
    val coverArt: MangaDexCoverArt,
)

data class MangaDexAttributes(
    val title: Map<String, String>,
    val altTitles: List<Map<String, String>>,
    val description: Map<String, String>,
    val isLocked: Boolean,
    val links: MangaDexLinks?,
    val originalLanguage: String,
    val lastVolume: String?,
    val lastChapter: String?,
    val publicationDemographic: MangaDexPublicationDemographic?,
    val status: MangaDexMangaStatus,
    val year: Int?,
    val contentRating: String,
    val tags: List<MangaDexTag>,
    val state: String,
    val chapterNumbersResetOnNewVolume: Boolean,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val version: Int,
    val availableTranslatedLanguages: List<String>,
    val latestUploadedChapter: String?,
)

data class MangaDexLinks(
    val aniList: String?,
    val animePlanet: String?,
    val bookWalker: String?,
    val mangaUpdates: String?,
    val novelUpdates: String?,
    val kitsu: String?,
    val amazon: String?,
    val ebookJapan: String?,
    val myAnimeList: String?,
    val cdJapan: String?,
    val raw: String?,
    val engTl: String?,
) {
    class Builder {
        var aniList: String? = null
        var animePlanet: String? = null
        var bookWalker: String? = null
        var mangaUpdates: String? = null
        var novelUpdates: String? = null
        var kitsu: String? = null
        var amazon: String? = null
        var ebookJapan: String? = null
        var myAnimeList: String? = null
        var cdJapan: String? = null
        var raw: String? = null
        var engTl: String? = null

        internal fun build() = MangaDexLinks(
            aniList,
            animePlanet,
            bookWalker,
            mangaUpdates,
            novelUpdates,
            kitsu,
            amazon,
            ebookJapan,
            myAnimeList,
            cdJapan,
            raw,
            engTl
        )

    }
}

enum class MangaDexPublicationDemographic {
    SHOUNEN,
    SHOUJO,
    JOSEI,
    SEINEN,
}

enum class MangaDexMangaStatus {
    ONGOING,
    COMPLETED,
    HIATUS,
    CANCELLED
}

@JsonClass(generateAdapter = true)
data class MangaDexTag(
    val id: String,
    val type: String,
    val attributes: MangaDexTagAttributes
)

@JsonClass(generateAdapter = true)
data class MangaDexTagAttributes(
    val name: Map<String, String>,
    val description: Map<String, String>,
    val group: String,
    val version: Int,
)

data class MangaDexAuthor(
    val id: String,
    val type: String,
    val name: String,
)

data class MangaDexCoverArt(
    val id: MangaDexCoverArtId,
    val description: String,
    val volume: String?,
    val fileName: String,
    val locale: String,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
    val version: Int,
)

fun MangaDexManga.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = filesUrl.newBuilder().addPathSegments("covers/${id.id}/${coverArt.fileName}.512.jpg").toString(),
        title = attributes.title["en"] ?: attributes.title.values.first(),
        provider = MANGADEX,
        resultId = id.id
    )
}