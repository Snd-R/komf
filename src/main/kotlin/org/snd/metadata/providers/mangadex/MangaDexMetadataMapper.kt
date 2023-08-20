package org.snd.metadata.providers.mangadex

import org.snd.config.BookMetadataConfig
import org.snd.config.SeriesMetadataConfig
import org.snd.metadata.MetadataConfigApplier
import org.snd.metadata.model.Image
import org.snd.metadata.model.metadata.Author
import org.snd.metadata.model.metadata.AuthorRole
import org.snd.metadata.model.metadata.BookMetadata
import org.snd.metadata.model.metadata.BookRange
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.model.metadata.ReleaseDate
import org.snd.metadata.model.metadata.SeriesBook
import org.snd.metadata.model.metadata.SeriesMetadata
import org.snd.metadata.model.metadata.SeriesStatus
import org.snd.metadata.model.metadata.SeriesTitle
import org.snd.metadata.model.metadata.TitleType.LOCALIZED
import org.snd.metadata.model.metadata.TitleType.NATIVE
import org.snd.metadata.model.metadata.TitleType.ROMAJI
import org.snd.metadata.model.metadata.WebLink
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArt
import org.snd.metadata.providers.mangadex.model.MangaDexManga
import org.snd.metadata.providers.mangadex.model.MangaDexMangaStatus

class MangaDexMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {
    private val mangaDexBaseUrl = "https://mangadex.org"

    fun toSeriesMetadata(manga: MangaDexManga, covers: List<MangaDexCoverArt>, cover: Image? = null): ProviderSeriesMetadata {
        val status = when (manga.attributes.status) {
            MangaDexMangaStatus.ONGOING -> SeriesStatus.ONGOING
            MangaDexMangaStatus.COMPLETED -> SeriesStatus.ENDED
            MangaDexMangaStatus.HIATUS -> SeriesStatus.HIATUS
            MangaDexMangaStatus.CANCELLED -> SeriesStatus.ABANDONED
        }

        val authors = (manga.authors + manga.artists).flatMap {
            when (it.type) {
                "author" -> authorRoles.map { role -> Author(it.name, role) }
                else -> artistRoles.map { role -> Author(it.name, role) }
            }
        }


        val tags = manga.attributes.tags.filter { it.attributes.group == "theme" }
            .mapNotNull { it.attributes.name["en"] ?: it.attributes.name.values.firstOrNull() }
        val genres = listOfNotNull(manga.attributes.publicationDemographic?.name?.lowercase()) +
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

        val links = listOf(WebLink("MangaDex", "$mangaDexBaseUrl/title/${manga.id.id}")) +
                (manga.attributes.links?.let { link ->
                    listOfNotNull(
                        link.aniList?.let { WebLink("AniList", it) },
                        link.animePlanet?.let { WebLink("Anime-Planet", it) },
                        link.bookWalker?.let { WebLink("BookWalkerJp", it) },
                        link.mangaUpdates?.let { WebLink("MangaUpdates", it) },
                        link.novelUpdates?.let { WebLink("NovelUpdates", it) },
                        link.kitsu?.let { WebLink("Kitsu", it) },
                        link.amazon?.let { WebLink("Amazon", it) },
                        link.ebookJapan?.let {
                            WebLink("eBookJapan",
                                (it.toIntOrNull()?.let { bookId -> "https://ebookjapan.yahoo.co.jp/books/$bookId" } ?: it))
                        },
                        link.myAnimeList?.let { WebLink("MyAnimeList", it) },
                        link.cdJapan?.let { WebLink("CDJapan", it) },
                        link.raw?.let { WebLink("Official Raw", it) },
                        link.engTl?.let { WebLink("Official English", it) },
                    )
                } ?: emptyList())

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = manga.attributes.description.let { descriptionMap ->
                descriptionMap["en"] ?: descriptionMap.values.firstOrNull()
            },
            genres = genres,
            tags = tags,
            authors = authors,
            thumbnail = cover,
            releaseDate = ReleaseDate(manga.attributes.year, null, null),
            links = links,
        )

        // TODO configurable language
        val books = covers.groupBy { it.volume }.values
            .map { byVolume ->
                byVolume.groupBy { it.locale }
                    .mapValues { (_, value) -> value.firstOrNull() }
            }
            .mapNotNull { byLocale ->
                (byLocale["en"] ?: byLocale["jp"] ?: byLocale.values.firstOrNull())
                    ?.let { coverArtToBook(it) }
            }

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(
                id = ProviderSeriesId(manga.id.id),
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
            id = ProviderBookId(coverArt.fileName),
            number = coverArt.volume?.toDoubleOrNull()?.let { BookRange(it, it) },
            name = coverArt.volume,
            type = null,
            edition = null
        )
    }
}
