package snd.komf.providers.ehentai

import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.providers.ehentai.model.EHentaiBook
import snd.komf.util.NameSimilarityMatcher
import kotlin.time.Duration.Companion.minutes

class EHentaiMetadataProvider(
    private val eHentaiClient: EHentaiClient,
    private val metadataMapper: EHentaiMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean
) : MetadataProvider {

    private val cache = Cache.Builder<ProviderSeriesId, EHentaiBook>()
        .expireAfterWrite(5.minutes)
        .build()

    override fun providerName() = CoreProviders.EHENTAI

    private suspend fun getBookOrThrow(seriesId: ProviderSeriesId): EHentaiBook {
        return cache.get(seriesId) {
            val response = eHentaiClient.searchByGidList(listOf(EHentaiParser.parseGid(seriesId)))
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
        val queries = EHentaiParser.getSearchQueries(seriesName)

        val rawResults = kotlinx.coroutines.coroutineScope {
            queries.map { query ->
                async { eHentaiClient.searchByTitle(query.take(400)).gmetadata }
            }.awaitAll().flatten()
        }.filter { it.error == null }.distinctBy { it.gid }

        val processedResults = metadataMapper.applyLanguagePreference(rawResults)

        return processedResults.map { result ->
            metadataMapper.toSeriesSearchResult(result)
                .also { cache.put(ProviderSeriesId(it.resultId), result) }
        }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        // Usually downloaded books from E-Hentai have complete title name.
        // We assume that user have not renamed their books and directly put them into the library.
        // So this method is based on book's title and use regex to search the gallery.
        val searchName = matchQuery.bookQualifier?.name ?: matchQuery.seriesName

        val searchResults = kotlinx.coroutines.coroutineScope {
            EHentaiParser.getSearchQueries(searchName).map { query ->
                async { eHentaiClient.searchByTitle(query.take(400)).gmetadata }
            }.awaitAll().flatten()
        }
            .filter { it.error == null }
            .distinctBy { it.gid }

        val processedResults = metadataMapper.applyLanguagePreference(searchResults)

        return processedResults
            .firstOrNull { book ->
                val matchTitle = matchesName(searchName, book.title)
                val matchTitleJpn = book.titleJpn?.let { matchesName(searchName, it) } ?: false
                matchTitle || matchTitleJpn
            }
            ?.let { book ->
                val cover = if (fetchSeriesCovers) eHentaiClient.getThumbnail(book) else null
                metadataMapper.toSeriesMetadata(book, cover).also { cache.put(it.id, book) }
            }
    }

    private fun matchesName(name: String, nameToMatch: String): Boolean {
        val localVariants = EHentaiParser.getSearchQueries(name)
        val remoteVariants = EHentaiParser.getSearchQueries(nameToMatch)
        return localVariants.any { local -> nameMatcher.matches(local, remoteVariants) }
    }
}