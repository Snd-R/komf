package snd.komf.providers.bookwalker.db.tables

import org.jetbrains.exposed.v1.core.Table

object SeriesTagsTable : Table("series_tags") {
    val seriesId = text("series_id").references(SeriesTable.id)
    val tagId = text("tag_id").references(TagsTable.id)

    override val primaryKey = PrimaryKey(seriesId, tagId)
}