package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object ImagesTable : Table("images") {
    val id = text("id")
    val name = text("name")
    val mime = text("mime")
    val width = integer("width")
    val height = integer("height")

    override val primaryKey = PrimaryKey(id)
}