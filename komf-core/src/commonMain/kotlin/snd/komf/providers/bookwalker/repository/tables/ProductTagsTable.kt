package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object ProductTagsTable : Table("product_tags") {
    val productId = text("product_id").references(ProductsTable.id)
    val tagId = text("tag_id").references(TagsTable.id)

    override val primaryKey = PrimaryKey(productId, tagId)
}