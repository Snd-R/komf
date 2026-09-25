package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table

object LinksTable : Table("links") {
    val seriesId = long("series_id").references(SeriesTable.id).index()
    val name = text("name")
    val nameDisplay = text("name_display")
    val language = text("language")
    val type = text("type")
    val url = text("url")

    val idUnused = text("id_unused")
}