package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table

object RelationshipsTable : Table("relationships") {
    val seriesId = long("series_id").references(SeriesTable.id).index()
    val chronology = text("chronology")
    val isManual = bool("is_manual")
    val note = text("note").nullable()
    val relationType = text("relation_type")

    val idUnused = text("id_unused")

}