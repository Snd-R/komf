package snd.komf.providers.stripinfo

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
import snd.komf.model.SeriesStatus
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.BookMetadataConfig
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.stripinfo.model.StripInfoAlbum
import snd.komf.providers.stripinfo.model.StripInfoAlbumId
import snd.komf.providers.stripinfo.model.StripInfoRole
import snd.komf.providers.stripinfo.model.StripInfoSearchResult
import snd.komf.providers.stripinfo.model.StripInfoSeries
import snd.komf.providers.stripinfo.model.StripInfoSeriesId

class StripInfoMetadataMapper(
    private val seriesMetadataConfig: SeriesMetadataConfig,
    private val bookMetadataConfig: BookMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
    private val artistRoles: Collection<AuthorRole>,
) {

    fun toSeriesSearchResult(result: StripInfoSearchResult, imageUrl: String? = null): SeriesSearchResult {
        return SeriesSearchResult(
            url = result.url,
            imageUrl = imageUrl,
            title = result.title,
            provider = CoreProviders.STRIP_INFO,
            resultId = result.id.id.toString(),
        )
    }

    fun toSeriesMetadata(series: StripInfoSeries, thumbnail: Image?): ProviderSeriesMetadata {
        val status = when {
            series.endYear != null -> SeriesStatus.ENDED
            series.startYear != null -> SeriesStatus.ONGOING
            else -> null
        }

        val metadata = SeriesMetadata(
            status = status,
            titles = listOf(SeriesTitle(series.title, null, toBcp47(series.language) ?: "nl")),
            publisher = series.publishers.firstOrNull()?.let { Publisher(it) },
            alternativePublishers = series.publishers.drop(1).map { Publisher(it) }.toSet(),
            language = toBcp47(series.language),
            readingDirection = ReadingDirection.LEFT_TO_RIGHT,
            totalBookCount = series.albums.size,
            releaseDate = series.startYear?.let { ReleaseDate(it, null, null) },
            score = series.rating,
            thumbnail = thumbnail,
            tags = series.tags,
            links = listOf(WebLink("stripINFO", seriesUrl(series.id))),
        )

        val books = series.albums.map { album ->
            SeriesBook(
                id = ProviderBookId(album.id.id.toString()),
                number = album.number?.toDoubleOrNull()?.let { BookRange(it, it) },
                name = album.title,
                type = null,
                edition = null,
            )
        }

        val providerMetadata = ProviderSeriesMetadata(
            id = ProviderSeriesId(series.id.id.toString()),
            metadata = metadata,
            books = books,
        )
        return MetadataConfigApplier.apply(providerMetadata, seriesMetadataConfig)
    }

    fun toBookMetadata(album: StripInfoAlbum, thumbnail: Image?): ProviderBookMetadata {
        val authors = album.credits.flatMap { credit ->
            when (credit.role) {
                StripInfoRole.SCENARIO -> authorRoles.map { Author(credit.name, it) }
                StripInfoRole.TEKENINGEN -> artistRoles.map { Author(credit.name, it) }
                StripInfoRole.KLEUREN -> listOf(Author(credit.name, AuthorRole.COLORIST))
                StripInfoRole.COVER -> listOf(Author(credit.name, AuthorRole.COVER))
            }
        }

        val metadata = BookMetadata(
            title = album.title,
            number = album.number?.toDoubleOrNull()?.let { BookRange(it, it) },
            releaseDate = album.year?.let { LocalDate(it, 1, 1) },
            authors = authors,
            isbn = album.barcode?.takeIf { isValidIsbn(it) },
            thumbnail = thumbnail,
            links = listOf(WebLink("stripINFO", albumUrl(album.id))),
        )

        val providerMetadata = ProviderBookMetadata(
            id = ProviderBookId(album.id.id.toString()),
            metadata = metadata,
        )
        return MetadataConfigApplier.apply(providerMetadata, bookMetadataConfig)
    }

    private fun toBcp47(language: String?): String? = when (language?.trim()) {
        "Nederlands" -> "nl"
        "Frans" -> "fr"
        "Engels" -> "en"
        "Duits" -> "de"
        "Spaans" -> "es"
        "Italiaans" -> "it"
        "Portugees" -> "pt"
        "Zweeds" -> "sv"
        "Noors" -> "no"
        "Deens" -> "da"
        "Fins" -> "fi"
        "Japans" -> "ja"
        "Koreaans" -> "ko"
        "Chinees" -> "zh"
        "Russisch" -> "ru"
        "Pools" -> "pl"
        "Tsjechisch" -> "cs"
        "Hongaars" -> "hu"
        "Grieks" -> "el"
        "Turks" -> "tr"
        null -> null
        else -> null
    }

    private fun seriesUrl(id: StripInfoSeriesId) = "$stripInfoBaseUrl/reeks/index/${id.id}"
    private fun albumUrl(id: StripInfoAlbumId) = "$stripInfoBaseUrl/reeks/strip/${id.id}"

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
