package org.snd.metadata.providers.mal

import mu.KotlinLogging
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.Provider.MAL
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.mal.model.Series.MalMediaType.DOUJINSHI
import org.snd.metadata.providers.mal.model.Series.MalMediaType.LIGHT_NOVEL
import org.snd.metadata.providers.mal.model.Series.MalMediaType.MANGA
import org.snd.metadata.providers.mal.model.Series.MalMediaType.MANHUA
import org.snd.metadata.providers.mal.model.Series.MalMediaType.MANHWA
import org.snd.metadata.providers.mal.model.Series.MalMediaType.NOVEL
import org.snd.metadata.providers.mal.model.Series.MalMediaType.OEL
import org.snd.metadata.providers.mal.model.Series.MalMediaType.ONE_SHOT
import org.snd.metadata.providers.mal.model.toSeriesSearchResult

private val logger = KotlinLogging.logger {}

private val mangaMediaTypes = listOf(MANGA, ONE_SHOT, DOUJINSHI, MANHWA, MANHUA, OEL)
private val novelMediaTypes = listOf(NOVEL, LIGHT_NOVEL)

class MalMetadataProvider(
    private val malClient: MalClient,
    private val metadataMapper: MalMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    private val seriesTypes = if (mediaType == MediaType.MANGA) mangaMediaTypes else novelMediaTypes

    override fun providerName() = MAL

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = malClient.getSeries(seriesId.id.toInt())
        val thumbnail = if (fetchSeriesCovers) malClient.getThumbnail(series) else null

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        if (seriesName.length < 3) {
            logger.warn { "$seriesName is less than 3 characters. Can't perform a search" }
            return emptyList()
        }

        return malClient.searchSeries(seriesName.take(64)).results
            .filter { seriesTypes.contains(it.mediaType) }
            .take(limit)
            .map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        if (seriesName.length < 3) {
            logger.warn { "$seriesName is less than 3 characters. Can't perform a search" }
            return null
        }

        val searchResults = malClient.searchSeries(seriesName.take(64))
        val match = searchResults.results
            .filter { seriesTypes.contains(it.mediaType) }
            .firstOrNull {
                val titles = listOfNotNull(it.title, it.alternativeTitles.en, it.alternativeTitles.ja) + it.alternativeTitles.synonyms
                nameMatcher.matches(seriesName, titles)
            }

        return match?.let {
            val series = malClient.getSeries(it.id)
            val thumbnail = if (fetchSeriesCovers) malClient.getThumbnail(series) else null
            metadataMapper.toSeriesMetadata(series, thumbnail)
        }
    }
}
