package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table

object KomgaSeriesTable : Table("komga_series") {
    val komgaId = text("komga_id").index()
    val mangaBakaId = long("mangabaka_id").references(SeriesTable.id).index()
    override val primaryKey = PrimaryKey(komgaId, mangaBakaId)
}