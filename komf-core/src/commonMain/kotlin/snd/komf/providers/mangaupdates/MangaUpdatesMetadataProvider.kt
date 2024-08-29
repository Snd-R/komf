package snd.komf.providers.mangaupdates

import snd.komf.model.Image
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import snd.komf.providers.CoreProviders.MANGA_UPDATES
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.mangaupdates.model.SeriesType.ARTBOOK
import snd.komf.providers.mangaupdates.model.SeriesType.DOUJINSHI
import snd.komf.providers.mangaupdates.model.SeriesType.FILIPINO
import snd.komf.providers.mangaupdates.model.SeriesType.FRENCH
import snd.komf.providers.mangaupdates.model.SeriesType.INDONESIAN
import snd.komf.providers.mangaupdates.model.SeriesType.MALAYSIAN
import snd.komf.providers.mangaupdates.model.SeriesType.MANGA
import snd.komf.providers.mangaupdates.model.SeriesType.MANHUA
import snd.komf.providers.mangaupdates.model.SeriesType.MANHWA
import snd.komf.providers.mangaupdates.model.SeriesType.NORDIC
import snd.komf.providers.mangaupdates.model.SeriesType.NOVEL
import snd.komf.providers.mangaupdates.model.SeriesType.OEL
import snd.komf.providers.mangaupdates.model.SeriesType.SPANISH
import snd.komf.providers.mangaupdates.model.SeriesType.THAI
import snd.komf.providers.mangaupdates.model.SeriesType.VIETNAMESE

private val mangaTypes = listOf(
    MANGA, MANHWA, MANHUA, ARTBOOK, DOUJINSHI,
    FILIPINO, INDONESIAN, THAI, VIETNAMESE, MALAYSIAN,
    OEL, NORDIC, FRENCH, SPANISH
)
private val novelTypes = listOf(NOVEL)

class MangaUpdatesMetadataProvider(
    private val client: MangaUpdatesClient,
    private val metadataMapper: MangaUpdatesMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {

    private val seriesTypes = when (mediaType) {
        MediaType.MANGA -> mangaTypes
        MediaType.NOVEL -> novelTypes
        MediaType.COMIC -> throw IllegalStateException("Comics media type is not supported")
    }

    override fun providerName() = MANGA_UPDATES

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(seriesId.value.toLong())
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series) else null
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(seriesId.value.toLong())
        return client.getThumbnail(series)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(
            seriesName.take(400), seriesTypes, 1, limit
        ).results.take(limit)

        return searchResults.map { metadataMapper.toSeriesSearchResult(it.record) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(400), seriesTypes).results.map { it.record }

        return searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.title.removeSuffix(" (Novel)")) }
            ?.let {
                val series = client.getSeries(it.id)
                val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series) else null
                metadataMapper.toSeriesMetadata(series, thumbnail)
            }
    }
}
