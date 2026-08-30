package snd.komf.providers.bookwalker.db.tables

import org.jetbrains.exposed.v1.core.Table

object GeoblocksTerritoriesTable : Table("geoblock_territories") {
    val geoblockId = text("geoblock_id").references(GeoblocksTable.id)
    val territory = text("territory_id")
    val type = integer("type")

    override val primaryKey = PrimaryKey(geoblockId, territory, type)
}