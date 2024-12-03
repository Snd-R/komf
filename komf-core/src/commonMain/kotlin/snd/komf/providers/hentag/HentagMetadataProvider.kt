package snd.komf.providers.hentag

import io.github.reactivecircus.cache4k.Cache
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.util.NameSimilarityMatcher
import kotlin.time.Duration.Companion.minutes

class HentagMetadataProvider(
    private val hentagClient: HentagClient,
    private val metadataMapper: HentagMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
) : MetadataProvider {

    private val cache = Cache.Builder<ProviderSeriesId, HentagBook>()
        .expireAfterWrite(5.minutes)
        .build()

    override fun providerName() = CoreProviders.HENTAG

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val book = cache.get(seriesId) { hentagClient.searchByIds(listOf(seriesId.value)).first() }
        val cover = if (fetchSeriesCovers) hentagClient.getCover(book) else null
        return metadataMapper.toSeriesMetadata(book, cover)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val book = cache.get(seriesId) { hentagClient.searchByIds(listOf(seriesId.value)).first() }
        return hentagClient.getCover(book)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        return hentagClient.searchByTitle(seriesName)
            .map { result ->
                metadataMapper.toSeriesSearchResult(result)
                    .also { cache.put(ProviderSeriesId(it.resultId), result) }
            }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = hentagClient.searchByTitle(seriesName.take(400))

        return searchResults
            .firstOrNull { matchesName(seriesName, it.title) }
            ?.let { series ->
                val cover = if (fetchSeriesCovers) hentagClient.getCover(series) else null
                metadataMapper.toSeriesMetadata(series, cover).also { cache.put(it.id, series) }
            }
    }

    private fun matchesName(name: String, nameToMatch: String): Boolean {
        return nameMatcher.matches(name, nameToMatch) ||
                nameMatcher.matches(
                    removeParentheses(name),
                    removeParentheses(nameToMatch)
                )
    }

    private fun removeParentheses(name: String): String {
        return name.replace("[(\\[{]([^)\\]}]+)[)\\]}]".toRegex(), "").trim()
    }
}