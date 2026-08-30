package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table


object ContributorsTable : Table("contributors") {
    val id = text("id")
    val name = text("name")
    val nameAlt = text("name_alt")

    override val primaryKey = PrimaryKey(id)
}