package snd.komf.providers.mangabaka.local

import io.ktor.client.HttpClient
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.MediaType
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher

class MangaBakaDBMetadataProvider(
    private val repository: MangaBakaRepository,
    private val nameMatcher: NameSimilarityMatcher,
    private val ktor: HttpClient,
    private val fetchSeriesCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    override fun providerName(): CoreProviders = CoreProviders.MANGA_BAKA_LOCAL

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        TODO("Not yet implemented")
    }

    override suspend fun getBookMetadata(
        seriesId: ProviderSeriesId,
        bookId: ProviderBookId
    ): ProviderBookMetadata {
        TODO("Not yet implemented")
    }

    override suspend fun searchSeries(
        seriesName: String,
        limit: Int
    ): Collection<SeriesSearchResult> {
        TODO("Not yet implemented")
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        TODO("Not yet implemented")
    }
}