package org.snd.metadata.providers.mangaupdates

import org.snd.metadata.MediaType
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Provider.MANGA_UPDATES
import org.snd.metadata.model.ProviderBookId
import org.snd.metadata.model.ProviderBookMetadata
import org.snd.metadata.model.ProviderSeriesId
import org.snd.metadata.model.ProviderSeriesMetadata
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.providers.mangaupdates.model.SeriesType.ARTBOOK
import org.snd.metadata.providers.mangaupdates.model.SeriesType.DOUJINSHI
import org.snd.metadata.providers.mangaupdates.model.SeriesType.FILIPINO
import org.snd.metadata.providers.mangaupdates.model.SeriesType.FRENCH
import org.snd.metadata.providers.mangaupdates.model.SeriesType.INDONESIAN
import org.snd.metadata.providers.mangaupdates.model.SeriesType.MALAYSIAN
import org.snd.metadata.providers.mangaupdates.model.SeriesType.MANGA
import org.snd.metadata.providers.mangaupdates.model.SeriesType.MANHUA
import org.snd.metadata.providers.mangaupdates.model.SeriesType.MANHWA
import org.snd.metadata.providers.mangaupdates.model.SeriesType.NORDIC
import org.snd.metadata.providers.mangaupdates.model.SeriesType.NOVEL
import org.snd.metadata.providers.mangaupdates.model.SeriesType.OEL
import org.snd.metadata.providers.mangaupdates.model.SeriesType.SPANISH
import org.snd.metadata.providers.mangaupdates.model.SeriesType.THAI
import org.snd.metadata.providers.mangaupdates.model.SeriesType.VIETNAMESE
import org.snd.metadata.providers.mangaupdates.model.toSeriesSearchResult

private val mangaTypes = listOf(
    MANGA, MANHWA, MANHUA, ARTBOOK, DOUJINSHI,
    FILIPINO, INDONESIAN, THAI, VIETNAMESE, MALAYSIAN,
    OEL, NORDIC, FRENCH, SPANISH
)
private val novelTypes = listOf(NOVEL)

class MangaUpdatesMetadataProvider(
    private val client: MangaUpdatesClient,
    private val metadataMapper: MangaUpdatesMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    mediaType: MediaType,
) : MetadataProvider {

    private val seriesTypes = if (mediaType == MediaType.MANGA) mangaTypes else novelTypes

    override fun providerName() = MANGA_UPDATES

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSeries(seriesId.id.toLong())
        val thumbnail = client.getThumbnail(series)
        return metadataMapper.toSeriesMetadata(series, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        throw UnsupportedOperationException()
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        val searchResults = client.searchSeries(
            seriesName.take(400), seriesTypes, 1, limit
        ).results.take(limit)

        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName.take(400), seriesTypes).results

        return searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.title) }
            ?.let {
                val series = client.getSeries(it.id)
                val thumbnail = client.getThumbnail(series)
                metadataMapper.toSeriesMetadata(series, thumbnail)
            }
    }
}
