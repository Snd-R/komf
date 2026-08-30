package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object SeriesExternalIdsTable : Table("series_external_ids") {
    val id = text("id")
    val seriesId = text("series_id").references(SeriesTable.id)
    val type = integer("type")
    val externalId = text("external_id")
    val externalIdOriginal = text("external_id_original")

    override val primaryKey = PrimaryKey(id)
}