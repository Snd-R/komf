package snd.komf.providers.mangabaka.db

import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import snd.komf.providers.mangabaka.MangaBakaAnilistSource
import snd.komf.providers.mangabaka.MangaBakaAnimeNewsNetworkSource
import snd.komf.providers.mangabaka.MangaBakaContentRating
import snd.komf.providers.mangabaka.MangaBakaCover
import snd.komf.providers.mangabaka.MangaBakaDataSource
import snd.komf.providers.mangabaka.MangaBakaKitsuSource
import snd.komf.providers.mangabaka.MangaBakaMangaDexSource
import snd.komf.providers.mangabaka.MangaBakaMangaUpdatesSource
import snd.komf.providers.mangabaka.MangaBakaMyAnimeListSource
import snd.komf.providers.mangabaka.MangaBakaPublisher
import snd.komf.providers.mangabaka.MangaBakaRelationships
import snd.komf.providers.mangabaka.MangaBakaSecondaryTitle
import snd.komf.providers.mangabaka.MangaBakaSeries
import snd.komf.providers.mangabaka.MangaBakaSeriesId
import snd.komf.providers.mangabaka.MangaBakaSeriesState
import snd.komf.providers.mangabaka.MangaBakaSources
import snd.komf.providers.mangabaka.MangaBakaStatus
import snd.komf.providers.mangabaka.MangaBakaType
import snd.komf.providers.mangabaka.db.MangaBakaSeriesTable.MangaBakaDbSecondaryTitle

