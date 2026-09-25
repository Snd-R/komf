package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table

object PublishersTable : Table("publishers") {
    val seriesId = long("seriesId").references(SeriesTable.id).index()
    val name = text("name")
    val type = text("type")
    val note = text("note").nullable()
}