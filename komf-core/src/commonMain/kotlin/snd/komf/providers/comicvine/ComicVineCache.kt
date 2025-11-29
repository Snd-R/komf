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

object CacheTable : Table("cache") {
    val queryCol = text("query")
    override val primaryKey = PrimaryKey(queryCol)

    val timestampCol = timestamp("timestamp")

    val responseCol = text("response")
}

class ComicVineCache(
    private val databaseFile: String,
    private val expiry: Int = 30,
) {
    private val databasePath = Path.of(databaseFile)
    private val database = Database.connect("jdbc:sqlite:$databasePath", driver = "org.sqlite.JDBC")

    init {
        transaction(db = database) {
            SchemaUtils.create(CacheTable)
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
            CacheTable.upsert {
                it[queryCol] = maskApiKey(url)
                it[responseCol] = response
                it[timestampCol] = getExpiryTimestamp()
            }
        }
    }

    suspend fun getEntry(url: String): String? {
        return transaction(db = database) {
            CacheTable
                .select(CacheTable.responseCol).where {
                    (CacheTable.queryCol eq maskApiKey(url)) and
                    (CacheTable.timestampCol greater getNowTimestamp())
                }
                .firstOrNull()
                ?.get(CacheTable.responseCol)
        }
    }
}
