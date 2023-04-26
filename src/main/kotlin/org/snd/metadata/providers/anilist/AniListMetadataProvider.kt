package org.snd.metadata.providers.anilist

import kotlinx.coroutines.runBlocking
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.Provider.ANILIST
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.type.MediaFormat.MANGA
import org.snd.type.MediaFormat.NOVEL
import org.snd.type.MediaFormat.ONE_SHOT

private val mangaMediaFormats = listOf(MANGA, ONE_SHOT)
private val novelMediaFormats = listOf(NOVEL)

class AniListMetadataProvider(
    private val client: AniListClient,
    private val metadataMapper: AniListMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    private val seriesFormats = when (mediaType) {
        MediaType.MANGA -> mangaMediaFormats
        MediaType.NOVEL -> novelMediaFormats
        MediaType.COMIC -> throw IllegalStateException("Comics media type is not supported")
    }

    override fun providerName() = ANILIST

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getMedia(seriesId.id.toInt())
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series.aniListManga) else null
        return metadataMapper.toSeriesMetadata(series.aniListManga, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = runBlocking {
            client.search(seriesName.take(400), seriesFormats, limit)
        }
        return searchResults.map { metadataMapper.toSearchResult(it.aniListManga) }
    }

    override fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.search(seriesName.take(400), seriesFormats)

        val match = searchResults.firstOrNull {
            val titles = listOfNotNull(
                it.aniListManga.title?.english,
                it.aniListManga.title?.romaji,
                it.aniListManga.title?.native
            )

            nameMatcher.matches(seriesName, titles)
        }

        return match?.let {
            val thumbnail = if (fetchSeriesCovers) client.getThumbnail(it.aniListManga) else null
            metadataMapper.toSeriesMetadata(it.aniListManga, thumbnail)
        }
    }
}
