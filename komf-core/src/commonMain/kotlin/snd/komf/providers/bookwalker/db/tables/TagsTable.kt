package snd.komf.providers.bookwalker.db.tables

import org.jetbrains.exposed.v1.core.Table

object TagsTable : Table("tags") {
    val id = text("id")
    val name = text("name")
    val slug = text("slug")
    val description = text("description")
    val namespace = integer("namespace")
    val priority = integer("priority")

    override val primaryKey = PrimaryKey(id)
}