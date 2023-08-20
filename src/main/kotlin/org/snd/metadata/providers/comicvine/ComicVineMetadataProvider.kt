package org.snd.metadata.providers.comicvine

import io.javalin.http.HttpStatus
import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.helper.ValidationException
import org.snd.common.exceptions.HttpException
import org.snd.metadata.ImageHashComparator
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.BookQualifier
import org.snd.metadata.model.Image
import org.snd.metadata.model.MatchQuery
import org.snd.metadata.model.Provider.COMIC_VINE
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.comicvine.model.ComicVineImage
import org.snd.metadata.providers.comicvine.model.ComicVineIssueId
import org.snd.metadata.providers.comicvine.model.ComicVineSearchResult
import org.snd.metadata.providers.comicvine.model.ComicVineVolumeId
import org.snd.metadata.providers.comicvine.model.ComicVineVolumeSearch
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
        val cover = series.image?.let { getCover(it) }
        return mapper.toSeriesMetadata(series, cover)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val issue = handleResult(client.getIssue(bookId.toComicVineIssueId()))
        val cover = issue.image?.let { getCover(it) }
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

    private fun matchesBookCover(volume: ComicVineVolumeSearch, qualifier: BookQualifier): Boolean {
        val firstIssueNumber = volume.firstIssue?.issueNumber?.toDoubleOrNull() ?: return false
        val qualifierImage = qualifier.cover?.toBufferedImage() ?: return false

        logger.info { "matching cover of volume \"${volume.name}\" ${volume.siteDetailUrl}" }

        // TODO get issue list and find matching number
        if (qualifier.number.start != firstIssueNumber) return false

        val issue = handleResult(client.getIssue(ComicVineIssueId(volume.firstIssue.id)))
        val issueCover = issue.image?.let { getCover(it) }?.toBufferedImage() ?: return false
        return ImageHashComparator.compareImages(qualifierImage, issueCover)
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

    private fun getCover(image: ComicVineImage): Image? {
        val url = image.mediumUrl ?: image.smallUrl ?: image.originalUrl ?: return null
        return try {
            client.getCover(url.toHttpUrl())
        } catch (e: HttpException) {
            // some entries have url but it returns 404. Ignore such cases
            if (e.code == HttpStatus.NOT_FOUND.code) {
                logger.error { "Entry has cover url but there was no image. Skipping cover retrieval \"$url\"" }
                null
            } else throw e
        }
    }

    private inline fun <reified T : Any> handleResult(result: ComicVineSearchResult<T>): T {
        if (result.error != "OK")
            throw ValidationException("Comic Vine returned error response. status code ${result.statusCode}")
        return result.results
    }
}