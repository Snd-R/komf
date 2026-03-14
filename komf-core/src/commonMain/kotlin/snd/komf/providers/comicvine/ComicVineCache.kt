package snd.komf.providers.comicvine

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.datetime.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import java.io.File
import java.time.temporal.ChronoUnit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.plus
import kotlinx.datetime.DateTimeUnit

object QueriesTable : Table("queries") {
    val urlCol = text("url")
    override val primaryKey = PrimaryKey(urlCol)

    val createdAtCol = timestamp("created_at")

    val responseCol = text("response")
}

class ComicVineCache(
    private val databaseFile: String,
    private val expiry: Int,
) {
    private val databasePath = Path.of(databaseFile)
    private val database = Database.connect("jdbc:sqlite:$databasePath", driver = "org.sqlite.JDBC")

    init {
        transaction(db = database) {
            SchemaUtils.create(QueriesTable)
        }
    }

    private fun getExpiryTimestamp(): Instant {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
            .toInstant(TimeZone.UTC)
            .plus(value = expiry * 24, DateTimeUnit.HOUR)
    }

    private fun getNowTimestamp(): Instant {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.UTC)
            .toInstant(TimeZone.UTC)
    }

    private fun maskApiKey(url: String): String {
        return url.replace(
            Regex("""api_key=[^&]+"""),
            "api_key=*****"
        )
    }

    fun addEntry(url: String, response: String) {
        transaction(db = database) {
            QueriesTable.upsert {
                it[urlCol] = maskApiKey(url)
                it[responseCol] = response
                it[createdAtCol] = getExpiryTimestamp()
            }
        }
    }

    suspend fun getEntry(url: String): String? {
        if (expiry == 0) {
            return transaction(db = database) {
                QueriesTable
                    .select(QueriesTable.responseCol).where {
                        QueriesTable.urlCol eq maskApiKey(url)
                    }
                    .firstOrNull()
                    ?.get(QueriesTable.responseCol)
            }
        }

        return transaction(db = database) {
            QueriesTable
                .select(QueriesTable.responseCol).where {
                    (QueriesTable.urlCol eq maskApiKey(url)) and
                    (QueriesTable.createdAtCol greater getNowTimestamp())
                }
                .firstOrNull()
                ?.get(QueriesTable.responseCol)
        }
    }
}
