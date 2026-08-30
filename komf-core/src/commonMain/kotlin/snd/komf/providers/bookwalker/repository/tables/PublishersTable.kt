package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object PublishersTable : Table("publishers") {
    val id = text("id")
    val displayName = text("display_name").nullable()
    val aliases = text("aliases")
    val country = text("country")

    override val primaryKey = PrimaryKey(id)
}