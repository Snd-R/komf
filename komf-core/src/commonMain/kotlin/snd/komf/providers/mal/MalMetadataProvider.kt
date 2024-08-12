package snd.komf.providers.mal

import io.github.oshai.kotlinlogging.KotlinLogging
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import snd.komf.providers.CoreProviders.MAL
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.mal.model.MalMediaType.DOUJINSHI
import snd.komf.providers.mal.model.MalMediaType.LIGHT_NOVEL
import snd.komf.providers.mal.model.MalMediaType.MANGA
import snd.komf.providers.mal.model.MalMediaType.MANHUA
import snd.komf.providers.mal.model.MalMediaType.MANHWA
import snd.komf.providers.mal.model.MalMediaType.NOVEL
import snd.komf.providers.mal.model.MalMediaType.OEL
import snd.komf.providers.mal.model.MalMediaType.ONE_SHOT

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
    private val seriesTypes = when (mediaType) {
        MediaType.MANGA -> mangaMediaTypes
        MediaType.NOVEL -> novelMediaTypes
        MediaType.COMIC -> throw IllegalStateException("Comics media type is not supported")
    }

    override fun providerName() = MAL

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = malClient.getSeries(seriesId.value.toInt())
        val thumbnail = if (fetchSeriesCovers) malClient.getThumbnail(series) else null

        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        if (seriesName.length < 3) {
            logger.warn { "$seriesName is less than 3 characters. Can't perform a search" }
            return emptyList()
        }

        return malClient.searchSeries(seriesName.take(64)).data
            .filter { seriesTypes.contains(it.node.mediaType) }
            .take(limit)
            .map { metadataMapper.toSeriesSearchResult(it.node) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        if (seriesName.length < 3) {
            logger.warn { "$seriesName is less than 3 characters. Can't perform a search" }
            return null
        }

        val searchResults = malClient.searchSeries(seriesName.take(64))
        val match = searchResults.data
            .map { it.node }
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
