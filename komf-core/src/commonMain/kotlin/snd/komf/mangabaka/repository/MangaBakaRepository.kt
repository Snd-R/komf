package snd.komf.mangabaka.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.Exists
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.api.JdbcPreparedStatementApi
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcResult
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import snd.komf.mangabaka.model.MangaBakaAniListSource
import snd.komf.mangabaka.model.MangaBakaAnimeInfo
import snd.komf.mangabaka.model.MangaBakaAnimeNewsNetworkSource
import snd.komf.mangabaka.model.MangaBakaAnimePlanetSource
import snd.komf.mangabaka.model.MangaBakaContentRating
import snd.komf.mangabaka.model.MangaBakaCover
import snd.komf.mangabaka.model.MangaBakaCoverDpi
import snd.komf.mangabaka.model.MangaBakaCoverRaw
import snd.komf.mangabaka.model.MangaBakaImportMetadata
import snd.komf.mangabaka.model.MangaBakaKitsuSource
import snd.komf.mangabaka.model.MangaBakaLink
import snd.komf.mangabaka.model.MangaBakaLinkId
import snd.komf.mangabaka.model.MangaBakaLinkType
import snd.komf.mangabaka.model.MangaBakaLinkedSeries
import snd.komf.mangabaka.model.MangaBakaMangaUpdatesSource
import snd.komf.mangabaka.model.MangaBakaMyAnimeListSource
import snd.komf.mangabaka.model.MangaBakaPublishedDate
import snd.komf.mangabaka.model.MangaBakaPublisher
import snd.komf.mangabaka.model.MangaBakaRelationType
import snd.komf.mangabaka.model.MangaBakaRelationship
import snd.komf.mangabaka.model.MangaBakaRelationshipChronology
import snd.komf.mangabaka.model.MangaBakaRelationshipId
import snd.komf.mangabaka.model.MangaBakaSeries
import snd.komf.mangabaka.model.MangaBakaSeriesId
import snd.komf.mangabaka.model.MangaBakaSeriesState
import snd.komf.mangabaka.model.MangaBakaSeriesTag
import snd.komf.mangabaka.model.MangaBakaShikimoriSource
import snd.komf.mangabaka.model.MangaBakaSource
import snd.komf.mangabaka.model.MangaBakaStatus
import snd.komf.mangabaka.model.MangaBakaTag
import snd.komf.mangabaka.model.MangaBakaTagId
import snd.komf.mangabaka.model.MangaBakaTagWeight
import snd.komf.mangabaka.model.MangaBakaTitle
import snd.komf.mangabaka.model.MangaBakaType
import snd.komf.mangabaka.repository.tables.ArtistsTable
import snd.komf.mangabaka.repository.tables.AuthorsTable
import snd.komf.mangabaka.repository.tables.ImportMetadataTable
import snd.komf.mangabaka.repository.tables.KomgaSeriesTable
import snd.komf.mangabaka.repository.tables.LinksTable
import snd.komf.mangabaka.repository.tables.PublishersTable
import snd.komf.mangabaka.repository.tables.RelationshipsTable
import snd.komf.mangabaka.repository.tables.SeriesTable
import snd.komf.mangabaka.repository.tables.SeriesTagsTable
import snd.komf.mangabaka.repository.tables.TagsTable
import snd.komf.mangabaka.repository.tables.TitlesTable
import snd.komf.model.KomgaSeriesId
import kotlin.time.Instant
import kotlin.time.measureTime

private val logger = KotlinLogging.logger { }

class MangaBakaRepository(private val database: Database) {

    fun link(komgaId: KomgaSeriesId, mangaBakaId: MangaBakaSeriesId) {
        transaction(database) {
            val existsOp = Exists(SeriesTable.select(SeriesTable.id).where { SeriesTable.id.eq(mangaBakaId.value) })
            val exists = Table.Dual.select(existsOp).first()[existsOp]
            require(exists) { "Series with id $mangaBakaId does not exist" }

            KomgaSeriesTable.deleteWhere { KomgaSeriesTable.komgaId.eq(komgaId.value) }
            KomgaSeriesTable.insert {
                it[KomgaSeriesTable.komgaId] = komgaId.value
                it[KomgaSeriesTable.mangaBakaId] = mangaBakaId.value
            }
        }
    }

    fun unlink(komgaId: KomgaSeriesId) {
        transaction(database) {
            KomgaSeriesTable.deleteWhere { KomgaSeriesTable.komgaId.eq(komgaId.value) }
        }
    }


