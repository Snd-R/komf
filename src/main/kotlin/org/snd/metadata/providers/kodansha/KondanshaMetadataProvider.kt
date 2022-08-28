package org.snd.metadata.providers.kodansha

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Provider
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.Thumbnail
import org.snd.metadata.providers.kodansha.model.KodanshaBookId
import org.snd.metadata.providers.kodansha.model.KodanshaSeriesId
import org.snd.metadata.providers.kodansha.model.toSeriesSearchResult

class KondanshaMetadataProvider(
    private val client: KodanshaClient,
    private val metadataMapper: KodanshaMetadataMapper,
) : MetadataProvider {

    override fun providerName(): Provider {
        return Provider.KODANSHA
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(KodanshaSeriesId(seriesId.id))
        val thumbnail = getThumbnail(series.coverUrl)
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(KodanshaBookId(bookId.id))
        val thumbnail = getThumbnail(bookMetadata.coverUrl)

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400))

        return searchResults.firstOrNull { NameSimilarityMatcher.matches(seriesName, it.title) }
            ?.let {
                val series = client.getSeries(it.seriesId)
                val thumbnail = getThumbnail(series.coverUrl)
                metadataMapper.toSeriesMetadata(series, thumbnail)
            }
    }

    private fun getThumbnail(url: String?): Thumbnail? = url?.toHttpUrl()?.let { client.getThumbnail(it) }
}
