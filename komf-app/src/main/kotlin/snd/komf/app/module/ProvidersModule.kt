package snd.komf.app.module

import io.ktor.client.HttpClient
import org.jetbrains.exposed.sql.Database
import snd.komf.providers.MetadataProvidersConfig
import snd.komf.providers.ProviderFactory
import snd.komf.providers.mangabaka.local.MangaBakaDbDownloader
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.notExists

class ProvidersModule(
    providersConfig: MetadataProvidersConfig,
    ktorBaseClient: HttpClient
) {
    private val providerFactory = ProviderFactory(ktorBaseClient)

    val mangaBakaDir = Path(providersConfig.mangabakaDatabaseDir)
    val mangaBakaDatabaseDownloader = MangaBakaDbDownloader(ktorBaseClient, mangaBakaDir)
    val mangaBakaDatabase = createMangaBakaDatabase(Path.of(providersConfig.mangabakaDatabaseDir))
    val metadataProviders = providerFactory.getMetadataProviders(providersConfig, mangaBakaDatabase)

    private fun createMangaBakaDatabase(baseDir: Path): Database? {
        val databaseFile = baseDir.resolve("mangabaka.sqlite")
        if (databaseFile.notExists()) {
            return null
        }
        return Database.connect("jdbc:sqlite:$databaseFile")
    }
}
