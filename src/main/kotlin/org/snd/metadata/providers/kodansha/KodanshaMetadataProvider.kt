package org.snd.metadata.providers.kodansha

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Image
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.kodansha.model.KodanshaBookId
import org.snd.metadata.providers.kodansha.model.KodanshaSeriesId
import org.snd.metadata.providers.kodansha.model.toSeriesSearchResult

class KodanshaMetadataProvider(
    private val client: KodanshaClient,
    private val metadataMapper: KodanshaMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): Provider {
        return Provider.KODANSHA
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(KodanshaSeriesId(seriesId.id.toInt()))
        val thumbnail = if (fetchSeriesCovers) getThumbnail(series.thumbnails?.firstOrNull()?.url) else null
        val bookList = client.getAllSeriesBooks(KodanshaSeriesId(series.id))
        return metadataMapper.toSeriesMetadata(series, bookList, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(KodanshaBookId(bookId.id.toInt())).response
        val thumbnail = if (fetchBookCovers) getThumbnail(bookMetadata.thumbnails.firstOrNull()?.url) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.search(sanitizeSearchInput(seriesName)).response.take(limit)
        return searchResults
            .filter { it.type == "series" }
            .map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.search(sanitizeSearchInput(seriesName)).response

        return searchResults
            .filter { it.type == "series" }
            .filter { it.content.readableUrl != null }
            .firstOrNull { nameMatcher.matches(seriesName, it.content.title.removeSuffix(" (manga)")) }
            ?.let {
                val series = client.getSeries(KodanshaSeriesId(it.content.id))
                val thumbnail = if (fetchSeriesCovers) getThumbnail(series.thumbnails?.firstOrNull()?.url) else null
                val bookList = client.getAllSeriesBooks(KodanshaSeriesId(series.id))
                metadataMapper.toSeriesMetadata(series, bookList, thumbnail)
            }
    }

    private fun getThumbnail(url: String?): Image? {
        if (url == null || url.contains("kodansha_placeholder")) return null

        return client.getThumbnail(
            url.toHttpUrl().newBuilder()
                .addQueryParameter("w", "1000")
                .addQueryParameter("f", "webp")
                .build()
        )
    }

    private fun sanitizeSearchInput(input: String): String {
        return input.take(300)
            .replace("\"", "")
    }
}
