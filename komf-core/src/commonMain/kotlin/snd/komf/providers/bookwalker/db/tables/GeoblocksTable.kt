package snd.komf.providers.bookwalker.db.tables

import org.jetbrains.exposed.v1.core.Table

object GeoblocksTable : Table("geoblocks") {
    val id = text("id")
    override val primaryKey = PrimaryKey(id)
}