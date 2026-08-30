package snd.komf.providers.bookwalker.db.tables

import org.jetbrains.exposed.v1.core.Table

object ExportMetadataTable : Table("distributors") {
    val key = text("key")
    val value = text("value")

    override val primaryKey = PrimaryKey(key)
}