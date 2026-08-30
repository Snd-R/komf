package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object PricesTable : Table("prices") {
    val id = text("id")
    val productId = text("product_id").references(ProductsTable.id)
    val amount = integer("amount")
    val startAt = text("start_at")

    override val primaryKey = PrimaryKey(id)
}