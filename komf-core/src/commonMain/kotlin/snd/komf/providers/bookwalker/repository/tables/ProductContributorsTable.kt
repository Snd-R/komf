package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object ProductContributorsTable : Table("product_contributors") {
    val productId = text("product_id").references(ProductsTable.id)
    val contributorId = text("contributor_id").references(ContributorsTable.id)
    val role = integer("role")
    val nameOverride = text("name_override").nullable()

    override val primaryKey = PrimaryKey(productId, contributorId, role)
}