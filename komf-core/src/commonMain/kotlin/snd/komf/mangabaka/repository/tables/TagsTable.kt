package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table

object TagsTable : Table("tags") {
    val id = long("id")
    val parentId = long("parent_id").nullable().index()
    val mergedWith = long("merged_with").nullable()
    val name = text("name")
    val namePath = text("name_path")
    val description = text("description").nullable()
    val isSpoiler = bool("is_spoiler")
    val isGenre = bool("is_genre")
    val contentRating = text("content_rating")
    val seriesCount = integer("series_count")
    val level = integer("level")

    override val primaryKey = PrimaryKey(id)
}