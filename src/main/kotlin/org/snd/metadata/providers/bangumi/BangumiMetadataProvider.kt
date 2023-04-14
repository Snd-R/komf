package org.snd.metadata.providers.bangumi

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MediaType
import org.snd.metadata.model.Provider
import org.snd.metadata.model.SeriesSearchResult
import org.snd.metadata.model.metadata.ProviderBookId
import org.snd.metadata.model.metadata.ProviderBookMetadata
import org.snd.metadata.model.metadata.ProviderSeriesId
import org.snd.metadata.model.metadata.ProviderSeriesMetadata
import org.snd.metadata.providers.bangumi.model.SubjectSearchData
import org.snd.metadata.providers.bangumi.model.SubjectType

// Manga and Novel are both considered book in Bangumi
// For now, Novel just means "everything"
// In the future, if there's other search support, Bangumi also have Anime, Music, etc.
class BangumiMetadataProvider(
    private val client: BangumiClient,
    private val metadataMapper: BangumiMetadataMapper,
    private val nameMatcher: NameSimilarityMatcher,
    private val fetchSeriesCovers: Boolean,
    private val mediaType: MediaType,
) : MetadataProvider {

    override fun providerName() = Provider.BANGUMI

    override fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSubject(seriesId.id.toLong())
        val bookRelations = client.getSubjectRelations(series.id)
            .filter { it.type == SubjectType.BOOK }
            .filter { it.relation == "单行本" }

        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series) else null
        return metadataMapper.toSeriesMetadata(series, bookRelations, thumbnail)
    }

    override fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val book = client.getSubject(bookId.id.toLong())
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(book) else null
        return metadataMapper.toBookMetadata(book, thumbnail)
    }

    override fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        return client.searchSeries(seriesName).data.asSequence()
            .sortedWith(subjectRank())
            .filter { it.tags.none { tag -> tag.name == "漫画单行本" } }
            .take(limit)
            .map {
                SeriesSearchResult(
                    imageUrl = it.image,
                    provider = this.providerName(),
                    resultId = it.id.toString(),
                    title = it.nameCn.ifBlank { it.name },
                )
            }.toList()
    }

    override fun matchSeriesMetadata(seriesName: String): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(seriesName)
        val matches = searchResults.data.asSequence()
            .sortedWith(subjectRank())
            .filter { it.tags.none { tag -> tag.name == "漫画单行本" } }
            .filter {
                val titles = listOfNotNull(it.nameCn, it.name)
                nameMatcher.matches(seriesName, titles)
            }.toList()

        val matchId = if (matches.size > 1) {
            matches.asSequence()
                .map { client.getSubject(it.id) }
                .firstOrNull {
                    when (this.mediaType) {
                        MediaType.MANGA -> it.platform == "漫画"
                        MediaType.NOVEL -> it.platform == "小说"
                    }
                }?.id
        } else matches.firstOrNull()?.id

        return matchId?.let {
            val series = client.getSubject(it)
            val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series) else null
            val bookRelations = client.getSubjectRelations(series.id)
                .filter { book -> book.type == SubjectType.BOOK }
                .filter { book -> book.relation == "单行本" }

            metadataMapper.toSeriesMetadata(
                series,
                bookRelations,
                thumbnail,
            )
        }
    }

    private fun subjectRank() = Comparator<SubjectSearchData> { a, b ->
        when {
            a.rank == b.rank -> 0
            a.rank == 0 -> 1
            b.rank == 0 -> -1
            else -> compareValues(a.rank, b.rank)
        }
    }
}