package snd.komf.providers.mangabaka

import com.fleeksoft.ksoup.Ksoup
import io.ktor.http.parseUrl
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.Publisher
import snd.komf.model.PublisherType
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType
import snd.komf.model.TitleType.ROMAJI
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig


class MangaBakaMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(series: MangaBakaSeries, thumbnail: Image? = null): ProviderSeriesMetadata {
        val status = when (series.status) {
            MangaBakaStatus.RELEASING -> SeriesStatus.ONGOING
            MangaBakaStatus.UPCOMING -> SeriesStatus.ONGOING
            MangaBakaStatus.COMPLETED -> SeriesStatus.COMPLETED
            MangaBakaStatus.CANCELLED -> SeriesStatus.ABANDONED
            MangaBakaStatus.HIATUS -> SeriesStatus.HIATUS
            MangaBakaStatus.UNKNOWN -> SeriesStatus.ONGOING
        }

        val authors = series.authors?.flatMap { authorRoles.map { role -> Author(it, role) } } ?: emptyList()
        val artists = series.artists?.flatMap { artistRoles.map { role -> Author(it, role) } } ?: emptyList()

        val originalPublishers = series.publishers?.filter { it.type == "Original" }
            ?.mapNotNull { it.name }
            ?.map { Publisher(it, PublisherType.ORIGINAL) }?.toSet()
            ?: emptySet()
        val englishPublishers = series.publishers?.filter { it.type == "English" }
            ?.mapNotNull { it.name }
            ?.map { Publisher(it, PublisherType.LOCALIZED, "en") }?.toSet()
            ?: emptySet()

        val originalLanguage = when (series.type) {
            MangaBakaType.MANGA -> "ja"
            MangaBakaType.NOVEL -> null
            MangaBakaType.MANHWA -> "ko"
            MangaBakaType.MANHUA -> "zh"
            MangaBakaType.OEL -> "en"
            MangaBakaType.OTHER -> null
        }
        val titles = listOf(SeriesTitle(series.title, null, null)) +
                listOfNotNull(series.nativeTitle?.let { SeriesTitle(it, TitleType.NATIVE, null) })
        val secondaryTitles = series.secondaryTitles?.flatMap { (language, titles) ->
            val titleType = when (language) {
                originalLanguage -> TitleType.NATIVE
                "ja-ro", "ko-ro", "zh-ro" -> ROMAJI
                else -> TitleType.LOCALIZED
            }
            titles.map { SeriesTitle(it.title, titleType, language) }
        } ?: emptyList()

        val publisher = if (metadataConfig.useOriginalPublisher) originalPublishers.firstOrNull()
        else englishPublishers.firstOrNull() ?: originalPublishers.firstOrNull()

        val links = (series.links ?: emptyList()).mapNotNull { link ->
            when {
                link.startsWith("https://anilist.co") -> WebLink("AniList", link)
                link.startsWith("https://kitsu.app") -> WebLink("Kitsu", link)
                link.startsWith("https://myanimelist.net") -> WebLink("MyAnimeList", link)
                link.startsWith("https://www.anime-planet.com") -> WebLink("Anime-Planet", link)
                link.startsWith("https://www.novelupdates.com") -> WebLink("NovelUpdates", link)
                link.startsWith("https://mangabaka.dev") -> WebLink("MangaBaka", link)
                else -> parseUrl(link)?.host?.let { WebLink(it.removePrefix("www."), link) }
            }
        }.sortedBy { it.label }

        val metadata = SeriesMetadata(
            status = status,
            titles = titles + secondaryTitles,
            summary = series.description?.let { Ksoup.parse(it).wholeText() },
            publisher = publisher,
            alternativePublishers = (originalPublishers + englishPublishers) - setOfNotNull(publisher),
            genres = series.genres?.sorted() ?: emptyList(),
            tags = series.tags?.sorted() ?: emptyList(),
            totalBookCount = series.finalChapter?.toIntOrNull(),
            authors = authors + artists,
            thumbnail = thumbnail,
            releaseDate = ReleaseDate(series.year, null, null),
            links = links,
            score = series.rating
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = ProviderSeriesId(series.id.toString()), metadata = metadata),
            metadataConfig
        )
    }

    fun toSeriesSearchResult(series: MangaBakaSeries): SeriesSearchResult {
        return SeriesSearchResult(
            url = series.url(),
            imageUrl = series.cover.small,
            title = series.title,
            provider = CoreProviders.MANGA_BAKA,
            resultId = series.id.value.toString()
        )
    }
}