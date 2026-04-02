package snd.komf.providers.stripinfo

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import snd.komf.model.Image
import snd.komf.model.MatchQuery
import snd.komf.model.ProviderBookId
import snd.komf.model.ProviderBookMetadata
import snd.komf.model.ProviderSeriesId
import snd.komf.model.ProviderSeriesMetadata
import snd.komf.model.SeriesSearchResult
import snd.komf.providers.CoreProviders
import snd.komf.providers.CoreProviders.STRIP_INFO
import snd.komf.providers.MetadataProvider
import snd.komf.providers.stripinfo.model.StripInfoAlbumId
import snd.komf.providers.stripinfo.model.StripInfoSeries
import snd.komf.providers.stripinfo.model.StripInfoSeriesId
import snd.komf.util.NameSimilarityMatcher

class StripInfoMetadataProvider(
    private val client: StripInfoClient,
    private val metadataMapper: StripInfoMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val fetchBookCovers: Boolean,
) : MetadataProvider {

    override fun providerName(): CoreProviders = STRIP_INFO

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(StripInfoSeriesId(seriesId.value.toInt()))
        val thumbnail = if (fetchSeriesCovers) fetchSeriesCoverImage(series) else null
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSeries(StripInfoSeriesId(seriesId.value.toInt()))
        return fetchSeriesCoverImage(series)
    }

    private suspend fun fetchSeriesCoverImage(series: StripInfoSeries): Image? {
        val firstAlbumId = series.albums.minByOrNull { it.id.id }?.id
        if (firstAlbumId != null) {
            val firstAlbum = client.getAlbum(firstAlbumId)
            firstAlbum.imageUrl?.let { return client.getCover(it) }
        }
        return series.imageUrl?.let { client.getCover(it) }
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val album = client.getAlbum(StripInfoAlbumId(bookId.id.toInt()))
        val thumbnail = if (fetchBookCovers) album.imageUrl?.let { client.getCover(it) } else null
        return metadataMapper.toBookMetadata(album, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val results = client.searchSeries(cleanSearchName(seriesName).take(400)).take(limit)
        return coroutineScope {
            results.map { result ->
                async {
                    val imageUrl = try {
                        val series = client.getSeries(result.id)
                        val firstAlbumId = series.albums.minByOrNull { it.id.id }?.id
                        if (firstAlbumId != null) client.getAlbum(firstAlbumId).imageUrl ?: series.imageUrl
                        else series.imageUrl
                    } catch (_: Exception) { null }
                    metadataMapper.toSeriesSearchResult(result, imageUrl)
                }
            }.awaitAll()
        }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = cleanSearchName(matchQuery.seriesName)
        val searchResults = client.searchSeries(seriesName.take(400))
        val match = searchResults
            .firstOrNull { nameMatcher.matches(seriesName, listOf(it.title)) }

        return match?.let {
            val series = client.getSeries(it.id)
            val thumbnail = if (fetchSeriesCovers) fetchSeriesCoverImage(series) else null
            metadataMapper.toSeriesMetadata(series, thumbnail)
        }
    }

    // Strip trailing number ranges like "01-35" and parentheticals like "(ic)" from folder names
    // e.g. "Billie Turf 01-35 (ic)" → "Billie Turf"
    private fun cleanSearchName(name: String): String =
        name.replace(Regex("""\s+\d+[-–]\d+(\s*\([^)]*\))*\s*$"""), "").trim()
}
