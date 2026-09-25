package snd.komf.mediaserver.match.repository.tables

import org.jetbrains.exposed.v1.core.Table

object SeriesMatchTable : Table("SeriesMatch") {
    val seriesId = text("seriesId")
    val type = text("type")
    val mediaServer = text("mediaServer")
    val provider = text("provider")
    val providerSeriesId = text("providerSeriesId")

    override val primaryKey = PrimaryKey(seriesId, mediaServer)
}