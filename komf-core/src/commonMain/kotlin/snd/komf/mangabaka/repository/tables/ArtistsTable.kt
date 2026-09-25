package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table

object ArtistsTable : Table("artists") {
    val seriesId = long("series_id").references(SeriesTable.id).index()
    val name = text("name")
}