package org.snd.metadata.providers.yenpress

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.MediaType.MANGA
import org.snd.metadata.model.MediaType.NOVEL
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.YEN_PRESS
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.yenpress.model.YenPressBookId
import org.snd.metadata.providers.yenpress.model.toSeriesSearchResult

class YenPressMetadataProvider(
    private val client: YenPressClient,
    private val metadataMapper: YenPressMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val mediaType: MediaType,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): Provider {
        return YEN_PRESS
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getBook(YenPressBookId(seriesId.id))
        val thumbnail = if (fetchSeriesCovers) client.getBookThumbnail(series) else null

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(YenPressBookId(bookId.id))
        val thumbnail = if (fetchBookCovers) client.getBookThumbnail(bookMetadata) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(seriesName.take(400)).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(400))

        return searchResults
            .filter {
                when (mediaType) {
                    MANGA -> !it.title.contains("(light novel)")
                    NOVEL -> !it.title.contains("(manga)")
                }
            }
            .firstOrNull { nameMatcher.matches(seriesName, bookTitle(it.title)) }
            ?.let {
                val book = client.getBook(it.id)
                val thumbnail = if (fetchSeriesCovers) client.getBookThumbnail(book) else null
                metadataMapper.toSeriesMetadata(book, thumbnail)
            }
    }
}
