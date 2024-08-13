package snd.komf.app

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import snd.komf.app.config.AppConfig
import snd.komf.app.config.ConfigLoader
import snd.komf.app.config.ConfigWriter
import snd.komf.app.module.MediaServerModule
import snd.komf.app.module.NotificationsModule
import snd.komf.app.module.ProvidersModule
import snd.komf.app.module.ServerModule
import snd.komf.ktor.komfUserAgent
import snd.komf.mediaserver.repository.Database
import snd.komf.mediaserver.repository.DriverFactory
import snd.komf.mediaserver.repository.createDatabase
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.isDirectory

class AppContext(private val configPath: Path? = null) {
    @Volatile
    var appConfig: AppConfig
        private set

    private val ktorBaseClient: HttpClient
    private val mediaServerDatabase: Database
    private val serverModule: ServerModule

    private var providersModule: ProvidersModule
    private var mediaServerModule: MediaServerModule
    private var notificationsModule: NotificationsModule

    private val yaml = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = false,
            strictMode = false
        )
    )
    private val configWriter = ConfigWriter(yaml)
    private val configLoader = ConfigLoader(yaml)
    private val mutex = Mutex()

    init {
        val config = loadConfig()
        setLogLevel(config)
        appConfig = config
        mediaServerDatabase = createDatabase(DriverFactory(Path.of(appConfig.database.file)))

        val httpLogger = KotlinLogging.logger("http.logging")
        val baseOkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor { httpLogger.info { it } }.setLevel(HttpLoggingInterceptor.Level.BASIC))
            .cache(
                Cache(
                    directory = File(System.getProperty("java.io.tmpdir")),
                    maxSize = 50L * 1024L * 1024L // 50 MiB
                )
            )
            .build()
        ktorBaseClient = HttpClient(OkHttp) {
            engine { preconfigured = baseOkHttpClient }
            expectSuccess = true
            install(UserAgent) { agent = komfUserAgent }
        }

        providersModule = ProvidersModule(config.metadataProviders, ktorBaseClient)
        notificationsModule = NotificationsModule(config.notifications, ktorBaseClient)

        mediaServerModule = MediaServerModule(
            config.komga,
            ktorBaseClient,
            mediaServerDatabase,
            notificationsModule.discordWebhookService,
            providersModule.metadataProviders
        )

        serverModule = ServerModule(
            appContext = this,
            komgaMediaServerClient = MutableStateFlow(mediaServerModule.komgaClient),
            komgaMetadataServiceProvider = MutableStateFlow(mediaServerModule.komgaMetadataServiceProvider),
            jobTracker = mediaServerModule.jobTracker,
            jobsRepository = mediaServerModule.jobRepository,
        )

        serverModule.startServer()
    }

    suspend fun updateConfig(newConfig: AppConfig) {
        withContext(Dispatchers.IO) {
            configPath?.let { path -> configWriter.writeConfig(newConfig, path) }
                ?: configWriter.writeConfigToDefaultPath(newConfig)

            refresh()
        }
    }

    suspend fun refresh() {
        mutex.withLock {
            close()
            val config = loadConfig()
            appConfig = config

            providersModule = ProvidersModule(config.metadataProviders, ktorBaseClient)
            notificationsModule = NotificationsModule(config.notifications, ktorBaseClient)
            mediaServerModule = MediaServerModule(
                config.komga,
                ktorBaseClient,
                mediaServerDatabase,
                notificationsModule.discordWebhookService,
                providersModule.metadataProviders
            )
        }
    }

    private fun close() {
        mediaServerModule.close()
    }

    private fun loadConfig(): AppConfig {
        return when {
            configPath == null -> configLoader.default()
            configPath.isDirectory() -> configLoader.loadDirectory(configPath)
            else -> configLoader.loadFile(configPath)
        }
    }

    private fun setLogLevel(config: AppConfig) {
        val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as Logger
        rootLogger.level = Level.valueOf(config.logLevel.uppercase())
    }
}