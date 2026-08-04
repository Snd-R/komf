package snd.komf.providers.ehentai

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.TitleType
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.ehentai.model.EHentaiBook

class EHentaiMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val preferredLanguages: List<String>,
) {

    fun toSeriesMetadata(
        book: EHentaiBook,
        thumbnail: Image?
    ): ProviderSeriesMetadata {
        val rawTags = book.tags.orEmpty()

        val language = EHentaiTagMapper.BCP47_MAP[rawTags.find { it.startsWith("language:") }]

        val authors = rawTags.asSequence()
            .filter { it.startsWith("artist:") || it.startsWith("group:") }
            .map { it.substringAfter(":") }
            .flatMap { name -> authorRoles.map { Author(name, it) } }
            .toList()

        val finalTags = rawTags.asSequence()
            .filterNot { it.startsWith("language:") || it.startsWith("artist:") || it.startsWith("group:") }
            .map { it.substringAfter(":") }
            .distinct()
            .toList()

        val title = when {
            language == "ja" && !book.titleJpn.isNullOrBlank() -> {
                SeriesTitle(
                    EHentaiParser.parseTitle(book.titleJpn).bestMatch,
                    TitleType.NATIVE,
                    "ja"
                )
            }

            else -> {
                val type = when (language) {
                    null -> null
                    "ja" -> TitleType.ROMAJI
                    else -> TitleType.LOCALIZED
                }
                SeriesTitle(
                    EHentaiParser.parseTitle(book.title).bestMatch,
                    type,
                    language
                )
            }
        }

        val link = WebLink("e-hentai", "https://e-hentai.org/g/${book.gid}/${book.token}")

        val metadata = SeriesMetadata(
            title = title,
            language = language,
            releaseDate = book.posted?.toLocalDateTime(TimeZone.UTC)?.let {
                ReleaseDate(it.year, it.month.number, it.day)
            },
            tags = finalTags,
            authors = authors,
            links = listOf(link),
            thumbnail = thumbnail,
            score = book.rating,
            status = SeriesStatus.ENDED,
            ageRating = EHentaiTagMapper.mapAgeRating(book.category, rawTags)
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(
                id = ProviderSeriesId("${book.gid};${book.token}"),
                metadata = metadata
            ),
            metadataConfig
        )
    }

    fun toSeriesSearchResult(result: EHentaiBook): SeriesSearchResult {
        return SeriesSearchResult(
            resultId = "${result.gid};${result.token}",
            url = "https://e-hentai.org/g/${result.gid}/${result.token}",
            imageUrl = result.thumb,
            title = result.title,
            provider = CoreProviders.EHENTAI
        )
    }

    fun applyLanguagePreference(books: List<EHentaiBook>): List<EHentaiBook> {
        val validLanguages = preferredLanguages
            .filter { EHentaiTagMapper.BCP47_MAP.containsValue(it) }
            .distinct()

        if (validLanguages.isEmpty()) {
            return books
        }

        val preferredBooksGrouped = mutableMapOf<String, MutableList<EHentaiBook>>()
        validLanguages.forEach { preferredBooksGrouped[it] = mutableListOf() }

        val noLanguageBooks = mutableListOf<EHentaiBook>()

        books.forEach { book ->
            val hasLanguageTag = book.tags?.any { it.startsWith("language:") } == true
            val langTag = book.tags?.firstOrNull { it.startsWith("language:") }
            val bookLang = EHentaiTagMapper.BCP47_MAP[langTag]

            when {
                bookLang in validLanguages -> preferredBooksGrouped[bookLang]!!.add(book)
                !hasLanguageTag -> noLanguageBooks.add(book)
            }
        }

        val sortedPreferredBooks = validLanguages.flatMap { lang ->
            preferredBooksGrouped[lang]!!.sortedByDescending { it.rating ?: 0.0 }
        }

        noLanguageBooks.sortByDescending { it.rating ?: 0.0 }

        if (sortedPreferredBooks.isNotEmpty() || noLanguageBooks.isNotEmpty()) {
            return sortedPreferredBooks + noLanguageBooks
        }

        return emptyList()
    }
}