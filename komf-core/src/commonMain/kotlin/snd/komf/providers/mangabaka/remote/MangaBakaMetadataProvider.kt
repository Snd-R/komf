package snd.komf.providers.mangabaka.remote

import io.github.reactivecircus.cache4k.Cache
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
import snd.komf.providers.mangabaka.remote.model.MangaBakaSeries
import snd.komf.providers.mangabaka.remote.model.MangaBakaSeriesId
import snd.komf.providers.mangabaka.remote.model.MangaBakaType
import snd.komf.util.NameSimilarityMatcher
import kotlin.time.Duration.Companion.minutes

class MangaBakaMetadataProvider(
    private val client: MangaBakaClient,
    private val metadataMapper: MangaBakaMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    mediaType: MediaType,
) : MetadataProvider {
    private val seriesTypes: List<MangaBakaType> = when (mediaType) {
        MediaType.MANGA -> listOf(
            MangaBakaType.MANGA,
            MangaBakaType.MANHWA,
            MangaBakaType.MANHUA,
            MangaBakaType.OEL,
            MangaBakaType.OTHER
        )

        MediaType.NOVEL -> listOf(MangaBakaType.NOVEL)
        MediaType.COMIC -> listOf(MangaBakaType.OEL, MangaBakaType.OTHER)
        MediaType.WEBTOON -> listOf(MangaBakaType.MANHUA, MangaBakaType.MANHWA)
    }

    private val cache = Cache.Builder<MangaBakaSeriesId, MangaBakaSeries>()
        .expireAfterWrite(30.minutes)
        .build()

    override fun providerName() = CoreProviders.MANGA_BAKA

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val id = seriesId.toMangaBakaId()
        val series = cache.get(id) { client.getSeries(id) }
        val cover = if (fetchSeriesCovers && series.cover != null) client.getCoverBytes(series.cover)
        else null

        return metadataMapper.toSeriesMetadata(series, cover)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val id = seriesId.toMangaBakaId()
        val series = cache.get(id) { client.getSeries(id) }
        return series.cover?.let { client.getCoverBytes(it) }
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
        val results = client.searchSeries(
            title = seriesName,
            types = seriesTypes,
            page = 1,
        ).results
        results.forEach { cache.put(it.id, it) }

        return results.take(limit).map { metadataMapper.toSeriesSearchResult(it) }
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val seriesName = matchQuery.seriesName
        val searchResults = client.searchSeries(seriesName.take(400), seriesTypes).results
        searchResults.forEach { cache.put(it.id, it) }

        val match = searchResults.firstOrNull { series ->
            val titles = listOfNotNull(
                series.title,
                series.nativeTitle
            ) + series.secondaryTitles.flatMap { it.value }
            nameMatcher.matches(seriesName, titles)
        }

        return match?.let { series ->
            val cover =
                if (fetchSeriesCovers && series.cover != null) client.getCoverBytes(series.cover)
                else null
            metadataMapper.toSeriesMetadata(series, cover)
        }
    }
}

private fun ProviderSeriesId.toMangaBakaId() = MangaBakaSeriesId(this.value.toInt())