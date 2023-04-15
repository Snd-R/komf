package org.snd.metadata.providers.bangumi

import org.snd.metadata.MetadataProvider
import org.snd.metadata.NameSimilarityMatcher
import org.snd.metadata.model.MatchQuery
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

    override fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(matchQuery.seriesName)
        val matches = searchResults.data.asSequence()
            .sortedWith(subjectRank())
            .filter { it.tags.none { tag -> tag.name == "漫画单行本" } }
            .filter { nameMatcher.matches(matchQuery.seriesName, listOfNotNull(it.nameCn, it.name)) }
            .toList()

        val subject = when (matches.size) {
            0 -> null
            1 -> matches.first().let { client.getSubject(it.id) }
            else -> {
                matches.asSequence()
                    .map { client.getSubject(it.id) }
                    .firstOrNull {
                        when (this.mediaType) {
                            MediaType.MANGA -> it.platform == "漫画"
                            MediaType.NOVEL -> it.platform == "小说"
                        }
                    }
            }
        } ?: return null

        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(subject) else null
        val bookRelations = client.getSubjectRelations(subject.id)
            .filter { it.type == SubjectType.BOOK }
            .filter { it.relation == "单行本" }

        return metadataMapper.toSeriesMetadata(
            subject,
            bookRelations,
            thumbnail,
        )

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