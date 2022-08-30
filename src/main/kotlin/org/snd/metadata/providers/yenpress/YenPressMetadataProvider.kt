package org.snd.metadata.providers.yenpress

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.YEN_PRESS
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.providers.yenpress.model.YenPressBookId
import org.snd.metadata.providers.yenpress.model.toSeriesSearchResult

class YenPressMetadataProvider(
    private val client: YenPressClient,
    private val metadataMapper: YenPressMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {

    override fun providerName(): Provider {
        return YEN_PRESS
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getBook(YenPressBookId(seriesId.id))
        val thumbnail = client.getBookThumbnail(series)

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(YenPressBookId(bookId.id))
        val thumbnail = client.getBookThumbnail(bookMetadata)

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400))

        return searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.title) }
            ?.let {
                val book = client.getBook(it.id)
                val thumbnail = client.getBookThumbnail(book)
                return metadataMapper.toSeriesMetadata(book, thumbnail)
            }
    }
}
