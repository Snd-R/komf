package org.snd.metadata.providers.bangumi

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.bangumi.model.SubjectType

// Manga and Novel are both considered book in Bangumi
// For now, Novel just means "everything"
// In the future, if there's other search support, Bangumi also have Anime, Music, etc.
private val mangaMediaFormats = listOf(SubjectType.BOOK)
private val allMediaFormats = listOf(SubjectType.ANIME, SubjectType.BOOK, SubjectType.MUSIC, SubjectType.GAME, SubjectType.REAL)

class BangumiMetadataProvider(
    private val client: BangumiClient,
    private val metadataMapper: BangumiMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    private val seriesFormats = if (mediaType == MediaType.MANGA) mangaMediaFormats else allMediaFormats

    override fun providerName() = Provider.BANGUMI

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(seriesId.id.toLong())
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series) else null
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResultsData = client.searchSeries(seriesName, seriesFormats).data?.take(limit)

        return searchResultsData?.map {
            SeriesSearchResult(
                imageUrl = it.image,
                provider = this.providerName(),
                resultId = it.id.toString(),
                title = it.nameCn.ifBlank { it.name },
            )
        } ?: listOf()
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName, seriesFormats)
        val match = searchResults.data?.firstOrNull {
            val titles = listOfNotNull(
                it.nameCn,
                it.name,
            )

            nameMatcher.matches(seriesName, titles)
        }

        return match?.let {
            val series = client.getSeries(it.id.toLong())
            val thumbnail = if (fetchSeriesCovers) client.getThumbnail(it.image) else null
            metadataMapper.toSeriesMetadata(
                series,
                thumbnail,
            )
        }
    }
}