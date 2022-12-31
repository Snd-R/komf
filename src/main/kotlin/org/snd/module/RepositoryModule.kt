package org.snd.module

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.snd.config.DatabaseConfig
import org.snd.db.JooqKavitaBookThumbnailsRepository
import org.snd.db.JooqKavitaSeriesMatchRepository
import org.snd.db.JooqKavitaSeriesThumbnailsRepository
import org.snd.db.JooqKomgaBookThumbnailsRepository
import org.snd.db.JooqKomgaSeriesMatchRepository
import org.snd.db.JooqKomgaSeriesThumbnailsRepository
import org.snd.mediaserver.repository.BookThumbnailsRepository
import org.snd.mediaserver.repository.SeriesMatchRepository
import org.snd.mediaserver.repository.SeriesThumbnailsRepository


class RepositoryModule(
    config: DatabaseConfig,
) : AutoCloseable {
    private val datasource: HikariDataSource
    private val dsl: DSLContext

    init {
        Flyway(
            Flyway.configure()
                .dataSource("jdbc:sqlite:${config.file}", null, null)
                .locations("classpath:db/migration/sqlite")
        ).migrate()

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:sqlite:${config.file}"
        hikariConfig.maximumPoolSize = 1
        this.datasource = HikariDataSource(hikariConfig)

        this.dsl = DSL.using(datasource, SQLDialect.SQLITE)
        System.getProperties().setProperty("org.jooq.no-logo", "true")
        System.getProperties().setProperty("org.jooq.no-tips", "true")
    }


    val komgaSeriesThumbnailsRepository: SeriesThumbnailsRepository = JooqKomgaSeriesThumbnailsRepository(dsl)
    val komgaBookThumbnailsRepository: BookThumbnailsRepository = JooqKomgaBookThumbnailsRepository(dsl)
    val komgaSeriesMatchRepository: SeriesMatchRepository = JooqKomgaSeriesMatchRepository(dsl)

    val kavitaSeriesThumbnailsRepository: SeriesThumbnailsRepository = JooqKavitaSeriesThumbnailsRepository(dsl)
    val kavitaBookThumbnailsRepository: BookThumbnailsRepository = JooqKavitaBookThumbnailsRepository(dsl)
    val kavitaSeriesMatchRepository: SeriesMatchRepository = JooqKavitaSeriesMatchRepository(dsl)

    override fun close() {
        datasource.close()
    }
}
