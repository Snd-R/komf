package snd.komf.providers.mangadex

import snd.komf.model.Image
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import snd.komf.providers.CoreProviders.MANGADEX
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.mangadex.model.MangaDexCoverArt
import snd.komf.providers.mangadex.model.MangaDexMangaId

class MangaDexMetadataProvider(
    private val client: MangaDexClient,
    private val metadataMapper: MangaDexMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName() = MANGADEX

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(MangaDexMangaId(seriesId.value))
        val cover = if (fetchSeriesCovers) {
            series.getCoverArt()?.let { client.getCover(series.id, it.attributes.fileName) }
        } else null

        return metadataMapper.toSeriesMetadata(series, getAllCovers(series.id), cover)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(MangaDexMangaId(seriesId.value))
        return series.getCoverArt()?.let { client.getCover(series.id, it.attributes.fileName) }
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val cover = if (fetchBookCovers) client.getCover(MangaDexMangaId(seriesId.value), bookId.id) else null
        return metadataMapper.toBookMetadata(bookId.id, cover)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400), limit, 0).data

        return searchResults.map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(400)).data

        return searchResults
            .firstOrNull { result ->
                val titles = result.attributes.title.values + result.attributes.altTitles.flatMap { it.values }
                nameMatcher.matches(seriesName, titles)
            }
            ?.let {
                val series = client.getSeries(it.id)
                val cover = if (fetchSeriesCovers)
                    series.getCoverArt()?.let { coverArt -> client.getCover(series.id, coverArt.attributes.fileName) }
                else null
                metadataMapper.toSeriesMetadata(series, getAllCovers(series.id), cover)
            }
    }

    private suspend fun getAllCovers(mangaId: MangaDexMangaId): List<MangaDexCoverArt> {
        val covers = mutableListOf<MangaDexCoverArt>()
        var offset = 0
        var requestCount = 0
        while (requestCount < 100) {
            val page = client.getSeriesCovers(mangaId, 100, offset)
            covers.addAll(page.data)
            if (page.offset + page.limit > page.total) break
            offset += page.limit
            requestCount++
        }
        return covers
    }
}
