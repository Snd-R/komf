package snd.komf.providers.nautiljon

import snd.komf.model.Image
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.NAUTILJON
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.nautiljon.model.NautiljonSeriesId
import snd.komf.providers.nautiljon.model.NautiljonVolumeId

class NautiljonMetadataProvider(
    private val client: NautiljonClient,
    private val metadataMapper: NautiljonSeriesMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): CoreProviders {
        return NAUTILJON
    }

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(NautiljonSeriesId(seriesId.value))
        val thumbnail = if (fetchSeriesCovers) client.getSeriesThumbnail(series) else null

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(NautiljonSeriesId(seriesId.value))
        return client.getSeriesThumbnail(series)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(NautiljonSeriesId(seriesId.value), NautiljonVolumeId(bookId.id))
        val thumbnail = if (fetchBookCovers) client.getVolumeThumbnail(bookMetadata) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(400))
        val match = searchResults
            .firstOrNull { nameMatcher.matches(seriesName, listOfNotNull(it.title, it.alternativeTitle)) }

        return match?.let {
            val series = client.getSeries(it.id)
            val thumbnail = if (fetchSeriesCovers) client.getSeriesThumbnail(series) else null
            metadataMapper.toSeriesMetadata(series, thumbnail)
        }
    }
}
