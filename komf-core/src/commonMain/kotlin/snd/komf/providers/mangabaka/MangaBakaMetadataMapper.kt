package snd.komf.providers.mangabaka

import com.fleeksoft.ksoup.Ksoup
import io.ktor.http.parseUrl
import kotlinx.datetime.number
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
import snd.komf.model.WebLink
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataConfigApplier
import snd.komf.providers.SeriesMetadataConfig
import snd.komf.providers.mangabaka.MangaBakaTitleTrait.NATIVE
import snd.komf.util.toStingEncoded


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


        val allTitles = series.titles?.sortedByDescending { it.isPrimary } ?: emptyList()
        val nativeTitle = allTitles.firstOrNull { title ->
            title.traits.any { it == NATIVE } && !title.language.endsWith("-Latn")
        }
        val titles = allTitles.sortedByDescending { it.isPrimary }.map { title ->
            val romanized = title.title.endsWith("-Latn")
            SeriesTitle(
                name = title.title,
                type = when {
                    title == nativeTitle -> TitleType.NATIVE
                    romanized -> TitleType.ROMAJI
                    else -> TitleType.LOCALIZED
                },
                language = title.language.replace("-Latn", "-ro")
            )
        }

        val publisher = if (metadataConfig.useOriginalPublisher) originalPublishers.firstOrNull()
        else englishPublishers.firstOrNull() ?: originalPublishers.firstOrNull()

        val links = series.linksV2?.mapNotNull { link ->
            parseUrl(link.url)?.let { url ->
                WebLink(
                    link.nameDisplay,
                    url.toStingEncoded()
                )
            }
        }?.sortedBy { it.label } ?: emptyList()

        val allTags = series.tagsV2 ?: emptyList()
        val genres = allTags.filter { it.isGenre }.map { it.name }
        val tags = allTags.filterNot { it.isGenre }.map { it.name }

        val metadata = SeriesMetadata(
            status = status,
            titles = titles,
            summary = series.description?.let { Ksoup.parse(it).wholeText() },
            publisher = publisher,
            alternativePublishers = (originalPublishers + englishPublishers) - setOfNotNull(publisher),
            genres = genres,
            tags = tags,
            totalBookCount = series.finalVolume?.toIntOrNull(),
            authors = authors + artists,
            thumbnail = thumbnail,
            releaseDate = ReleaseDate(
                series.published?.startDate?.year,
                series.published?.startDate?.month?.number,
                series.published?.startDate?.day
            ),
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
            imageUrl = series.cover.x350?.x1,
            title = getPrimaryTitle(series),
            provider = CoreProviders.MANGA_BAKA,
            resultId = series.id.value.toString()
        )
    }

    private fun getPrimaryTitle(series: MangaBakaSeries): String {
        if (series.titles == null) return ""
        val nativeTitle = series.titles.firstOrNull { title -> title.traits.any { it == NATIVE } }
        if (nativeTitle != null) return nativeTitle.title
        val primaryEnglish = series.titles.firstOrNull { it.language == "en" && it.isPrimary == true }
        if (primaryEnglish != null) return primaryEnglish.title

        return series.titles.first().title
    }
}