package snd.komf

import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.HttpCookies
import org.jetbrains.exposed.v1.jdbc.Database
import snd.komf.ktor.komfUserAgent
import snd.komf.providers.MetadataProvidersConfig
import snd.komf.providers.ProvidersModule
import snd.komf.providers.bookwalker.db.BookWalkerDbDownloader
import snd.komf.providers.mangabaka.db.MangaBakaDbDownloader
import snd.komf.providers.mangabaka.db.MangaBakaDbMetadata
import java.nio.file.Path
import kotlin.io.path.notExists

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
    private val mangaBakaDatabaseFile = mangaBakaDir.resolve("mangabaka.sqlite")
    val mangaBakaDbMetadata = MangaBakaDbMetadata(
        mangaBakaDir.resolve("timestamp"),
        mangaBakaDir.resolve("checksum.sha1")
    )
    val mangaBakaDatabaseDownloader = MangaBakaDbDownloader(
        baseHttpClient,
        databaseArchive = mangaBakaDir.resolve("mangabaka.tar.gz"),
        databaseFile = mangaBakaDatabaseFile,
        dbMetadata = mangaBakaDbMetadata,
        onStateRefresh = onStateRefresh
    )

    val mangaBakaDatabase =
        if (mangaBakaDatabaseFile.notExists()) null
        else Database.connect("jdbc:sqlite:$mangaBakaDatabaseFile")

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


    val metadataProviders = ProvidersModule(
        config,
        baseHttpClient,
        mangaBakaDatabase,
        bookWalkerDatabase,
    ).getMetadataProviders()
}
