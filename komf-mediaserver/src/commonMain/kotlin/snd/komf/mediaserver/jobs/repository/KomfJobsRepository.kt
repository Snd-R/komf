package snd.komf.mediaserver.jobs.repository

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import snd.komf.mediaserver.jobs.MetadataJob
import snd.komf.mediaserver.jobs.MetadataJobId
import snd.komf.mediaserver.jobs.MetadataJobStatus
import snd.komf.mediaserver.model.MediaServerSeriesId
import kotlin.time.Instant
import kotlin.uuid.Uuid


class KomfJobsRepository(
    private val database: Database,
) {

    fun get(id: MetadataJobId): MetadataJob? {
        return transaction(database) {
            KomfJobsTable.selectAll()
                .where { KomfJobsTable.id.eq(id.value.toHexDashString()) }
                .firstOrNull()
                ?.fromRecord()
        }
    }

    fun countAll(
        status: MetadataJobStatus? = null,
    ): Long {
        return transaction(database) {
            KomfJobsTable.select(KomfJobsTable.id.count()).apply {
                if (status != null) where { KomfJobsTable.status.eq(status.name) }
            }.first()[KomfJobsTable.id.count()]
        }
    }

    fun findAll(
        status: MetadataJobStatus? = null,
        limit: Int = 1000,
        offset: Long = 0
    ): List<MetadataJob> {
        return transaction(database) {
            KomfJobsTable.selectAll()
                .apply { if (status != null) where { KomfJobsTable.status.eq(status.name) } }
                .limit(limit)
                .offset(offset)
                .map { it.fromRecord() }
        }
    }

    fun save(job: MetadataJob) {
        return transaction(database) {
            KomfJobsTable.upsert {
                it[KomfJobsTable.id] = job.id.value.toHexDashString()
                it[KomfJobsTable.seriesId] = job.seriesId.value
                it[KomfJobsTable.status] = job.status.name
                it[KomfJobsTable.message] = job.message
                it[KomfJobsTable.startedAt] = job.startedAt.toEpochMilliseconds()
                it[KomfJobsTable.finishedAt] = job.finishedAt?.toEpochMilliseconds()
            }
        }
    }

    fun cancelAllRunning() {
        transaction(database) {
            KomfJobsTable.update(where = { KomfJobsTable.status.eq(MetadataJobStatus.RUNNING.name) }) {
                it[KomfJobsTable.status] = MetadataJobStatus.FAILED.name
                it[KomfJobsTable.message] = "Cancelled"
            }
        }
    }

    fun deleteAllBeforeDate(instant: Instant) {
        transaction(database) {
            KomfJobsTable.deleteWhere {
                KomfJobsTable.startedAt.lessEq(instant.toEpochMilliseconds())
            }
        }
    }

    fun deleteAll() {
        transaction(database) {
            KomfJobsTable.deleteAll()
        }
    }

    private fun ResultRow.fromRecord() = MetadataJob(
        id = MetadataJobId(Uuid.parseHexDash(this[KomfJobsTable.id])),
        seriesId = MediaServerSeriesId(this[KomfJobsTable.seriesId]),
        status = MetadataJobStatus.valueOf(this[KomfJobsTable.status]),
        message = this[KomfJobsTable.message],
        startedAt = Instant.fromEpochMilliseconds(this[KomfJobsTable.startedAt]),
        finishedAt = this[KomfJobsTable.finishedAt]?.let { Instant.fromEpochMilliseconds(it) }
    )
}