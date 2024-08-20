package snd.komf.providers.comicvine

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.plugins.*
import io.ktor.http.*
import snd.komf.model.BookQualifier
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders.COMIC_VINE
import snd.komf.providers.MetadataProvider
import snd.komf.providers.comicvine.model.ComicVineImage
import snd.komf.providers.comicvine.model.ComicVineIssueId
import snd.komf.providers.comicvine.model.ComicVineSearchResult
import snd.komf.providers.comicvine.model.ComicVineStoryArcId
import snd.komf.providers.comicvine.model.ComicVineVolumeId
import snd.komf.providers.comicvine.model.ComicVineVolumeSearch
import snd.komf.providers.comicvine.model.toComicVineIssueId
import snd.komf.providers.comicvine.model.toComicVineVolumeId
import snd.komf.util.NameSimilarityMatcher
import snd.komf.util.compareImages

private val logger = KotlinLogging.logger {}

class ComicVineMetadataProvider(
    private val client: ComicVineClient,
    private val mapper: ComicVineMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {
    private val startYearRegex = "\\((?<startYear>\\d{4})(-\\d{4})?\\)".toRegex()

    override fun providerName() = COMIC_VINE

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = handleResult(client.getVolume(seriesId.toComicVineVolumeId()))
        val cover = series.image?.let { getCover(it) }
        return mapper.toSeriesMetadata(series, cover)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val issue = handleResult(client.getIssue(bookId.toComicVineIssueId()))
        val storyArcs = issue.storyArcCredits?.let { credits ->
            credits.map { arc -> client.getStoryArc(ComicVineStoryArcId(arc.id)).results }
        } ?: emptyList()
        val cover = issue.image?.let { getCover(it) }
        return mapper.toBookMetadata(issue, storyArcs, cover)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val result = handleResult(client.searchVolume(seriesName.take(400)))

        return result.map { mapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = removeParentheses(matchQuery.seriesName)
        val searchResults = handleResult(client.searchVolume(seriesName.take(400)))

        val results = searchResults
            .filter { resultMatchFilter(matchQuery, it) }

        if (results.size > 1) {
            logger.info { "Multiple series matches: ${results.joinToString { "\"${it.name}\" id=${it.id}" }}" }
            if (matchQuery.bookQualifier == null) return null

            logger.info { "Attempting to match using cover of book number: ${matchQuery.bookQualifier.number} name: \"${matchQuery.bookQualifier.name}\"" }
            return results
                .firstOrNull { matchesBookCover(it, matchQuery.bookQualifier) }
                ?.let { handleResult(client.getVolume(ComicVineVolumeId(it.id))) }
                ?.let { mapper.toSeriesMetadata(it, null) }
        }
        return results.firstOrNull()
            ?.let { handleResult(client.getVolume(ComicVineVolumeId(it.id))) }
            ?.let { mapper.toSeriesMetadata(it, null) }
    }

    private suspend fun matchesBookCover(volume: ComicVineVolumeSearch, qualifier: BookQualifier): Boolean {
        val firstIssueNumber = volume.firstIssue?.issueNumber?.toDoubleOrNull() ?: return false
        val qualifierImage = qualifier.cover ?: return false

        logger.info { "matching cover of volume \"${volume.name}\" ${volume.siteDetailUrl}" }

        // TODO get issue list and find matching number
        if (qualifier.number.start != firstIssueNumber) return false

        val issue = handleResult(client.getIssue(ComicVineIssueId(volume.firstIssue.id)))
        val issueCover = issue.image?.let { getCover(it) } ?: return false
        return compareImages(qualifierImage.image, issueCover.image)
    }

    private fun extractYear(seriesName: String): Int? {
        return startYearRegex.find(seriesName)?.groups?.get("startYear")?.value?.toIntOrNull()
    }

    private fun removeParentheses(seriesName: String): String {
        return seriesName.replace("[(\\[{]([^)\\]}]+)[)\\]}]".toRegex(), "").trim()
    }

    private fun resultMatchFilter(matchQuery: MatchQuery, result: ComicVineVolumeSearch): Boolean {
        val startYear = matchQuery.startYear ?: extractYear(matchQuery.seriesName)
        val seriesName = removeParentheses(matchQuery.seriesName)

        val nameMatch = nameMatcher.matches(seriesName, result.name)

        if (!nameMatch) return false
        if (startYear == null || result.startYear == null) return true
        return startYear == result.startYear.toIntOrNull()
    }

    private suspend fun getCover(image: ComicVineImage): Image? {
        val url = image.mediumUrl ?: image.smallUrl ?: image.originalUrl ?: return null
        return try {
            client.getCover(url)
        } catch (e: ClientRequestException) {
            // some entries have url but it returns 404. Ignore such cases
            if (e.response.status == HttpStatusCode.NotFound) {
                logger.error { "Entry has cover url but there was no image. Skipping cover retrieval \"$url\"" }
                null
            } else throw e
        }
    }

    private inline fun <reified T : Any> handleResult(result: ComicVineSearchResult<T>): T {
        if (result.error != "OK")
            throw IllegalStateException("Comic Vine returned error response. status code ${result.statusCode}")
        return result.results
    }
}