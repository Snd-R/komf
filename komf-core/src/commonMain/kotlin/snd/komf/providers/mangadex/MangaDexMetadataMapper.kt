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
import snd.komf.util.toStingEncoded

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
        val originalRomaji = "${manga.attributes.originalLanguage}-ro"
        // search altTitles for given key
        var originalRomajiFound = false
        val titleLang = manga.attributes.title.keys.first()
        var titleLangDuped = false

        // also check altTitles for native that's wrongly marked as "en"
        var incorrectAltTitle : Map<String, String>? = null
        for (altTitle in manga.attributes.altTitles) {
            if (altTitle.keys.contains(originalRomaji)) {
                originalRomajiFound = true
            }

            if (altTitle.keys.contains(titleLang)) {
                titleLangDuped = true
            }

            if (altTitle.keys.contains("en")) {
                altTitle["en"]?.let {
                    val detectedLanguages = LanguageCharType.detect(it)
                    if ((detectedLanguages.size == 1) && (detectedLanguages.contains(originalLang))) {
                        incorrectAltTitle = altTitle
                    }
                }
            }
        }

        var title = manga.attributes.title
        if (titleLangDuped && (originalLang == "ja")) {
            // if original romaji was not found in altTitle and title lang is duped, assume
            // title is original
            // romaji irrespective of what its language indicator says.
            if (!originalRomajiFound) {
                val name = title[titleLang]
                name?.let { title = mapOf(originalRomaji to it) }
            }
        }

        // combine title and altTitle into 1 list.
        val titleList = buildList {
            add(title)
            manga.attributes.altTitles.forEach {
                if ((incorrectAltTitle != null) && (it == incorrectAltTitle)) {
                    // We know incorrectAltTitle["en"] isn't null here because we checked it above.
                    add(mapOf(originalLang to incorrectAltTitle["en"]!!))
                } else {
                    add(it)
                }
            }
        }

        val titles = titleList
            .map { it.entries.first() }
            .map { (lang, name) ->
                when (lang) {
                    originalLang -> SeriesTitle(name, NATIVE, lang)
                    "ja-ro", "ko-ro", "zh-ro" -> SeriesTitle(name, ROMAJI, lang)
                    else -> SeriesTitle(name, LOCALIZED, lang)
                }
            }

        val links = mutableMapOf<MangaDexLink, WebLink>()
        links[MangaDexLink.MANGA_DEX] = WebLink("MangaDex", seriesUrl(manga.id))
        manga.attributes.links?.forEach { (key, value) ->
            when (key) {
                "al" -> links[ANILIST] =
                    WebLink("AniList", "https://anilist.co/manga/${value.encodeURLPath()}")

                "ap" -> links[ANIME_PLANET] =
                    WebLink("Anime-Planet", "https://www.anime-planet.com/manga/${value.encodeURLPath()}")

                "bw" -> parseUrl("https://bookwalker.jp/$value")?.let { url ->
                    links[BOOKWALKER_JP] = WebLink("BookWalkerJp", url.toStingEncoded())
                }

                "mu" -> {
                    val url = if (value.toLongOrNull() != null)
                        "https://www.mangaupdates.com/series.html?id=$value"
                    else "https://www.mangaupdates.com/series/${value.encodeURLPath()}"
                    links[MANGA_UPDATES] = WebLink("MangaUpdates", url)
                }

                "nu" -> links[NOVEL_UPDATES] = WebLink(
                    "NovelUpdates",
                    "https://www.novelupdates.com/series/${value.encodeURLPath()}"
                )

                "kt" -> links[KITSU] = WebLink("Kitsu", "https://kitsu.app/manga/${value.encodeURLPath()}")
                "amz" -> parseUrl(value)?.let { url -> links[AMAZON] = WebLink("Amazon", url.toStingEncoded()) }
                "ebj" -> {
                    val url = if (value.toIntOrNull() != null) {
                        "https://ebookjapan.yahoo.co.jp/books/${value}}"
                    } else {
                        parseUrl(value)?.toString()
                    }
                    url?.let { links[EBOOK_JAPAN] = WebLink("eBookJapan", it) }
                }

                "mal" -> links[MY_ANIME_LIST] =
                    WebLink("MyAnimeList", "https://myanimelist.net/manga/${value.encodeURLPath()}")

                "cdj" -> parseUrl(value)?.let { url -> links[CD_JAPAN] = WebLink("CDJapan", url.toStingEncoded()) }
                "raw" -> parseUrl(value)?.let { url -> links[RAW] = WebLink("Official Raw", url.toStingEncoded()) }
                "engtl" -> parseUrl(value)?.let { url ->
                    links[ENGLISH_TL] = WebLink("Official English", url.toStingEncoded())
                }
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
