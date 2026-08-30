package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object LabelsTable : Table("labels") {
    val id = text("id")
    val publisherId = text("publisher_id").references(PublishersTable.id)
    val geoblockId = text("geoblock_id").references(GeoblocksTable.id).nullable()
    val name = text("name")
    val aliases = text("aliases")

    override val primaryKey = PrimaryKey(id)
}