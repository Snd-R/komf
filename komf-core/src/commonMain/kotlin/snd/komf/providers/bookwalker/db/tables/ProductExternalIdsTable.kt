package snd.komf.providers.bookwalker.db.tables

import org.jetbrains.exposed.v1.core.Table

object ProductExternalIdsTable : Table("product_external_ids") {
    val id = text("id")
    val productId = text("product_id").references(ProductsTable.id)
    val source1 = text("source")
    val type = integer("type")
    val externalId = text("external_id")
    val externalIdOriginal = text("external_id_original")

    override val primaryKey = PrimaryKey(id)
}