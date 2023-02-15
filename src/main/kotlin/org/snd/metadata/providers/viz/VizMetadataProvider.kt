package org.snd.metadata.providers.viz

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.snd.common.exceptions.HttpException
import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.Image
import org.snd.metadata.model.Provider
import org.snd.metadata.model.Provider.VIZ
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.viz.model.VizBook
import org.snd.metadata.providers.viz.model.VizBookId
import org.snd.metadata.providers.viz.model.VizBookReleaseType.DIGITAL
import org.snd.metadata.providers.viz.model.VizBookReleaseType.PAPERBACK
import org.snd.metadata.providers.viz.model.toSeriesSearchResult
import org.snd.metadata.providers.viz.model.toVizSeriesBook

class VizMetadataProvider(
    private val client: VizClient,
    private val metadataMapper: VizMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
) : MetadataProvider {
    override fun providerName(): Provider {
        return VIZ
    }

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = getBook(VizBookId(seriesId.id))
        val books = series.allBooksId
            ?.let { client.getAllBooks(it) }
            ?: listOf(series.toVizSeriesBook())
        val thumbnail = getThumbnail(series.coverUrl)

        return metadataMapper.toSeriesMetadata(series, books, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val bookMetadata = getBook(VizBookId(bookId.id))
        val thumbnail = getThumbnail(bookMetadata.coverUrl)

        return metadataMapper.toBookMetadata(bookMetadata, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        if (isInvalidName(seriesName)) return emptyList()

        val searchResults = client.searchSeries(sanitizeSearchInput(seriesName.take(100))).take(limit)
        return searchResults.map { it.toSeriesSearchResult() }
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        if (isInvalidName(seriesName)) return null
        val searchResults = client.searchSeries(seriesName.take(100))

        return searchResults
            .firstOrNull { nameMatcher.matches(seriesName, it.seriesName) }
            ?.let {
                val firstBook = getBook(it.id)
                val books = firstBook.allBooksId
                    ?.let { id -> client.getAllBooks(id) }
                    ?: listOf(firstBook.toVizSeriesBook())
                val thumbnail = getThumbnail(firstBook.coverUrl)
                metadataMapper.toSeriesMetadata(firstBook, books, thumbnail)
            }
    }

    private fun getBook(id: VizBookId): VizBook {
        return try {
            client.getBook(id, DIGITAL)
        } catch (e: HttpException.NotFound) {
            client.getBook(id, PAPERBACK)
        }
    }

    private fun getThumbnail(url: String?): Image? = url?.toHttpUrl()?.let { client.getThumbnail(it) }

    private fun isInvalidName(name: String) = name.contains("^[0-9]+--".toRegex())

    private fun sanitizeSearchInput(name: String): String {
        return name
            .replace("[(]([^)]+)[)]".toRegex(), "")
            .trim()
    }
}
