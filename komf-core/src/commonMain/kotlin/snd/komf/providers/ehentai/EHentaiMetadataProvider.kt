package snd.komf.providers.ehentai

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

class EHentaiMetadataProvider(
    private val eHentaiClient: EHentaiClient,
    private val metadataMapper: EHentaiMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
) : MetadataProvider {

    private val cache = Cache.Builder<ProviderSeriesId, EHentaiBook>()
        .expireAfterWrite(5.minutes)
        .build()

    override fun providerName() = CoreProviders.EHENTAI

    private suspend fun getBookOrThrow(seriesId: ProviderSeriesId): EHentaiBook {
        return cache.get(seriesId) {
            val response = eHentaiClient.searchByGidList(listOf(seriesId.parseEHentaiGid()))
            val book = response.gmetadata.firstOrNull() ?: throw RuntimeException("Gallery not found")
            if (book.error != null) {
                throw RuntimeException("E-Hentai API Error for $seriesId: ${book.error}")
            }
            book
        }
    }

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val book = getBookOrThrow(seriesId)
        val thumbnail = if (fetchSeriesCovers) eHentaiClient.getThumbnail(book) else null
        return metadataMapper.toSeriesMetadata(book, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val book = getBookOrThrow(seriesId)
        return eHentaiClient.getThumbnail(book)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        return eHentaiClient.searchByTitle(seriesName).gmetadata
            .filter { it.error == null && it.token != null }
            .map { result ->
                metadataMapper.toSeriesSearchResult(result)
                    .also { cache.put(ProviderSeriesId(it.resultId), result) }
            }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = eHentaiClient.searchByTitle(seriesName.take(400)).gmetadata
            .filter { it.error == null && it.token != null && it.title != null }

        return searchResults
            .firstOrNull { matchesName(seriesName, it.title!!) }
            ?.let { book ->
                val cover = if (fetchSeriesCovers) eHentaiClient.getThumbnail(book) else null
                metadataMapper.toSeriesMetadata(book, cover).also { cache.put(it.id, book) }
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
        val strippedName = name.replace("[(\\[{]([^)\\]}]+)[)\\]}]".toRegex(), "").trim()
        return strippedName.ifBlank { name }
    }
}