class MangaBakaDbDataSource(
    private val database: Database,
) : MangaBakaDataSource {

    override suspend fun search(
        title: String,
        types: List<MangaBakaType>?
    ): List<MangaBakaSeries> {
        return transaction(database) {
            val ftsStatement = connection.prepareStatement(
                "SELECT * FROM series_fts WHERE title MATCH ? ORDER BY rank limit 10",
                false
            )
            ftsStatement[1] = title
            val resultSet = ftsStatement.executeQuery()
            val ids = buildList {
                while (resultSet.next()) add(resultSet.getInt("id"))
            }

            MangaBakaSeriesTable.selectAll()
                .where { MangaBakaSeriesTable.id.inList(ids) }
                .map { it.toModel() }
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
            title = this[MangaBakaSeriesTable.title],
            nativeTitle = this[MangaBakaSeriesTable.nativeTitle],
            romanizedTitle = this[MangaBakaSeriesTable.romanizedTitle],
            secondaryTitles = this.getSecondaryTitles(),
            cover = MangaBakaCover(
                raw = this[MangaBakaSeriesTable.coverRawUrl],
                default = this[MangaBakaSeriesTable.coverDefaultUrl],
                small = this[MangaBakaSeriesTable.coverSmalltUrl],
            ),
            authors = this[MangaBakaSeriesTable.authors],
            artists = this[MangaBakaSeriesTable.artists],
            description = this[MangaBakaSeriesTable.description],
            year = this[MangaBakaSeriesTable.year],
            status = MangaBakaStatus.valueOf(this[MangaBakaSeriesTable.status].uppercase()),
            isLicensed = this[MangaBakaSeriesTable.isLicenced],
            hasAnime = this[MangaBakaSeriesTable.hasAnime],
            anime = null,
            contentRating = MangaBakaContentRating.valueOf(this[MangaBakaSeriesTable.contentRating].uppercase()),
            type = MangaBakaType.valueOf(this[MangaBakaSeriesTable.type].uppercase()),
            rating = this[MangaBakaSeriesTable.rating],
            finalVolume = this[MangaBakaSeriesTable.finalVolume]?.toString(),
            finalChapter = this[MangaBakaSeriesTable.finalChapter]?.toString(),
            totalChapter = this[MangaBakaSeriesTable.totalChapters]?.toString(),
            links = this[MangaBakaSeriesTable.links],
            publishers = this[MangaBakaSeriesTable.publishers]?.map {
                MangaBakaPublisher(
                    name = it.name,
                    note = it.note,
                    type = it.type
                )
            },
            genres = this[MangaBakaSeriesTable.genres],
            tags = this[MangaBakaSeriesTable.tags],
            lastUpdatedAt = Instant.parse(this[MangaBakaSeriesTable.lastUpdatedAt]),
            relationships = this.mapRelationships(),
            source = MangaBakaSources(
                anilist = MangaBakaAnilistSource(
                    this[MangaBakaSeriesTable.sourceAnilistId],
                    this[MangaBakaSeriesTable.sourceAnilistRating]
                ),
                animeNewsNetwork = MangaBakaAnimeNewsNetworkSource(
                    this[MangaBakaSeriesTable.sourceAnimeNewNetworkId],
                    this[MangaBakaSeriesTable.sourceAnimeNewNetworkRating]
                ),
                kitsu = MangaBakaKitsuSource(
                    this[MangaBakaSeriesTable.sourceKitsuId],
                    this[MangaBakaSeriesTable.sourceKitsuRating]
                ),
                mangaUpdates = MangaBakaMangaUpdatesSource(
                    this[MangaBakaSeriesTable.sourceMangaUpdatesId],
                    this[MangaBakaSeriesTable.sourceMangaUpdatesRating]
                ),
                mangadex = MangaBakaMangaDexSource(
                    this[MangaBakaSeriesTable.sourceMangaDexId],
                    this[MangaBakaSeriesTable.sourceMangaDexRating]
                ),
                myAnimeList = MangaBakaMyAnimeListSource(
                    this[MangaBakaSeriesTable.sourceMyAnimeListId],
                    this[MangaBakaSeriesTable.sourceMyAnimeListRating]
                )
            )
        )
    }

    private fun ResultRow.mapRelationships(): MangaBakaRelationships? {
        val mainStory = this[MangaBakaSeriesTable.relationshipsMainStory]
        val adaptation = this[MangaBakaSeriesTable.relationshipsAdaptation]
        val prequel = this[MangaBakaSeriesTable.relationshipsPrequel]
        val sequel = this[MangaBakaSeriesTable.relationshipsSequel]
        val sideStory = this[MangaBakaSeriesTable.relationshipsSideStory]
        val spinOff = this[MangaBakaSeriesTable.relationshipsSpinOff]
        val alternative = this[MangaBakaSeriesTable.relationshipsAlternative]
        val other = this[MangaBakaSeriesTable.relationshipsOther]
        if (mainStory == null && adaptation == null && prequel == null && sequel == null && sideStory == null && spinOff == null && alternative == null && other == null) {
            return null
        }
        return MangaBakaRelationships(
            mainStory = mainStory,
            adaptation = adaptation,
            prequel = prequel,
            sequel = sequel,
            sideStory = sideStory,
            spinOff = spinOff,
            alternative = alternative,
            other = other
        )
    }

    private fun ResultRow.getSecondaryTitles(): Map<String, List<MangaBakaSecondaryTitle>> {
        val secondaryTitles = mutableMapOf<String, List<MangaBakaSecondaryTitle>>()

        fun MangaBakaDbSecondaryTitle.toMangaBakaTitle() =
            MangaBakaSecondaryTitle(type = this.type, title = this.title)

        this[MangaBakaSeriesTable.secondaryTitlesEn]?.let { titles ->
            secondaryTitles.put("en", titles.map { it.toMangaBakaTitle() })
        }
        this[MangaBakaSeriesTable.secondaryTitlesJa]?.let { titles ->
            secondaryTitles.put("ja", titles.map { it.toMangaBakaTitle() })
        }
        this[MangaBakaSeriesTable.secondaryTitlesJaRo]?.let { titles ->
            secondaryTitles.put("ja-ro", titles.map { it.toMangaBakaTitle() })
        }
        this[MangaBakaSeriesTable.secondaryTitlesEs]?.let { titles ->
            secondaryTitles.put("es", titles.map { it.toMangaBakaTitle() })
        }
        this[MangaBakaSeriesTable.secondaryTitlesFr]?.let { titles ->
            secondaryTitles.put("fr", titles.map { it.toMangaBakaTitle() })
        }
        this[MangaBakaSeriesTable.secondaryTitlesDe]?.let { titles ->
            secondaryTitles.put("de", titles.map { it.toMangaBakaTitle() })
        }
        return secondaryTitles
    }
}