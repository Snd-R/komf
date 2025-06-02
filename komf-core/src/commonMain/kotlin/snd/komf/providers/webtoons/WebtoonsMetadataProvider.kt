package snd.komf.providers.webtoons

import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.providers.webtoons.model.WebtoonsChapterId
import snd.komf.providers.webtoons.model.WebtoonsSeriesId
import snd.komf.util.NameSimilarityMatcher

class WebtoonsMetadataProvider(
    private val client: WebtoonsClient,
    private val metadataMapper: WebtoonsMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    override fun providerName() = CoreProviders.WEBTOOONS

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val seriesWithChapters = client.getSeriesWithChapters(WebtoonsSeriesId(seriesId.value))
        val thumbnail = if (fetchSeriesCovers) client.getSeriesThumbnail(seriesWithChapters) else null

        return metadataMapper.toSeriesMetadata(seriesWithChapters, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(WebtoonsSeriesId(seriesId.value))
        return client.getSeriesThumbnail(series)
    }

    override suspend fun getBookMetadata(
        seriesId: ProviderSeriesId, bookId: ProviderBookId
    ): ProviderBookMetadata {
        val chapters = client.getChapters(WebtoonsSeriesId(seriesId.value))

        // Assume that if you're searching with a bookId, that book exists
        // This can probably fail since Webtoons eventually hides the chapter list for web clients
        // The app seems to contact somewhere else, and is able to get the full list
        val chapter = chapters.find { it.id == WebtoonsChapterId(bookId.id) }!!

        val thumbnail = if (fetchBookCovers) client.getChapterThumbnail(chapter) else null
        return metadataMapper.toBookMetadata(chapter, seriesId, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(400))
        val match = searchResults.firstOrNull { nameMatcher.matches(seriesName, listOfNotNull(it.title)) }

        return match?.let {
            val series = client.getSeriesWithChapters(it.id)
            val thumbnail = if (fetchSeriesCovers) client.getSeriesThumbnail(series) else null
            metadataMapper.toSeriesMetadata(series, thumbnail)
        }
    }
}