    fun get(id: MangaBakaSeriesId): MangaBakaSeries {
        return transaction(database) {
            SeriesTable.selectAll().where { SeriesTable.id.eq(id.value) }.mapToBakaModel().first()
        }
    }

    fun find(id: KomgaSeriesId): MangaBakaLinkedSeries? {
        return transaction(database) { findAllLinked(listOf(id)).firstOrNull() }
    }

    fun getAllTags(): List<MangaBakaTag> {
        return transaction(database) {
            TagsTable.selectAll()
                .map { it.toBakaTag() }
        }
    }

    fun search(
        title: String,
        types: List<MangaBakaType>? = null,
        typesNot: List<MangaBakaType>? = null
    ): List<MangaBakaSeries> {
        return transaction(database) {
            var ftsStatement: JdbcPreparedStatementApi? = null
            var result: JdbcResult? = null
            try {
                val sqlString = buildString {
                    append("SELECT id FROM titles_fts WHERE title MATCH ?")
                    types?.joinToString(", ") { "?" }?.let { append(" AND type IN ($it)") }
                    typesNot?.joinToString(", ") { "?" }?.let { append(" AND type NOT IN ($it)") }
                    append(" ORDER BY rank LIMIT 24")
                }

                ftsStatement = connection.prepareStatement(sqlString, false)
                ftsStatement.set(1, "\"$title\"", TextColumnType())
                var statementCurrentIndex = 1
                types?.forEach { value ->
                    statementCurrentIndex += 1
                    ftsStatement.set(
                        statementCurrentIndex,
                        value.name,
                        TextColumnType()
                    )
                }
                typesNot?.forEach { value ->
                    statementCurrentIndex += 1
                    ftsStatement.set(
                        statementCurrentIndex,
                        value.name,
                        TextColumnType()
                    )
                }

                result = ftsStatement.executeQuery()
                val rs = result.result
                val ids = buildList { while (rs.next()) add(MangaBakaSeriesId(rs.getLong("id"))) }

                findAll(ids)
            } finally {
                result?.close()
                ftsStatement?.closeIfPossible()
            }
        }
    }


    private fun findAll(ids: List<MangaBakaSeriesId>): List<MangaBakaSeries> {
        return transaction(database) {
            SeriesTable
                .selectAll()
                .where { SeriesTable.id.inList(ids.map { it.value }) }
                .mapToBakaModel()
        }
    }

    fun findAllLinked(ids: List<KomgaSeriesId>): List<MangaBakaLinkedSeries> {
        return transaction(database) {
            KomgaSeriesTable
                .join(
                    otherTable = SeriesTable,
                    joinType = JoinType.LEFT,
                    onColumn = KomgaSeriesTable.mangaBakaId,
                    otherColumn = SeriesTable.id,
                )
                .selectAll()
                .where { KomgaSeriesTable.komgaId.inList(ids.map { it.value }) }
                .mapToLinkedModel()
        }
    }

    fun getImportMetadata(): MangaBakaImportMetadata? {
        return transaction(database) {
            val timestamp = ImportMetadataTable.selectAll()
                .where { ImportMetadataTable.key.eq("download_date") }.firstOrNull()
                ?.let { Instant.parse(it[ImportMetadataTable.value]) }

            val checksum = ImportMetadataTable.selectAll()
                .where { ImportMetadataTable.key.eq("checksum") }.firstOrNull()
                ?.let { it[ImportMetadataTable.value] }

            if (timestamp == null || checksum == null) null
            else MangaBakaImportMetadata(timestamp, checksum)
        }
    }


    private fun Query.mapToBakaModel(): List<MangaBakaSeries> {
        return this.fetch().map { (_, series) -> series }
    }


    private fun Query.mapToLinkedModel(): List<MangaBakaLinkedSeries> {
        return this.fetch().map { (row, series) ->
            MangaBakaLinkedSeries(KomgaSeriesId(row[KomgaSeriesTable.komgaId]), series)
        }
    }

