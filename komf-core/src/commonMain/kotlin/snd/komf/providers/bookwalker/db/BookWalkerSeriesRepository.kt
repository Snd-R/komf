package snd.komf.providers.bookwalker.db

import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.api.JdbcPreparedStatementApi
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcResult
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import snd.komf.providers.bookwalker.model.BookWalkerBook
import snd.komf.providers.bookwalker.model.BookWalkerBookFormat
import snd.komf.providers.bookwalker.model.BookWalkerBookId
import snd.komf.providers.bookwalker.model.BookWalkerContentType
import snd.komf.providers.bookwalker.model.BookWalkerContributor
import snd.komf.providers.bookwalker.model.BookWalkerContributorId
import snd.komf.providers.bookwalker.model.BookWalkerContributorRole
import snd.komf.providers.bookwalker.model.BookWalkerImage
import snd.komf.providers.bookwalker.model.BookWalkerImageId
import snd.komf.providers.bookwalker.model.BookWalkerSeries
import snd.komf.providers.bookwalker.model.BookWalkerSeriesId
import snd.komf.providers.bookwalker.model.BookWalkerTag
import snd.komf.providers.bookwalker.model.BookWalkerTagId
import snd.komf.providers.bookwalker.db.tables.ContributorsTable
import snd.komf.providers.bookwalker.db.tables.ImagesTable
import snd.komf.providers.bookwalker.db.tables.ProductContributorsTable
import snd.komf.providers.bookwalker.db.tables.ProductExternalIdsTable
import snd.komf.providers.bookwalker.db.tables.ProductTagsTable
import snd.komf.providers.bookwalker.db.tables.ProductsTable
import snd.komf.providers.bookwalker.db.tables.SeriesTable
import snd.komf.providers.bookwalker.db.tables.SeriesTagsTable
import snd.komf.providers.bookwalker.db.tables.TagsTable
import kotlin.time.Instant

