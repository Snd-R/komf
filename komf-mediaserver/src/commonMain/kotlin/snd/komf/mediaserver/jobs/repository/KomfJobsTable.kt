package snd.komf.mediaserver.jobs.repository

import org.jetbrains.exposed.v1.core.Table

object KomfJobsTable : Table("KomfJobRecord") {
    val id = text("id")
    val seriesId = text("seriesId")
    val status = text("status")
    val message = text("message").nullable()
    val startedAt = long("startedAt")
    val finishedAt = long("finishedAt").nullable()

    override val primaryKey = PrimaryKey(id)
}