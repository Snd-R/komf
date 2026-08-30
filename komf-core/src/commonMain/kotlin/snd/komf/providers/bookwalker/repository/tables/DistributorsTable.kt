package snd.komf.providers.bookwalker.repository.tables

import org.jetbrains.exposed.v1.core.Table

object DistributorsTable : Table("distributors") {
    val id = text("id")
    val displayName = text("display_name")
    val legalName = text("legal_name")
    val codeName = text("code_name")
    val currencyCode = text("currency_code")

    override val primaryKey = PrimaryKey(id)
}