class BookWalkerSeriesRepository(
    private val database: Database,
) {

    fun search(title: String, contentTypes: List<BookWalkerContentType>): List<BookWalkerSeries> {
        return transaction(database) {
            var ftsStatement: JdbcPreparedStatementApi? = null
            var result: JdbcResult? = null

            try {
                val sqlString = buildString {
                    append("SELECT id FROM series_fts WHERE titles MATCH ?")
                    contentTypes.ifEmpty { null }?.joinToString(", ") { "?" }?.let { append(" AND type IN (${it})") }
                    append(" ORDER BY rank LIMIT 10")
                }

                ftsStatement = connection.prepareStatement(sqlString, false)
                ftsStatement.set(1, "\"$title\"", TextColumnType())
                var statementCurrentIndex = 1
                contentTypes.forEach { type ->
                    statementCurrentIndex += 1
                    ftsStatement.set(
                        statementCurrentIndex,
                        type.number,
                        IntegerColumnType()
                    )
                }

                result = ftsStatement.executeQuery()
                val rs = result.result
                val ids = buildList { while (rs.next()) add(rs.getString("id")) }
                if (ids.isEmpty()) return@transaction emptyList()

                SeriesTable.join(
                    otherTable = ImagesTable,
                    joinType = JoinType.LEFT,
                    onColumn = SeriesTable.imageId,
                    otherColumn = ImagesTable.id,
                ).selectAll()
                    .where { SeriesTable.id.inList(ids) }
                    .fetchAndMapSeries()
            } finally {
                result?.close()
                ftsStatement?.closeIfPossible()
            }
        }
    }

    fun getSeries(seriesId: BookWalkerSeriesId): BookWalkerSeries {
        return transaction(database) {
            SeriesTable.join(
                otherTable = ImagesTable,
                joinType = JoinType.LEFT,
                onColumn = SeriesTable.imageId,
                otherColumn = ImagesTable.id,
            ).selectAll()
                .where { SeriesTable.id.eq(seriesId.value) }
                .fetchAndMapSeries()
                .firstOrNull()
                ?: throw IllegalStateException("failed to find series with id $seriesId")
        }
    }

    fun getSeriesBooks(seriesId: BookWalkerSeriesId): List<BookWalkerBook> {
        return transaction(database) {
            ProductsTable
                .join(
                    otherTable = ImagesTable,
                    joinType = JoinType.LEFT,
                    onColumn = ProductsTable.imageId,
                    otherColumn = ImagesTable.id,
                )
                .join(
                    otherTable = ProductExternalIdsTable,
                    joinType = JoinType.LEFT,
                    onColumn = ProductsTable.id,
                    otherColumn = ProductExternalIdsTable.productId,
                    additionalConstraint = { ProductExternalIdsTable.type.eq(3) }
                )
                .selectAll()
                .where { ProductsTable.seriesId.eq(seriesId.value) }
                .fetchAndMapBooks()
        }
    }

    fun getBook(bookId: BookWalkerBookId): BookWalkerBook {
        return transaction(database) {
            ProductsTable
                .join(
                    otherTable = ImagesTable,
                    joinType = JoinType.LEFT,
                    onColumn = ProductsTable.imageId,
                    otherColumn = ImagesTable.id,
                )
                .join(
                    otherTable = ProductExternalIdsTable,
                    joinType = JoinType.LEFT,
                    onColumn = ProductsTable.id,
                    otherColumn = ProductExternalIdsTable.productId,
                    additionalConstraint = { ProductExternalIdsTable.type.eq(3) }
                )
                .selectAll()
                .where { ProductsTable.id.eq(bookId.value) }
                .fetchAndMapBooks()
                .firstOrNull()
                ?: throw IllegalStateException("failed to find book with id $bookId")
        }
    }

    private fun Query.fetchAndMapBooks(): List<BookWalkerBook> {
        val rows = this.toList()
        val bookIds = rows.map { it[ProductsTable.id] }
        val tags = selectBookTags(bookIds)
        val contributors = selectContributors(bookIds)

        return rows.map { row ->
            val bookId = row[ProductsTable.id]
            row.toBookModel(
                tags[bookId].orEmpty(),
                contributors[bookId].orEmpty(),
            )
        }
    }

    private fun Query.fetchAndMapSeries(): List<BookWalkerSeries> {
        val rows = this.toList()
        val seriesIds = rows.map { it[SeriesTable.id] }
        val tags = selectSeriesTags(seriesIds)

        return rows.map { row ->
            val seriesId = row[SeriesTable.id]
            row.toSeriesModel(tags[seriesId].orEmpty())
        }
    }

    private fun selectSeriesTags(seriesIds: List<String>): Map<String, List<BookWalkerTag>> {
        return SeriesTagsTable
            .join(
                otherTable = TagsTable,
                joinType = JoinType.LEFT,
                onColumn = SeriesTagsTable.tagId,
                otherColumn = TagsTable.id,
            )
            .selectAll()
            .where { SeriesTagsTable.seriesId.inList(seriesIds) }
            .groupBy(
                { it[SeriesTagsTable.seriesId] },
                { it.toTagModel() }
            )
    }

    private fun selectBookTags(bookIds: List<String>): Map<String, List<BookWalkerTag>> {
        return ProductTagsTable
            .join(
                otherTable = TagsTable,
                joinType = JoinType.LEFT,
                onColumn = ProductTagsTable.tagId,
                otherColumn = TagsTable.id,
            )
            .selectAll()
            .where { ProductTagsTable.productId.inList(bookIds) }
            .groupBy(
                { it[ProductTagsTable.productId] },
                { it.toTagModel() }
            )
    }

    private fun selectContributors(bookIds: List<String>): Map<String, List<BookWalkerContributor>> {
        return ProductContributorsTable
            .join(
                otherTable = ContributorsTable,
                joinType = JoinType.LEFT,
                onColumn = ProductContributorsTable.contributorId,
                otherColumn = ContributorsTable.id,
            )
            .selectAll()
            .where { ProductContributorsTable.productId.inList(bookIds) }
            .groupBy(
                { it[ProductContributorsTable.productId] },
                { it.toContributorModel() }
            )

    }

    private fun ResultRow.toTagModel(): BookWalkerTag {
        return BookWalkerTag(
            id = BookWalkerTagId(this[TagsTable.id]),
            name = this[TagsTable.name],
            slug = this[TagsTable.slug],
            description = this[TagsTable.description],
            namespace = this[TagsTable.namespace],
            priority = this[TagsTable.priority]
        )
    }

    private fun ResultRow.toContributorModel(): BookWalkerContributor {
        return BookWalkerContributor(
            id = BookWalkerContributorId(this[ContributorsTable.id]),
            role = BookWalkerContributorRole.valueOf(this[ProductContributorsTable.role]),
            name = this[ContributorsTable.name],
        )
    }

    private fun ResultRow.toSeriesModel(
        tags: List<BookWalkerTag>,
    ): BookWalkerSeries {
        return BookWalkerSeries(
            id = BookWalkerSeriesId(this[SeriesTable.id]),
            type = BookWalkerContentType.valueOf(this[SeriesTable.type]),
            title = this[SeriesTable.title],
            altTitles = this[SeriesTable.altTitles],
            subtitle = this[SeriesTable.subtitle],
            displayTitle = this[SeriesTable.displayTitle],
            displayTitleShort = this[SeriesTable.displayTitleShort],
            description = this[SeriesTable.description],
            descriptionShort = this[SeriesTable.descriptionShort],
            listedAt = Instant.parse(this[SeriesTable.listedAt]),
            tags = tags,
            image = this.toImageModel()
        )
    }

    private fun ResultRow.toBookModel(
        tags: List<BookWalkerTag>,
        contributors: List<BookWalkerContributor>,
    ): BookWalkerBook {

        return BookWalkerBook(
            id = BookWalkerBookId(this[ProductsTable.id]),
            contentId = this[ProductsTable.contentId],
            seriesId = BookWalkerSeriesId(this[ProductsTable.seriesId]),
            level = this[ProductsTable.level],
            contentType = BookWalkerContentType.valueOf(this[ProductsTable.contentType]),
            format = BookWalkerBookFormat.valueOf(this[ProductsTable.productType]),
            title = this[ProductsTable.title],
            altTitles = this[ProductsTable.altTitles],
            subtitle = this[ProductsTable.subtitle],
            displayTitle = this[ProductsTable.displayTittle],
            displayTitleShort = this[ProductsTable.displayTittleShort],
            description = this[ProductsTable.description],
            descriptionShort = this[ProductsTable.descriptionShort],
            displayOrder = this[ProductsTable.displayOrder],
            listedAt = Instant.parse(this[ProductsTable.listedAt]),
            labelId = this[ProductsTable.labelId],
            geoblockId = this[ProductsTable.geoblockId],
            displayName = this[ProductsTable.displayName],
            copyright = this[ProductsTable.copyright],
            onPresaleAt = this[ProductsTable.onPresaleAt]?.let { Instant.parse(it) },
            onSaleAt = Instant.parse(this[ProductsTable.onSaleAt]),
            offSaleAt = this[ProductsTable.offSaleAt]?.let { Instant.parse(it) },
            addOn = this[ProductsTable.addOn],
            addOnCampaignOnly = this[ProductsTable.addOnCampaignOnly],
            tags = tags,
            contributors = contributors,
            image = this.toImageModel(),
            isbn = this.getOrNull(ProductExternalIdsTable.externalId)
        )
    }

    private fun ResultRow.toImageModel(): BookWalkerImage? {
        val imageId = this.getOrNull(ImagesTable.id) ?: return null

        return BookWalkerImage(
            id = BookWalkerImageId(imageId),
            name = this[ImagesTable.name],
            mime = this[ImagesTable.name],
            width = this[ImagesTable.width],
            height = this[ImagesTable.height]
        )
    }

}