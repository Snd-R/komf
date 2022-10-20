package org.snd.metadata.providers.anilist

import kotlinx.coroutines.runBlocking
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.ANILIST
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesSearchResult

class AniListMetadataProvider(
    private val client: AniListClient,
    private val metadataMapper: AniListMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher
) : MetadataProvider {

    override fun providerName(): Provider {
        return ANILIST
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getMedia(seriesId.id.toInt())
        val thumbnail = client.getThumbnail(series.aniListManga)
        return metadataMapper.toSeriesMetadata(series.aniListManga, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = runBlocking {
            client.search(seriesName.take(400), limit)
        }
        return searchResults.map { metadataMapper.toSearchResult(it.aniListManga) }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.search(seriesName.take(400))

        val match = searchResults.firstOrNull {
            val titles = listOfNotNull(
                it.aniListManga.title?.english,
                it.aniListManga.title?.romaji,
                it.aniListManga.title?.native
            )

            nameMatcher.matches(seriesName, titles)
        }

        return match?.let {
            val thumbnail = client.getThumbnail(it.aniListManga)
            metadataMapper.toSeriesMetadata(it.aniListManga, thumbnail)
        }
    }
}
