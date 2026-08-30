package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.json

object ProductsTable : Table("products") {
    val id = text("id")
    val contentId = text("content_id")
    val seriesId = text("series_id").references(SeriesTable.id)
    val parentContentId = text("parent_content_id").nullable()
    val level = integer("level")
    val contentType = integer("content_type")
    val productType = integer("product_type")
    val title = text("title")
    val altTitles = json<List<String>>("alt_titles", json)
    val subtitle = text("subtitle")
    val displayTittle = text("display_title")
    val displayTittleShort = text("display_title_short")
    val description = text("description")
    val descriptionShort = text("description_short")
    val imageId = text("image_id")
    val displayOrder = double("display_order")
    val listedAt = text("listed_at")
    val labelId = text("label_id").references(LabelsTable.id)
    val geoblockId = text("geoblock_id").references(GeoblocksTable.id).nullable()
    val displayName = text("display_name")
    val copyright = text("copyright").nullable()
    val onPresaleAt = text("on_presale_at").nullable()
    val onSaleAt = text("on_sale_at")
    val offSaleAt = text("off_sale_at").nullable()
    val addOn = integer("add_on")
    val addOnCampaignOnly = integer("add_on_campaign_only")

    override val primaryKey = PrimaryKey(id)
}