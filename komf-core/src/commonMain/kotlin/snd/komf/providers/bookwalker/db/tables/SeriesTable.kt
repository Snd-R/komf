package snd.komf.providers.bookwalker.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json

object SeriesTable : Table("series") {
    val id = text("id")
    val type = integer("type")
    val title = text("title")
    val altTitles = json<List<String>>("alt_titles", json)
    val subtitle = text("subtitle")
    val displayTitle = text("display_title")
    val displayTitleShort = text("display_title_short")
    val description = text("description")
    val descriptionShort = text("description_short")
    val imageId = text("image_id")
    val listedAt = text("listed_at")

    override val primaryKey = PrimaryKey(id)
}