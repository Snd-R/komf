package snd.komf.providers.bangumi

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
import snd.komf.providers.bangumi.model.BangumiSubject
import snd.komf.providers.bangumi.model.SubjectSearchData
import snd.komf.providers.bangumi.model.SubjectType
import snd.komf.util.NameSimilarityMatcher

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
    init {
        if (mediaType == MediaType.COMIC) throw IllegalStateException("Comics media type is not supported")
    }

    override fun providerName() = CoreProviders.BANGUMI

    override suspend fun getSeriesMetadata(seriesId: ProviderSeriesId): ProviderSeriesMetadata {
        val series = client.getSubject(seriesId.value.toLong())
        val bookRelations = client.getSubjectRelations(series.id)
            .filter { it.type == SubjectType.BOOK }
            .filter { it.relation == "单行本" }

        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(series) else null
        return metadataMapper.toSeriesMetadata(series, bookRelations, thumbnail)
    }

    override suspend fun getSeriesCover(seriesId: ProviderSeriesId): Image? {
        val series = client.getSubject(seriesId.value.toLong())
        return client.getThumbnail(series)
    }

    override suspend fun getBookMetadata(seriesId: ProviderSeriesId, bookId: ProviderBookId): ProviderBookMetadata {
        val book = client.getSubject(bookId.id.toLong())
        val thumbnail = if (fetchSeriesCovers) client.getThumbnail(book) else null
        return metadataMapper.toBookMetadata(book, thumbnail)
    }

    override suspend fun searchSeries(seriesName: String, limit: Int): Collection<SeriesSearchResult> {
        return client.searchSeries(seriesName).data.asSequence()
            .filter { it.tags.none { tag -> tag.name == "漫画单行本" } }
            .take(limit)
            .map {
                metadataMapper.toSearchResult(it)
            }.toList()
    }

    override suspend fun matchSeriesMetadata(matchQuery: MatchQuery): ProviderSeriesMetadata? {
        val searchResults = client.searchSeries(matchQuery.seriesName)
        val matches = searchResults.data.asSequence()
            .filter { it.tags.none { tag -> tag.name == "漫画单行本" } }
            .filter { nameMatcher.matches(matchQuery.seriesName, listOfNotNull(it.nameCn, it.name)) }
            .toList()

        val subject = when (matches.size) {
            0 -> null
            1 -> matches.first().let { client.getSubject(it.id) }
            else -> firstMatchingType(matches, this.mediaType)

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

    private suspend fun firstMatchingType(matches: List<SubjectSearchData>, type: MediaType): BangumiSubject? {
        val matchPlatform = when (type) {
            MediaType.MANGA -> "漫画"
            MediaType.NOVEL -> "小说"
            MediaType.COMIC, MediaType.WEBTOON -> return null
        }

        for (match in matches) {
            val subject = client.getSubject(match.id)
            if (subject.platform == matchPlatform) return subject
        }
        return null
    }
}