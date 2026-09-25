package snd.komf

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import snd.komf.ktor.HttpRequestRateLimiter
import snd.komf.ktor.komfUserAgent
import snd.komf.mangabaka.external.MangaBakaApiClient
import snd.komf.mangabaka.external.MangaBakaDbDownloader
import snd.komf.mangabaka.repository.MangaBakaRepository
import snd.komf.providers.MetadataProvidersConfig
import snd.komf.providers.ProvidersModule
import snd.komf.providers.bookwalker.db.BookWalkerDbDownloader
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.time.Duration.Companion.seconds

class CoreModule(
    private val config: MetadataProvidersConfig,
    workDir: Path,
    ktor: HttpClient,
    onStateRefresh: suspend () -> Unit,
) {
    private val baseHttpClient = ktor.config {
        expectSuccess = true
        install(HttpCookies.Companion)
        install(UserAgent) { agent = komfUserAgent }

    }

    private val mangaBakaDir = workDir.resolve("mangabaka")
    private val mangaBakaDatabaseFile = mangaBakaDir.resolve("mangabaka2.sqlite")
    val mangaBakaApiClient = MangaBakaApiClient(
        ktor.config {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpRequestRateLimiter) {
                interval = 1.seconds
                eventsPerInterval = 1
                allowBurst = false
            }
            install(HttpRequestRetry) {
                retryIf(3) { _, response ->
                    when (response.status.value) {
                        HttpStatusCode.TooManyRequests.value -> true
                        in 500..599 -> true
                        else -> false
                    }
                }
                exponentialDelay(baseDelayMs = 2000, respectRetryAfterHeader = true)
            }
        }
    )

    val mangaBakaDatabase = createMangaBakaDatabase(mangaBakaDatabaseFile)
    val mangaBakaRepository = MangaBakaRepository(mangaBakaDatabase)

    val mangaBakaDatabaseDownloader = MangaBakaDbDownloader(
        ktor = baseHttpClient,
        mangaBakaApi = mangaBakaApiClient,
        workDir = mangaBakaDir,
        mangaBakaDatabase = mangaBakaDatabase,
        onStateRefresh = onStateRefresh,
    )


    private val bookWalkerDir = workDir.resolve("bookwalker")
    private val bookWalkerDatabaseFile = bookWalkerDir.resolve("bkwk-db.sqlite")
    val bookWalkerDbDownloader = BookWalkerDbDownloader(
        baseHttpClient,
        databaseWorkDirectory = bookWalkerDir,
        databaseFile = bookWalkerDatabaseFile,
        onStateRefresh = onStateRefresh
    )
    val bookWalkerDatabase =
        if (bookWalkerDatabaseFile.notExists()) null
        else Database.connect("jdbc:sqlite:$bookWalkerDatabaseFile")


    val providersModule = ProvidersModule(
        config = config,
        baseHttpClient = baseHttpClient,
        mangaBakaApiClient = mangaBakaApiClient,
        mangaBakaRepository = mangaBakaRepository,
        bookWalkerDatabase = bookWalkerDatabase,
    )
    val metadataProviders = providersModule.getMetadataProviders()

    private fun createMangaBakaDatabase(file: Path): Database {
        val config = SQLiteConfig().apply {
            enforceForeignKeys(true)
            setJournalMode(SQLiteConfig.JournalMode.DELETE)
            busyTimeout = 5_000
        }
        val datasource = HikariDataSource(
            HikariConfig().apply {
                dataSource = SQLiteDataSource(config).apply { url = "jdbc:sqlite:${file}" }
                poolName = "app db pool"
                maximumPoolSize = 1
            }
        )
        // old db files TODO remove later
        mangaBakaDir.resolve("mangabaka.sqlite").deleteIfExists()
        mangaBakaDir.resolve("checksum.sha1").deleteIfExists()
        mangaBakaDir.resolve("timestamp").deleteIfExists()
        Flyway(
            Flyway.configure(CoreModule::class.java.classLoader)
                .loggers("slf4j")
                .dataSource(datasource)
                .locations("db/mangabaka")
                .baselineOnMigrate(true)
        ).migrate()

        return Database.connect(
            datasource = datasource,
        )
    }
}
