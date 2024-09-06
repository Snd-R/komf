package snd.komf.providers.hentag

import io.ktor.http.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import snd.komf.model.Author
import snd.komf.model.AuthorRole
import snd.komf.model.Image
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.ReleaseDate
import snd.komf.model.SeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.model.SeriesTitle
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig

class HentagMetadataMapper(
    private val metadataConfig: SeriesMetadataConfig,
    private val authorRoles: Collection<AuthorRole>,
) {

    fun toSeriesMetadata(book: HentagBook, cover: Image? = null): ProviderSeriesMetadata {
        val hentagLink = book.locations?.firstOrNull { it.startsWith("https://hentag.com/vault/") }
        checkNotNull(hentagLink) { "Could not find link to hentag in locations response" }
        val seriesId = ProviderSeriesId(hentagLink.removePrefix("https://hentag.com/vault/"))

        val language = when (book.language) {
            "english" -> "en"
            "chinese" -> "zh"
            "japanese" -> "ja"
            "korean" -> "ko"
            else -> null
        }
        val parodyTags = book.parodies?.map { "parody:$it" } ?: emptyList()
        val characterTags = book.characters?.map { "character:$it" } ?: emptyList()
        val groupTags = book.circles?.map { "group:$it" } ?: emptyList()
        val femaleTags = book.femaleTags?.map { "female:$it" } ?: emptyList()
        val maleTags = book.maleTags?.map { "male:$it" } ?: emptyList()
        val otherTags = book.otherTags ?: emptyList()
        val tags = parodyTags + characterTags + groupTags +
                femaleTags + maleTags + otherTags +
                "category:${book.category}"

        val links = book.locations.mapNotNull { location ->
            when {
                location.startsWith("https://hentag.com") -> WebLink("hentag", location)
                location.startsWith("https://exhentai.org") -> WebLink("exhentai", location)
                location.startsWith("https://e-hentai.org") -> WebLink("e-hentai", location)
                location.startsWith("https://www.fakku.net") -> WebLink("fakku", location)
                else -> parseUrl(location)?.host?.let { WebLink(it, location) }
            }
        }
        val metadata = SeriesMetadata(
            titles = listOf(
                SeriesTitle(
                    name = book.title,
                    type = null,
                    language = null
                )
            ),
            language = language,
            releaseDate = book.publishedOn?.toLocalDateTime(TimeZone.UTC)?.let { date ->
                ReleaseDate(
                    year = date.year,
                    month = date.monthNumber,
                    day = date.dayOfMonth
                )
            },
            tags = tags,
            authors = book.artists?.flatMap { authorRoles.map { role -> Author(it, role) } } ?: emptyList(),
            links = links,
            thumbnail = cover
        )

        return MetadataConfigApplier.apply(
            ProviderSeriesMetadata(id = seriesId, metadata = metadata),
            metadataConfig
        )
    }

    fun toSeriesSearchResult(result: HentagBook): SeriesSearchResult {
        val hentagLink = result.locations?.firstOrNull { it.startsWith("https://hentag.com/vault/") }
        checkNotNull(hentagLink) { "Could not find link to hentag in locations response" }
        val seriesId = hentagLink.removePrefix("https://hentag.com/vault/")

        return SeriesSearchResult(
            resultId = seriesId,
            url = hentagLink,
            imageUrl = result.coverImageUrl,
            title = result.title,
            provider = CoreProviders.HENTAG
        )
    }
}