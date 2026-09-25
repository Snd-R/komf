package snd.komf.mangabaka.repository.tables

import org.jetbrains.exposed.v1.core.Table

object ImportMetadataTable : Table("import_metadata") {
    val key = text("key").index()
    val value = text("value")

    override val primaryKey = PrimaryKey(key)
}