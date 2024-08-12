package snd.komf.mediaserver.jobs

import snd.komf.mediaserver.repository.KomfJobRecord
import snd.komf.mediaserver.repository.KomfJobRecordQueries


class KomfJobsRepository(
    private val queries: KomfJobRecordQueries,
) {

    fun get(id: MetadataJobId): MetadataJob? {
        return queries.get(id).executeAsOneOrNull()?.fromRecord()
    }

    fun countAll(
        status: MetadataJobStatus? = null,
    ): Long {
        return if (status == null)
            queries.countAll().executeAsOne()
        else
            queries.countAllWithStatus(status).executeAsOne()
    }

    fun findAll(
        status: MetadataJobStatus? = null,
        limit: Long = 1000,
        offset: Long = 0
    ): List<MetadataJob> {
        val jobs = if (status == null)
            queries.findAll(limit, offset).executeAsList()
        else
            queries.findAllWithStatus(status, limit, offset).executeAsList()

        return jobs.map { it.fromRecord() }
    }

    fun save(job: MetadataJob) {
        queries.save(job.toRecord())
    }

    fun deleteAll(){
        queries.deleteAll()
    }

    private fun MetadataJob.toRecord() = KomfJobRecord(
        id = id,
        seriesId = seriesId,
        status = status,
        message = message,
        startedAt = startedAt,
        finishedAt = finishedAt
    )

    private fun KomfJobRecord.fromRecord() = MetadataJob(
        id = id,
        seriesId = seriesId,
        status = status,
        message = message,
        startedAt = startedAt,
        finishedAt = finishedAt
    )
}