    private fun Query.fetch(): List<Pair<ResultRow, MangaBakaSeries>> {
        val rows = this.toList()

        var seriesIds: List<Long>
        var authors: Map<Long, List<String>>
        var artists: Map<Long, List<String>>
        var links: Map<Long, List<MangaBakaLink>>
        var publishers: Map<Long, List<MangaBakaPublisher>>
        var relationships: Map<Long, List<MangaBakaRelationship>>
        var titles: Map<Long, List<MangaBakaTitle>>
        var tags: Map<Long, List<MangaBakaSeriesTag>>
        var allTags: Map<Long, List<MangaBakaSeriesTag>>
        measureTime {
            seriesIds = rows.map { it[SeriesTable.id] }
            if (seriesIds.isEmpty()) {
                return emptyList()
            }
            authors = selectAuthors(seriesIds)
            artists = selectArtists(seriesIds)
            links = selectLinks(seriesIds)
            publishers = selectPublishers(seriesIds)
            relationships = selectRelationships(seriesIds)
            titles = selectTitles(seriesIds)
            tags = selectTags(seriesIds)
//            allTags = tags.map { (id, tags) -> id to withMissingTags(tags) }.toMap()
        }.also { logger.info { "fetched ${seriesIds.size} list data in $it" } }

        return rows.map { row ->
            val seriesId = row[SeriesTable.id]
            row to row.toBakaSeriesModel(
                authors = authors[seriesId].orEmpty(),
                artists = artists[seriesId].orEmpty(),
                links = links[seriesId].orEmpty(),
                publishers = publishers[seriesId].orEmpty(),
                relationships = relationships[seriesId].orEmpty(),
                titles = titles[seriesId].orEmpty(),
                tags = tags[seriesId].orEmpty(),
            )
        }
    }

    private fun selectAuthors(seriesIds: List<Long>): Map<Long, List<String>> {
        return AuthorsTable.selectAll()
            .where { AuthorsTable.seriesId.inList(seriesIds) }
            .groupBy({ it[AuthorsTable.seriesId] }, { it[AuthorsTable.name] })
    }

    private fun selectArtists(seriesIds: List<Long>): Map<Long, List<String>> {
        return ArtistsTable.selectAll()
            .where { ArtistsTable.seriesId.inList(seriesIds) }
            .groupBy({ it[ArtistsTable.seriesId] }, { it[ArtistsTable.name] })
    }

    private fun selectLinks(seriesIds: List<Long>): Map<Long, List<MangaBakaLink>> {
        return LinksTable.selectAll()
            .where { LinksTable.seriesId.inList(seriesIds) }
            .groupBy({ it[LinksTable.seriesId] }, { it.toBakaLink() })
    }

    private fun selectPublishers(seriesIds: List<Long>): Map<Long, List<MangaBakaPublisher>> {
        return PublishersTable.selectAll()
            .where { PublishersTable.seriesId.inList(seriesIds) }
            .groupBy({ it[PublishersTable.seriesId] }, { it.toBakaPublisher() })
    }

    private fun selectRelationships(seriesIds: List<Long>): Map<Long, List<MangaBakaRelationship>> {
        return RelationshipsTable.selectAll()
            .where { RelationshipsTable.seriesId.inList(seriesIds) }
            .groupBy({ it[RelationshipsTable.seriesId] }, { it.toBakaRelationship() })
    }

    private fun selectTitles(seriesIds: List<Long>): Map<Long, List<MangaBakaTitle>> {
        return TitlesTable.selectAll()
            .where { TitlesTable.seriesId.inList(seriesIds) }
            .groupBy({ it[TitlesTable.seriesId] }, { it.toBakaTitle() })
    }

    private fun selectTags(seriesIds: List<Long>): Map<Long, List<MangaBakaSeriesTag>> {
        return SeriesTagsTable
            .join(
                otherTable = TagsTable,
                joinType = JoinType.LEFT,
                onColumn = SeriesTagsTable.tagId,
                otherColumn = TagsTable.id,
            )
            .selectAll()
            .where { SeriesTagsTable.seriesId.inList(seriesIds) }
            .groupBy({ it[SeriesTagsTable.seriesId] }, { it.toBakaSeriesTag() })
    }

    private fun withMissingTags(tags: List<MangaBakaSeriesTag>): List<MangaBakaSeriesTag> {
        val tagsById = tags.associateBy { it.id }.toMutableMap()
        val missingTags = HashSet<MangaBakaSeriesTag>()
        var tagsToScan = tags
        for (i in 0 until 5) {
            val orphans = tagsToScan.filter { tag -> tag.parentId != null && tagsById[tag.parentId] == null }
            if (orphans.isEmpty()) break
            val missing = findTagsByIds(ids = orphans.mapNotNull { it.parentId })
            missing.forEach { tagsById[it.id] = it }
            missingTags.addAll(missing)
            tagsToScan = missing
        }

        return tags + missingTags
    }

