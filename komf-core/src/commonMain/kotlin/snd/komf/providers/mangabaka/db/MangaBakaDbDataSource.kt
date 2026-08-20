package snd.komf.providers.mangabaka.db

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.api.JdbcPreparedStatementApi
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcResult
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import snd.komf.providers.mangabaka.MangaBakaAnimeInfo
import snd.komf.providers.mangabaka.MangaBakaContentRating
import snd.komf.providers.mangabaka.MangaBakaCover
import snd.komf.providers.mangabaka.MangaBakaCoverDpi
import snd.komf.providers.mangabaka.MangaBakaCoverRaw
import snd.komf.providers.mangabaka.MangaBakaDataSource
import snd.komf.providers.mangabaka.MangaBakaPublishedDate
import snd.komf.providers.mangabaka.MangaBakaPublisher
import snd.komf.providers.mangabaka.MangaBakaSeries
import snd.komf.providers.mangabaka.MangaBakaSeriesId
import snd.komf.providers.mangabaka.MangaBakaSeriesState
import snd.komf.providers.mangabaka.MangaBakaStatus
import snd.komf.providers.mangabaka.MangaBakaType
import kotlin.time.Instant

class MangaBakaDbDataSource(
    private val database: Database,
) : MangaBakaDataSource {

    override suspend fun search(
        title: String,
        types: List<MangaBakaType>?,
        typesNot: List<MangaBakaType>?
    ): List<MangaBakaSeries> {
        return transaction(database) {
            var ftsStatement: JdbcPreparedStatementApi? = null
            var result: JdbcResult? = null
            try {
                val sqlString = buildString {
                    append("SELECT id FROM series_fts WHERE titles MATCH ?")
                    types?.joinToString(", ") { "?" }?.let { append(" AND type IN ($it)") }
                    typesNot?.joinToString(", ") { "?" }?.let { append(" AND type NOT IN ($it)") }
                    append(" ORDER BY rank LIMIT 10")
                }

                ftsStatement = connection.prepareStatement(sqlString, false)
                ftsStatement.set(1, "\"$title\"", TextColumnType())
                var statementCurrentIndex = 1
                types?.forEach { value ->
                    statementCurrentIndex += 1
                    ftsStatement.set(
                        statementCurrentIndex,
                        value.name.lowercase(),
                        TextColumnType()
                    )
                }
                typesNot?.forEach { value ->
                    statementCurrentIndex += 1
                    ftsStatement.set(
                        statementCurrentIndex,
                        value.name.lowercase(),
                        TextColumnType()
                    )
                }

                result = ftsStatement.executeQuery()
                val rs = result.result
                val ids = buildList { while (rs.next()) add(rs.getInt("id")) }

                MangaBakaSeriesTable.selectAll()
                    .where { MangaBakaSeriesTable.id.inList(ids) }
                    .map { it.toModel() }
            } finally {
                result?.close()
                ftsStatement?.closeIfPossible()
            }
        }
    }

    override suspend fun getSeries(id: MangaBakaSeriesId): MangaBakaSeries {
        return transaction(database) {
            MangaBakaSeriesTable.selectAll()
                .where { MangaBakaSeriesTable.id.eq(id.value) }
                .first()
                .toModel()
        }
    }

    private fun ResultRow.toModel(): MangaBakaSeries {
        return MangaBakaSeries(
            id = MangaBakaSeriesId(this[MangaBakaSeriesTable.id]),
            state = MangaBakaSeriesState.valueOf(this[MangaBakaSeriesTable.state].uppercase()),
            mergedWith = this[MangaBakaSeriesTable.mergedWith],
            cover = MangaBakaCover(
                raw = MangaBakaCoverRaw(
                    url = this[MangaBakaSeriesTable.coverRawUrl],
                    size = this[MangaBakaSeriesTable.coverRawSize],
                    height = this[MangaBakaSeriesTable.coverRawHeight],
                    width = this[MangaBakaSeriesTable.coverRawWidth],
                    blurhash = this[MangaBakaSeriesTable.coverRawBlurhash],
                    thumbhash = this[MangaBakaSeriesTable.coverRawThumbhash],
                    format = this[MangaBakaSeriesTable.coverRawFormat]
                ),
                x350 = this[MangaBakaSeriesTable.coverX350X1Url]?.let { MangaBakaCoverDpi(x1 = it) },
            ),
            authors = this[MangaBakaSeriesTable.authors],
            artists = this[MangaBakaSeriesTable.artists],
            description = this[MangaBakaSeriesTable.description],
            published = MangaBakaPublishedDate(
                endDate = this[MangaBakaSeriesTable.publishedEndDate]?.let { LocalDate.parse(it) },
                endDateIsEstimated = this[MangaBakaSeriesTable.publishedEndDateIsEstimated],
                startDate = this[MangaBakaSeriesTable.publishedStartDate]?.let { LocalDate.parse(it) },
                startDateIsEstimated = this[MangaBakaSeriesTable.publishedStartDateIsEstimated],
            ),
            status = MangaBakaStatus.valueOf(this[MangaBakaSeriesTable.status].uppercase()),
            isLicensed = this[MangaBakaSeriesTable.isLicenced],
            hasAnime = this[MangaBakaSeriesTable.hasAnime],
            anime = MangaBakaAnimeInfo(
                start = this[MangaBakaSeriesTable.anime_start],
                end = this[MangaBakaSeriesTable.anime_end]
            ),
            contentRating = MangaBakaContentRating.valueOf(this[MangaBakaSeriesTable.contentRating].uppercase()),
            type = MangaBakaType.valueOf(this[MangaBakaSeriesTable.type].uppercase()),
            rating = this[MangaBakaSeriesTable.rating],
            finalVolume = this[MangaBakaSeriesTable.finalVolume],
            totalChapters = this[MangaBakaSeriesTable.totalChapters],
            linksV2 = this[MangaBakaSeriesTable.linksV2],
            publishers = this[MangaBakaSeriesTable.publishers]?.map {
                MangaBakaPublisher(
                    name = it.name,
                    note = it.note,
                    type = it.type
                )
            },
            titles = this[MangaBakaSeriesTable.titles],
            tagsV2 = this[MangaBakaSeriesTable.tagsV2],
            lastUpdatedAt = Instant.parse(this[MangaBakaSeriesTable.lastUpdatedAt]),
            relationshipsV2 = this[MangaBakaSeriesTable.relationshipsV2],
        )
    }
}