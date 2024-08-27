package snd.komf.providers.mangadex

import io.ktor.http.*
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType.LOCALIZED
import snd.komf.model.TitleType.NATIVE
import snd.komf.model.TitleType.ROMAJI
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.mangadex.model.MangaDexArtist
import snd.komf.providers.mangadex.model.MangaDexAuthor
import snd.komf.providers.mangadex.model.MangaDexCoverArt
import snd.komf.providers.mangadex.model.MangaDexLink
import snd.komf.providers.mangadex.model.MangaDexLink.AMAZON
import snd.komf.providers.mangadex.model.MangaDexLink.ANILIST
import snd.komf.providers.mangadex.model.MangaDexLink.ANIME_PLANET
import snd.komf.providers.mangadex.model.MangaDexLink.BOOKWALKER_JP
import snd.komf.providers.mangadex.model.MangaDexLink.CD_JAPAN
import snd.komf.providers.mangadex.model.MangaDexLink.EBOOK_JAPAN
import snd.komf.providers.mangadex.model.MangaDexLink.ENGLISH_TL
import snd.komf.providers.mangadex.model.MangaDexLink.KITSU
import snd.komf.providers.mangadex.model.MangaDexLink.MANGA_UPDATES
import snd.komf.providers.mangadex.model.MangaDexLink.MY_ANIME_LIST
import snd.komf.providers.mangadex.model.MangaDexLink.NOVEL_UPDATES
import snd.komf.providers.mangadex.model.MangaDexLink.RAW
import snd.komf.providers.mangadex.model.MangaDexManga
import snd.komf.providers.mangadex.model.MangaDexMangaId

class MangaDexMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
    private val coverLanguages: List<String>,
    private val linksFilter: List<MangaDexLink>
) {
    private val mangaDexBaseUrl = "https://mangadex.org"

    fun toSeriesMetadata(
        manga: MangaDexManga,
        covers: List<MangaDexCoverArt>,
        cover: Image? = null
    ): ProviderSeriesMetadata {
        val status = when (manga.attributes.status) {
            "ongoing" -> SeriesStatus.ONGOING
            "completed" -> SeriesStatus.ENDED
            "hiatus" -> SeriesStatus.HIATUS
            "cancelled" -> SeriesStatus.ABANDONED
            else -> error("unknown status")
        }

        val authors = manga.relationships.filterIsInstance<MangaDexAuthor>()
            .flatMap { authorRoles.map { role -> Author(it.attributes.name, role) } }
        val artists = manga.relationships.filterIsInstance<MangaDexArtist>()
            .flatMap { artistRoles.map { role -> Author(it.attributes.name, role) } }


        val tags = manga.attributes.tags.filter { it.attributes.group == "theme" }
            .mapNotNull { it.attributes.name["en"] ?: it.attributes.name.values.firstOrNull() }

        val genres = listOfNotNull(manga.attributes.publicationDemographic?.lowercase()) +
                manga.attributes.tags.filter { it.attributes.group == "genre" }
                    .mapNotNull { it.attributes.name["en"] ?: it.attributes.name.values.firstOrNull() }

        val originalLang = manga.attributes.originalLanguage
        val titles = manga.attributes.altTitles
            .map { it.entries.first() }
            .map { (lang, name) ->
                when (lang) {
                    originalLang -> SeriesTitle(name, NATIVE, lang)
                    "ja-ro" -> SeriesTitle(name, ROMAJI, lang)
                    else -> SeriesTitle(name, LOCALIZED, lang)
                }
            }

        val links = mutableMapOf<MangaDexLink, WebLink>()
        links[MangaDexLink.MANGA_DEX] = WebLink("MangaDex", seriesUrl(manga.id))
        manga.attributes.links?.forEach { (key, value) ->
            when (key) {
                "al" -> links[ANILIST] =
                    WebLink("AniList", "https://anilist.co/manga/$value".encodeURLPath())

                "ap" -> links[ANIME_PLANET] =
                    WebLink("Anime-Planet", "https://www.anime-planet.com/manga/$value".encodeURLPath())

                "bw" -> links[BOOKWALKER_JP] =
                    WebLink("BookWalkerJp", "https://bookwalker.jp/$value".encodeURLPath())

                "mu" -> links[MANGA_UPDATES] =
                    WebLink("MangaUpdates", "https://www.mangaupdates.com/series.html?id=$value".encodeURLPath())

                "nu" -> links[NOVEL_UPDATES] =
                    WebLink("NovelUpdates", "https://www.novelupdates.com/series/$value".encodeURLPath())

                "kt" -> links[KITSU] = WebLink("Kitsu", "https://kitsu.app/manga/$value".encodeURLPath())
                "amz" -> links[AMAZON] = WebLink("Amazon", value)
                "ebj" -> links[EBOOK_JAPAN] = WebLink(
                    "eBookJapan",
                    value.toIntOrNull()
                        ?.let { bookId -> "https://ebookjapan.yahoo.co.jp/books/$bookId".encodeURLPath() }
                        ?: value.encodeURLPath()
                )

                "mal" -> links[MY_ANIME_LIST] =
                    WebLink("MyAnimeList", "https://myanimelist.net/manga/$value".encodeURLPath())

                "cdj" -> links[CD_JAPAN] = WebLink("CDJapan", value.encodeURLPath())
                "raw" -> links[RAW] = WebLink("Official Raw", value.encodeURLPath())
                "engtl" -> links[ENGLISH_TL] = WebLink("Official English", value.encodeURLPath())
            }
        }

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = manga.attributes.description.let { descriptionMap ->
                descriptionMap["en"] ?: descriptionMap.values.firstOrNull()
            },
            genres = genres,
            tags = tags,
            authors = authors + artists,
            thumbnail = cover,
            releaseDate = ReleaseDate(manga.attributes.year, null, null),
            links = if (linksFilter.isNotEmpty()) links.filter { it.key in linksFilter }.values else links.values,
        )

        val books = covers
            .filter { it.attributes.locale in coverLanguages }
            .groupBy { it.attributes.volume }.values
            .map { coverArtToBook(it.first()) }

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(
                id = ProviderSeriesId(manga.id.value),
                metadata = metadata,
                books = books
            ),
            seriesMetadataConfig
        )
    }

    fun toBookMetadata(filename: String, cover: Image?): ProviderBookMetadata {
        val metadata = BookMetadata(thumbnail = cover)

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(filename),
            metadata = metadata
        )

        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun coverArtToBook(coverArt: MangaDexCoverArt): SeriesBook {
        return SeriesBook(
            id = ProviderBookId(coverArt.attributes.fileName),
            number = coverArt.attributes.volume?.toDoubleOrNull()?.let { BookRange(it, it) },
            name = coverArt.attributes.volume,
            type = null,
            edition = null
        )
    }


    fun toSeriesSearchResult(manga: MangaDexManga): SeriesSearchResult {
        return SeriesSearchResult(
            url = seriesUrl(manga.id),
            imageUrl = manga.relationships.filterIsInstance<MangaDexCoverArt>().firstOrNull()?.attributes?.fileName
                ?.let { fileName -> "$filesUrl/covers/${manga.id.value}/$fileName.512.jpg" },
            title = manga.attributes.title["en"] ?: manga.attributes.title.values.first(),
            provider = CoreProviders.MANGADEX,
            resultId = manga.id.value
        )
    }

    private fun seriesUrl(id: MangaDexMangaId) = "$mangaDexBaseUrl/title/${id.value}"
}