    private fun findTagsByIds(ids: List<MangaBakaTagId>): List<MangaBakaSeriesTag> {
        return TagsTable
            .selectAll()
            .where { TagsTable.id.inList(ids.map { it.value }) }
            .map {
                MangaBakaSeriesTag(
                    id = MangaBakaTagId(it[TagsTable.id]),
                    contentRating = MangaBakaContentRating.valueOf(it[TagsTable.contentRating]),
                    description = it[TagsTable.description],
                    level = it[TagsTable.level],
                    name = it[TagsTable.name],
                    namePath = it[TagsTable.namePath],
                    parentId = it[TagsTable.parentId]?.let { MangaBakaTagId(it) },
                    seriesCount = it[TagsTable.seriesCount],
                    isGenre = it[TagsTable.isGenre],
                    mergedWith = it[TagsTable.mergedWith],
                    impliedByTagIds = emptyList(),
                    isSpoiler = false,
                    isExplicit = false,
                    weight = MangaBakaTagWeight.UNWEIGHTED,
                )
            }
    }

    private fun ResultRow.toBakaLink(): MangaBakaLink {
        return MangaBakaLink(
            id = MangaBakaLinkId(this[LinksTable.idUnused]),
            language = this[LinksTable.language],
            name = this[LinksTable.name],
            nameDisplay = this[LinksTable.nameDisplay],
            type = MangaBakaLinkType.valueOf(this[LinksTable.type]),
            url = this[LinksTable.url]
        )
    }

    private fun ResultRow.toBakaPublisher(): MangaBakaPublisher {
        return MangaBakaPublisher(
            name = this[PublishersTable.name],
            note = this[PublishersTable.note],
            type = this[PublishersTable.type],
        )
    }

    private fun ResultRow.toBakaRelationship(): MangaBakaRelationship {
        return MangaBakaRelationship(
            id = MangaBakaRelationshipId(this[RelationshipsTable.idUnused]),
            chronology = MangaBakaRelationshipChronology.valueOf(this[RelationshipsTable.chronology]),
            isManual = this[RelationshipsTable.isManual],
            note = this[RelationshipsTable.note],
            relationType = MangaBakaRelationType.valueOf(this[RelationshipsTable.relationType]),
            toSeriesId = MangaBakaSeriesId(this[RelationshipsTable.seriesId])
        )
    }

    private fun ResultRow.toBakaTitle(): MangaBakaTitle {
        return MangaBakaTitle(
            language = this[TitlesTable.language],
            title = this[TitlesTable.title],
            traits = this[TitlesTable.traits],
            isPrimary = this[TitlesTable.isPrimary],
            note = this[TitlesTable.note],
        )
    }

    private fun ResultRow.toBakaSeriesTag(): MangaBakaSeriesTag {
        return MangaBakaSeriesTag(
            id = MangaBakaTagId(this[TagsTable.id]),
            contentRating = MangaBakaContentRating.valueOf(this[TagsTable.contentRating]),
            description = this[TagsTable.description],
            level = this[TagsTable.level],
            name = this[TagsTable.name],
            namePath = this[TagsTable.namePath],
            parentId = this[TagsTable.parentId]?.let { MangaBakaTagId(it) },
            seriesCount = this[TagsTable.seriesCount],
            isGenre = this[TagsTable.isGenre],
            mergedWith = this[TagsTable.mergedWith],
            impliedByTagIds = this[SeriesTagsTable.impliedByTagIds],
            isSpoiler = this[SeriesTagsTable.isSpoiler],
            isExplicit = this[SeriesTagsTable.isExplicit],
            weight = MangaBakaTagWeight.valueOf(this[SeriesTagsTable.weight]),
        )
    }

    private fun ResultRow.toBakaTag(): MangaBakaTag {
        return MangaBakaTag(
            id = MangaBakaTagId(this[TagsTable.id]),
            contentRating = MangaBakaContentRating.valueOf(this[TagsTable.contentRating]),
            description = this[TagsTable.description],
            level = this[TagsTable.level],
            name = this[TagsTable.name],
            namePath = this[TagsTable.namePath],
            parentId = this[TagsTable.parentId]?.let { MangaBakaTagId(it) },
            seriesCount = this[TagsTable.seriesCount],
            isGenre = this[TagsTable.isGenre],
            mergedWith = this[TagsTable.mergedWith],
            isSpoiler = this[TagsTable.isSpoiler],
        )
    }

