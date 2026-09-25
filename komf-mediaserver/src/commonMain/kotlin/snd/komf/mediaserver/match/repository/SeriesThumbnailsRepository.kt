package snd.komf.mediaserver.match.repository

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import snd.komf.mediaserver.match.SeriesThumbnailMatch
import snd.komf.mediaserver.match.repository.tables.SeriesThumbnailTable
import snd.komf.mediaserver.model.MediaServer
import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.mediaserver.model.MediaServerThumbnailId

class SeriesThumbnailsRepository(
    private val database: Database,
    private val mediaServer: MediaServer
) {

    fun findFor(seriesId: MediaServerSeriesId): SeriesThumbnailMatch? {
        return transaction(database) {
            SeriesThumbnailTable.selectAll().where {
                SeriesThumbnailTable.seriesId.eq(seriesId.value)
            }.firstOrNull()?.toModel()
        }
    }

    fun save(
        seriesId: MediaServerSeriesId,
        thumbnailId: MediaServerThumbnailId,
    ) {
        transaction(database) {
            SeriesThumbnailTable.upsert {
                it[SeriesThumbnailTable.seriesId] = seriesId.value
                it[SeriesThumbnailTable.thumbnailId] = thumbnailId.value
                it[SeriesThumbnailTable.mediaServer] = this@SeriesThumbnailsRepository.mediaServer.name
            }
        }
    }

    fun delete(seriesId: MediaServerSeriesId) {
        transaction(database) {
            SeriesThumbnailTable.deleteWhere { SeriesThumbnailTable.seriesId.eq(seriesId.value) }
        }
    }

    private fun ResultRow.toModel(): SeriesThumbnailMatch {
        return SeriesThumbnailMatch(
            seriesId = MediaServerSeriesId(this[SeriesThumbnailTable.seriesId]),
            thumbnailId = MediaServerThumbnailId(this[SeriesThumbnailTable.thumbnailId]),
            mediaServer = MediaServer.valueOf(this[SeriesThumbnailTable.mediaServer])
        )
    }
}