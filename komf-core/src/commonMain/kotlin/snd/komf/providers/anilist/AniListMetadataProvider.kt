package snd.komf.providers.anilist

import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.MetadataProvider
import snd.komf.providers.CoreProviders.ANILIST
import snd.komf.providers.anilist.model.AniListMediaFormat
import snd.komf.util.NameSimilarityMatcher

private val mangaMediaFormats = listOf(AniListMediaFormat.MANGA, AniListMediaFormat.ONE_SHOT)
private val novelMediaFormats = listOf(AniListMediaFormat.NOVEL)

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

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getMedia(seriesId.value.toInt())
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series) else null
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.search(seriesName.take(400), seriesFormats, limit)
        return searchResults.map { metadataMapper.toSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.search(seriesName.take(400), seriesFormats)

        val match = searchResults.firstOrNull {
            val titles = listOfNotNull(
                it.title?.english,
                it.title?.romaji,
                it.title?.native
            )

            nameMatcher.matches(seriesName, titles)
        }

        return match?.let {
            val thumbnail = if (fetchSeriesCovers) client.getThumbnail(it) else null
            metadataMapper.toSeriesMetadata(it, thumbnail)
        }
    }
}