    private fun ResultRow.toBakaSeriesModel(
        authors: List<String>,
        artists: List<String>,
        links: List<MangaBakaLink>,
        publishers: List<MangaBakaPublisher>,
        relationships: List<MangaBakaRelationship>,
        titles: List<MangaBakaTitle>,
        tags: List<MangaBakaSeriesTag>,
    ): MangaBakaSeries {
        return MangaBakaSeries(
            id = MangaBakaSeriesId(this[SeriesTable.id]),
            state = MangaBakaSeriesState.valueOf(this[SeriesTable.state].uppercase()),
            mergedWith = this[SeriesTable.mergedWith],
            canonicalUrl = this[SeriesTable.canonicalUrl],
            cover = MangaBakaCover(
                raw = MangaBakaCoverRaw(
                    url = this[SeriesTable.coverRawUrl],
                    size = this[SeriesTable.coverRawSize],
                    height = this[SeriesTable.coverRawHeight],
                    width = this[SeriesTable.coverRawWidth],
                    blurhash = this[SeriesTable.coverRawBlurhash],
                    thumbhash = this[SeriesTable.coverRawThumbhash],
                    format = this[SeriesTable.coverRawFormat]
                ),
                x350 = this[SeriesTable.coverX350X1]?.let { MangaBakaCoverDpi(x1 = it) },
            ),
            authors = authors,
            artists = artists,
            description = this[SeriesTable.description],
            published = MangaBakaPublishedDate(
                endDate = this[SeriesTable.publishedEndDate]?.let { LocalDate.parse(it) },
                endDateIsEstimated = this[SeriesTable.publishedEndDateIsEstimated],
                startDate = this[SeriesTable.publishedStartDate]?.let { LocalDate.parse(it) },
                startDateIsEstimated = this[SeriesTable.publishedStartDateIsEstimated],
            ),
            status = MangaBakaStatus.valueOf(this[SeriesTable.status].uppercase()),
            isLicensed = this[SeriesTable.isLicenced],
            hasAnime = this[SeriesTable.hasAnime],
            anime = MangaBakaAnimeInfo(
                start = this[SeriesTable.animeStart],
                end = this[SeriesTable.animeEnd]
            ),
            contentRating = MangaBakaContentRating.valueOf(this[SeriesTable.contentRating].uppercase()),
            type = MangaBakaType.valueOf(this[SeriesTable.type].uppercase()),
            rating = this[SeriesTable.rating],
            finalVolume = this[SeriesTable.finalVolume],
            totalChapters = this[SeriesTable.totalChapters],
            linksV2 = links,
            publishers = publishers,
            titles = titles,
            tagsV2 = tags,
            lastUpdatedAt = Instant.parse(this[SeriesTable.lastUpdatedAt]),
            relationshipsV2 = relationships,
            source = MangaBakaSource(
                anilist = MangaBakaAniListSource(
                    id = this[SeriesTable.sourceAniListId],
                    rating = this[SeriesTable.sourceAniListRating],
                    ratingNormalized = this[SeriesTable.sourceAniListRatingNormalized],
                ),
                animeNewsNetwork = MangaBakaAnimeNewsNetworkSource(
                    id = this[SeriesTable.sourceAnimeNewsNetworkId],
                    rating = this[SeriesTable.sourceAnimeNewsNetworkRating],
                    ratingNormalized = this[SeriesTable.sourceAnimeNewsNetworkRatingNormalized],
                ),
                animePlanet = MangaBakaAnimePlanetSource(
                    id = this[SeriesTable.sourceAnimePlanetId],
                    rating = this[SeriesTable.sourceAnimePlanetRating],
                    ratingNormalized = this[SeriesTable.sourceAnimePlanetRatingNormalized],
                ),
                kitsu = MangaBakaKitsuSource(
                    id = this[SeriesTable.sourceKitsuId],
                    rating = this[SeriesTable.sourceKitsuRating],
                    ratingNormalized = this[SeriesTable.sourceKitsuRatingNormalized],
                ),
                mangaUpdates = MangaBakaMangaUpdatesSource(
                    id = this[SeriesTable.sourceMangaUpdatesId],
                    rating = this[SeriesTable.sourceMangaUpdatesRating],
                    ratingNormalized = this[SeriesTable.sourceMangaUpdatesRatingNormalized],
                ),
                myAnimeList = MangaBakaMyAnimeListSource(
                    id = this[SeriesTable.sourceMyAnimeListId],
                    rating = this[SeriesTable.sourceMyAnimeListRating],
                    ratingNormalized = this[SeriesTable.sourceMyAnimeListRatingNormalized],
                ),
                shikimori = MangaBakaShikimoriSource(
                    id = this[SeriesTable.sourceShikimoriId],
                    rating = this[SeriesTable.sourceShikimoriRating],
                    ratingNormalized = this[SeriesTable.sourceShikimoriRatingNormalized],
                ),
            )
        )
    }
}