package snd.komf.providers.lambiek

import kotlinx.datetime.LocalDate
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.BookMetadata
import snd.komf.model.BookRange
import snd.komf.model.Image
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.Publisher
import snd.komf.model.ReadingDirection
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesBook
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.lambiek.model.LambiekBook
import snd.komf.providers.lambiek.model.LambiekSeries
import snd.komf.providers.lambiek.model.LambiekSearchResult
import snd.komf.providers.lambiek.model.LambiekSeriesId

class LambiekMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesSearchResult(result: LambiekSearchResult, imageUrl: String? = null): SeriesSearchResult {
        return SeriesSearchResult(
            url = seriesUrl(result.id),
            imageUrl = imageUrl,
            title = result.seriesName,
            provider = CoreProviders.LAMBIEK,
            resultId = result.id.slug,
        )
    }

    fun toSeriesMetadata(series: LambiekSeries, thumbnail: Image?): ProviderSeriesMetadata {
        val language = toBcp47(series.language)
        val metadata = SeriesMetadata(
            titles = listOf(SeriesTitle(series.title, null, language ?: "nl")),
            publisher = series.publisher?.let { Publisher(it) },
            language = language,
            readingDirection = ReadingDirection.LEFT_TO_RIGHT,
            totalBookCount = series.books.size.takeIf { it > 0 },
            thumbnail = thumbnail,
            tags = series.genres,
            links = listOf(WebLink("Lambiek", seriesUrl(series.id))),
        )

        val books = series.books.map { book ->
            SeriesBook(
                id = ProviderBookId(book.id.id.toString()),
                number = book.volume?.toDoubleOrNull()?.let { BookRange(it, it) },
                name = book.title,
                type = null,
                edition = null,
            )
        }

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.slug),
            metadata = metadata,
            books = books,
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(book: LambiekBook, thumbnail: Image?): ProviderBookMetadata {
        val authors = buildList {
            book.writer?.let { name ->
                authorRoles.forEach { add(Author(name, it)) }
            }
            book.illustrator?.let { name ->
                artistRoles.forEach { add(Author(name, it)) }
            }
        }

        val releaseDate = book.releaseDate?.let { parseReleaseDate(it) }

        val metadata = BookMetadata(
            title = book.title,
            summary = book.description,
            number = book.volume?.toDoubleOrNull()?.let { BookRange(it, it) },
            releaseDate = releaseDate,
            authors = authors,
            isbn = book.isbn?.takeIf { isValidIsbn(it) },
            thumbnail = thumbnail,
            links = book.seriesId?.let {
                listOf(WebLink("Lambiek", bookUrl(it, book.id)))
            } ?: emptyList(),
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(book.id.id.toString()),
            metadata = metadata,
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    /** Parses "dd-MM-yyyy" (Lambiek format) to LocalDate. */
    private fun parseReleaseDate(raw: String): LocalDate? {
        return try {
            val parts = raw.split("-")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt()
                val year = parts[2].toInt()
                LocalDate(year, month, day)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun seriesUrl(id: LambiekSeriesId) = "$lambiekBaseUrl/shop/series/${id.slug}/"
    private fun bookUrl(seriesId: LambiekSeriesId, bookId: snd.komf.providers.lambiek.model.LambiekBookId) =
        "$lambiekBaseUrl/shop/series/${seriesId.slug}/${bookId.id}/"

    /** Maps Lambiek's English language names to BCP-47 codes. */
    private fun toBcp47(language: String?): String? = when (language?.trim()) {
        "Dutch" -> "nl"
        "French" -> "fr"
        "English" -> "en"
        "German" -> "de"
        "Spanish" -> "es"
        "Italian" -> "it"
        "Portuguese" -> "pt"
        "Swedish" -> "sv"
        "Norwegian" -> "no"
        "Danish" -> "da"
        "Finnish" -> "fi"
        "Japanese" -> "ja"
        "Korean" -> "ko"
        "Chinese" -> "zh"
        "Russian" -> "ru"
        "Polish" -> "pl"
        "Czech" -> "cs"
        "Hungarian" -> "hu"
        "Greek" -> "el"
        "Turkish" -> "tr"
        null -> null
        else -> null
    }

    private fun isValidIsbn(value: String): Boolean {
        val digits = value.replace("-", "").replace(" ", "")
        return when (digits.length) {
            10 -> {
                if (!digits.take(9).all { it.isDigit() }) return false
                if (!digits[9].isDigit() && digits[9] != 'X') return false
                val sum = digits.take(9).mapIndexed { i, c -> (10 - i) * c.digitToInt() }.sum() +
                        (if (digits[9] == 'X') 10 else digits[9].digitToInt())
                sum % 11 == 0
            }
            13 -> {
                if (!digits.all { it.isDigit() }) return false
                val sum = digits.mapIndexed { i, c -> (if (i % 2 == 0) 1 else 3) * c.digitToInt() }.sum()
                sum % 10 == 0
            }
            else -> false
        }
    }
}
