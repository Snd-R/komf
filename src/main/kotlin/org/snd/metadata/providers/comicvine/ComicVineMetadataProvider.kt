package org.snd.metadata.providers.comicvine

import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.helper.ValidationException
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.Provider.COMIC_VINE
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.comicvine.model.ComicVineResult
import org.snd.metadata.providers.comicvine.model.ComicVineVolume
import org.snd.metadata.providers.comicvine.model.toComicVineIssueId
import org.snd.metadata.providers.comicvine.model.toComicVineVolumeId

private val logger = KotlinLogging.logger {}

class ComicVineMetadataProvider(
    private val client: ComicVineClient,
    private val mapper: ComicVineMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {
    private val startYearRegex = "\\((?<startYear>\\d{4})(-\\d{4})?\\)".toRegex()

    override fun providerName() = COMIC_VINE

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = handleResult(client.getVolume(seriesId.toComicVineVolumeId()))
        val cover = series.image?.medium_url?.let { client.getCover(it.toHttpUrl()) }
        return mapper.toSeriesMetadata(series, cover)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val issue = handleResult(client.getIssue(bookId.toComicVineIssueId()))
        val cover = issue.image?.medium_url?.let { client.getCover(it.toHttpUrl()) }
        return mapper.toBookMetadata(issue, cover)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val result = handleResult(client.searchVolume(seriesName.take(400)))

        return result.map { mapper.toSeriesSearchResult(it) }
    }

    override fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = removeParentheses(matchQuery.seriesName)
        val searchResults = handleResult(client.searchVolume(seriesName.take(400)))

        val results = searchResults
            .filter { resultMatchFilter(matchQuery, it) }

        if (results.size > 1) {
            logger.info { "Multiple series matches: ${results.joinToString { "${it.name} (${it.id})" }}. Skipping" }
            return null
        }
        return results.firstOrNull()?.let { mapper.toSeriesMetadata(it, null) }
    }

    private fun extractYear(seriesName: String): Int? {
        return startYearRegex.find(seriesName)?.groups?.get("startYear")?.value?.toIntOrNull()
    }

    private fun removeParentheses(seriesName: String): String {
        return seriesName.replace("[(\\[{]([^)\\]}]+)[)\\]}]".toRegex(), "").trim()
    }

    private fun resultMatchFilter(matchQuery: MatchQuery, result: ComicVineVolume): Boolean {
        val startYear = matchQuery.startYear ?: extractYear(matchQuery.seriesName)
        val seriesName = removeParentheses(matchQuery.seriesName)

        val nameMatch = nameMatcher.matches(seriesName, result.name)

        if (!nameMatch) return false
        if (startYear == null || result.start_year == null) return true
        return startYear == result.start_year.toIntOrNull()
    }

    private inline fun <reified T : Any> handleResult(result: ComicVineResult<T>): T {
        if (result.error != "OK")
            throw ValidationException("Comic Vine returned error response. status code ${result.status_code}")
        return result.results
    }
}