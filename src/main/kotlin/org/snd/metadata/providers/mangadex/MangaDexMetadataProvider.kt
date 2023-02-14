package org.snd.metadata.providers.mangadex

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Provider.MANGADEX
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.mangadex.model.MangaDexCoverArt
import org.snd.metadata.providers.mangadex.model.MangaDexMangaId
import org.snd.metadata.providers.mangadex.model.toSeriesSearchResult

class MangaDexMetadataProvider(
    private val client: MangaDexClient,
    private val metadataMapper: MangaDexMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {

    override fun providerName() = MANGADEX

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(MangaDexMangaId(seriesId.id))
        val cover = client.getCover(series.id, series.coverArt.fileName)
        return metadataMapper.toSeriesMetadata(series, getAllCovers(series.id), cover)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val cover = client.getCover(MangaDexMangaId(seriesId.id), bookId.id)
        return metadataMapper.toBookMetadata(bookId.id, cover)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400), limit, 0).data

        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400)).data

        return searchResults
            .firstOrNull { result ->
                val titles = result.attributes.title.values + result.attributes.altTitles.flatMap { it.values }
                nameMatcher.matches(seriesName, titles)
            }
            ?.let {
                val series = client.getSeries(it.id)
                val cover = client.getCover(series.id, series.coverArt.fileName)
                metadataMapper.toSeriesMetadata(series, getAllCovers(series.id), cover)
            }
    }

    private fun getAllCovers(mangaId: MangaDexMangaId): List<MangaDexCoverArt> {
        return generateSequence(client.getSeriesCovers(mangaId, 100, 0)) {
            if (it.offset + it.limit > it.total) null
            else client.getSeriesCovers(mangaId, it.limit, it.offset + it.limit)
        }
            .flatMap { it.data }
            .toList()
    }
}
