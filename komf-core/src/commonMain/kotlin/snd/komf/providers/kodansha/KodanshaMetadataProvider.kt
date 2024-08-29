package snd.komf.providers.kodansha

import io.ktor.http.*
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.MetadataProvider
import snd.komf.providers.kodansha.model.KodanshaBookId
import snd.komf.providers.kodansha.model.KodanshaSeriesId
import snd.komf.util.NameSimilarityMatcher

class KodanshaMetadataProvider(
    private val client: KodanshaClient,
    private val metadataMapper: KodanshaMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): CoreProviders {
        return CoreProviders.KODANSHA
    }

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(KodanshaSeriesId(seriesId.value.toInt())).response
        val thumbnail = if (fetchSeriesCovers) getThumbnail(series.thumbnails?.firstOrNull()?.url) else null
        val bookList = client.getAllSeriesBooks(KodanshaSeriesId(series.id))
        return metadataMapper.toSeriesMetadata(series, bookList, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(KodanshaSeriesId(seriesId.value.toInt())).response
        return getThumbnail(series.thumbnails?.firstOrNull()?.url)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = client.getBook(KodanshaBookId(bookId.id.toInt())).response
        val thumbnail = if (fetchBookCovers) getThumbnail(bookMetadata.thumbnails.firstOrNull()?.url) else null

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.search(sanitizeSearchInput(seriesName)).response.take(limit)
        return searchResults
            .filter { it.type == "series" }
            .map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.search(sanitizeSearchInput(seriesName)).response

        return searchResults
            .filter { it.type == "series" }
            .filter { it.content.readableUrl != null }
            .firstOrNull { nameMatcher.matches(seriesName, it.content.title.removeSuffix(" (manga)")) }
            ?.let {
                val series = client.getSeries(KodanshaSeriesId(it.content.id)).response
                val thumbnail = if (fetchSeriesCovers) getThumbnail(series.thumbnails?.firstOrNull()?.url) else null
                val bookList = client.getAllSeriesBooks(KodanshaSeriesId(series.id))
                metadataMapper.toSeriesMetadata(series, bookList, thumbnail)
            }
    }

    private suspend fun getThumbnail(url: String?): Image? {
        if (url == null || url.contains("kodansha_placeholder")) return null

        return client.getThumbnail(
            URLBuilder(url).apply {
                parameters.append("w", "1000")
                parameters.append("f", "webp")
            }.buildString()
        )
    }

    private fun sanitizeSearchInput(input: String): String {
        return input.take(300)
            .replace("\"", "")
    }
}
