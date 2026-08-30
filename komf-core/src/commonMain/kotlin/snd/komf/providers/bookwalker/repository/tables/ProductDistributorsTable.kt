package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object ProductDistributorsTable : Table("product_distributors") {
    val productId = text("product_id").references(ProductsTable.id)
    val distributorId = text("distributor_id").references(DistributorsTable.id)

    override val primaryKey = PrimaryKey(productId, distributorId)
}