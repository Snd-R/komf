package snd.komf

import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.HttpCookies
import org.jetbrains.exposed.sql.Database
import snd.komf.ktor.komfUserAgent
import snd.komf.providers.MetadataProvidersConfig
import snd.komf.providers.ProvidersModule
import snd.komf.providers.mangabaka.db.MangaBakaDbDownloader
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.notExists

class CoreModule(
    private val config: MetadataProvidersConfig,
    ktor: HttpClient,
) {
    private val baseHttpClient = ktor.config {
        expectSuccess = true
        install(HttpCookies.Companion)
        install(UserAgent) { agent = komfUserAgent }

    }
    val mangaBakaDir = Path(config.mangabakaDatabaseDir)
    val mangaBakaDatabaseDownloader = MangaBakaDbDownloader(baseHttpClient, mangaBakaDir)
    val mangaBakaDatabase = createMangaBakaDatabase(Path.of(config.mangabakaDatabaseDir))

    val metadataProviders = ProvidersModule(config, baseHttpClient, mangaBakaDatabase).getMetadataProviders()

    private fun createMangaBakaDatabase(baseDir: Path): Database? {
        val databaseFile = baseDir.resolve("mangabaka.sqlite")
        if (databaseFile.notExists()) {
            return null
        }
        return Database.connect("jdbc:sqlite:$databaseFile")
    }
}