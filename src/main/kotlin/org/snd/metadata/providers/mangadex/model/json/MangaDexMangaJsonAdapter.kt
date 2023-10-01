package org.snd.metadata.providers.mangadex.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.metadata.providers.mangadex.model.MangaDexAttributes
import org.snd.metadata.providers.mangadex.model.MangaDexAuthor
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArt
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArtId
import org.snd.metadata.providers.mangadex.model.MangaDexLinks
import org.snd.metadata.providers.mangadex.model.MangaDexManga
import org.snd.metadata.providers.mangadex.model.MangaDexMangaId
import org.snd.metadata.providers.mangadex.model.MangaDexMangaStatus
import org.snd.metadata.providers.mangadex.model.MangaDexPublicationDemographic
import java.time.ZonedDateTime

class MangaDexMangaJsonAdapter {
    @FromJson
    fun fromJson(json: MangaDexMangaJson): MangaDexManga {
        return MangaDexManga(
            id = MangaDexMangaId(json.id),
            type = json.type,
            attributes = attributes(json.attributes),
            authors = authors(json.relationships, "author"),
            artists = authors(json.relationships, "artist"),
            coverArt = coverArt(json.relationships)
        )
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") manga: MangaDexManga): MangaDexMangaJson {
        throw UnsupportedOperationException()
    }

    private fun authors(relationships: List<MangaDexRelationshipJson>, type: String): List<MangaDexAuthor> {
        return relationships.filter { it.type == type }
            .filter { (it.attributes != null && it.attributes["name"] != null) }
            .map {
                MangaDexAuthor(
                    id = it.id,
                    type = it.type,
                    name = it.attributes!!["name"] as String
                )
            }
    }

    private fun coverArt(relationships: List<MangaDexRelationshipJson>): MangaDexCoverArt? {
        return relationships.firstOrNull() { it.type == "cover_art" }
            ?.let { relationship ->
                MangaDexCoverArt(
                    id = MangaDexCoverArtId(relationship.id),
                    description = relationship.attributes!!["description"]!! as String,
                    volume = relationship.attributes["volume"]?.let { it as String },
                    fileName = relationship.attributes["fileName"]!! as String,
                    locale = relationship.attributes["locale"]!! as String,
                    createdAt = ZonedDateTime.parse(relationship.attributes["createdAt"]!! as String),
                    updatedAt = ZonedDateTime.parse(relationship.attributes["createdAt"]!! as String),
                    version = (relationship.attributes["version"]!! as Double).toInt()
                )
            }
    }

    private fun attributes(json: MangaDexAttributesJson): MangaDexAttributes {
        return MangaDexAttributes(
            title = json.title,
            altTitles = json.altTitles,
            description = json.description,
            isLocked = json.isLocked,
            links = json.links?.let { links(it) },
            originalLanguage = json.originalLanguage,
            lastVolume = json.lastVolume,
            lastChapter = json.lastChapter,
            publicationDemographic = json.publicationDemographic?.let { MangaDexPublicationDemographic.valueOf(it.uppercase()) },
            status = MangaDexMangaStatus.valueOf(json.status.uppercase()),
            year = json.year,
            contentRating = json.contentRating,
            tags = json.tags,
            state = json.state,
            chapterNumbersResetOnNewVolume = json.chapterNumbersResetOnNewVolume,
            createdAt = json.createdAt,
            updatedAt = json.updatedAt,
            version = json.version,
            availableTranslatedLanguages = json.availableTranslatedLanguages,
            latestUploadedChapter = json.latestUploadedChapter
        )
    }

    private fun links(linksMap: Map<String, String>): MangaDexLinks {
        return linksMap.entries
            .map { (key, value) -> key to value.trim() }
            .fold(MangaDexLinks.Builder()) { builder, (key, value) ->
                when (key) {
                    "al" -> builder.apply { aniList = "https://anilist.co/manga/$value" }
                    "ap" -> builder.apply { animePlanet = "https://www.anime-planet.com/manga/$value" }
                    "bw" -> builder.apply { bookWalker = "https://bookwalker.jp/$value" }
                    "mu" -> builder.apply { mangaUpdates = "https://www.mangaupdates.com/series.html?id=$value" }
                    "nu" -> builder.apply { novelUpdates = "https://www.novelupdates.com/series/$value" }
                    "kt" -> builder.apply {
                        kitsu = value.toIntOrNull()?.let { "https://kitsu.io/manga/$it" }
                            ?: "https://kitsu.io/manga/$value"
                    }

                    "amz" -> builder.apply { amazon = value }
                    "ebj" -> builder.apply { ebookJapan = value }
                    "mal" -> builder.apply { myAnimeList = "https://myanimelist.net/manga/$value" }
                    "cdj" -> builder.apply { cdJapan = value }
                    "raw" -> builder.apply { raw = value }
                    "engtl" -> builder.apply { engTl = value }
                    else -> builder
                }
            }.build()
    }
}
