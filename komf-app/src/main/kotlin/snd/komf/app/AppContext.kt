package snd.komf.app

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import snd.komf.app.config.AppConfig
import snd.komf.app.config.ConfigLoader
import snd.komf.app.config.ConfigWriter
import snd.komf.app.module.ApiDynamicDependencies
import snd.komf.app.module.MediaServerModule
import snd.komf.app.module.NotificationsModule
import snd.komf.app.module.ProvidersModule
import snd.komf.app.module.ServerModule
import snd.komf.ktor.komfUserAgent
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.KomfJobsRepository
import snd.komf.mediaserver.repository.Database
import snd.komf.mediaserver.repository.DriverFactory
import snd.komf.mediaserver.repository.createDatabase
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

private val logger = KotlinLogging.logger {}

class AppContext(private val configPath: Path? = null) {
    @Volatile
    var appConfig: AppConfig
        private set

    private val reloadMutex = Mutex()

    private val ktorBaseClient: HttpClient
    private val jsonBase: Json
    private val mediaServerDatabase: Database
    private val serverModule: ServerModule

    private val jobRepository: KomfJobsRepository
    private val jobTracker: KomfJobTracker
    private var providersModule: ProvidersModule
    private var mediaServerModule: MediaServerModule
    private var notificationsModule: NotificationsModule

    private var apiRoutesDependencies: MutableStateFlow<ApiDynamicDependencies>

    private val yaml = Yaml(
        configuration = YamlConfiguration(
            encodeDefaults = false,
            strictMode = false
        )
    )
    private val configWriter = ConfigWriter(yaml)
    private val configLoader = ConfigLoader(yaml)

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
            .addInterceptor(HttpLoggingInterceptor { httpLogger.info { it } }
                .setLevel(appConfig.httpLogLevel))
            .cache(
                Cache(
                    directory = Path.of(System.getProperty("java.io.tmpdir"))
                        .resolve("komf").createDirectories()
                        .toFile(),
                    maxSize = 50L * 1024L * 1024L // 50 MiB
                )
            )
            .build()

        jsonBase = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        ktorBaseClient = HttpClient(OkHttp) {
            engine { preconfigured = baseOkHttpClient }
            expectSuccess = true
            install(UserAgent) { agent = komfUserAgent }
        }

        jobRepository = KomfJobsRepository(mediaServerDatabase.komfJobRecordQueries)
        jobTracker = KomfJobTracker(jobRepository)

        providersModule = ProvidersModule(config.metadataProviders, ktorBaseClient)
        notificationsModule = NotificationsModule(config.notifications, ktorBaseClient)

        mediaServerModule = MediaServerModule(
            komgaConfig = config.komga,
            kavitaConfig = config.kavita,
            jsonBase = jsonBase,
            ktorBaseClient = ktorBaseClient,
            mediaServerDatabase = mediaServerDatabase,
            appriseService = notificationsModule.appriseService,
            discordWebhookService = notificationsModule.discordWebhookService,
            jobTracker = jobTracker,
            metadataProviders = providersModule.metadataProviders
        )
        this.apiRoutesDependencies = MutableStateFlow(createApiRoutesDependencies())

        serverModule = ServerModule(
            serverPort = config.server.port,
            onConfigUpdate = this::refreshState,
            onStateReload = this::refreshState,
            jobTracker = jobTracker,
            jobsRepository = jobRepository,
            dynamicDependencies = apiRoutesDependencies,
        )

        serverModule.startServer()
    }


    suspend fun refreshState() {
        reloadMutex.withLock {
            reloadModules(this.appConfig)
        }
    }

    suspend fun refreshState(newConfig: AppConfig) {
        reloadMutex.withLock {
            reloadModules(newConfig)
            appConfig = newConfig
            writeConfig(newConfig)
        }
    }

    private fun reloadModules(config: AppConfig) {
        logger.info { "Reconfiguring application state" }

        val providersModule = ProvidersModule(config.metadataProviders, ktorBaseClient)
        val notificationsModule = NotificationsModule(config.notifications, ktorBaseClient)
        val mediaServerModule = MediaServerModule(
            komgaConfig = config.komga,
            kavitaConfig = config.kavita,
            jsonBase = jsonBase,
            ktorBaseClient = ktorBaseClient,
            mediaServerDatabase = mediaServerDatabase,
            appriseService = notificationsModule.appriseService,
            discordWebhookService = notificationsModule.discordWebhookService,
            jobTracker = jobTracker,
            metadataProviders = providersModule.metadataProviders
        )

        this.close()

        this.providersModule = providersModule
        this.notificationsModule = notificationsModule
        this.mediaServerModule = mediaServerModule
        apiRoutesDependencies.value = createApiRoutesDependencies()
    }

    private fun createApiRoutesDependencies() = ApiDynamicDependencies(
        config = this.appConfig,
        komgaMediaServerClient = mediaServerModule.komgaClient,
        komgaMetadataServiceProvider = mediaServerModule.komgaMetadataServiceProvider,
        kavitaMediaServerClient = mediaServerModule.kavitaMediaServerClient,
        kavitaMetadataServiceProvider = mediaServerModule.kavitaMetadataServiceProvider,
        discordService = notificationsModule.discordWebhookService,
        discordRenderer = notificationsModule.discordVelocityRenderer,
        appriseService = notificationsModule.appriseService,
        appriseRenderer = notificationsModule.appriseVelocityRenderer,
        mangaBakaDbAvailable = providersModule.mangaBakaDatabase != null,
        mangaBakaDownloader = providersModule.mangaBakaDatabaseDownloader,
    )

    private suspend fun writeConfig(config: AppConfig) {
        withContext(Dispatchers.IO) {
            configPath?.let { path -> configWriter.writeConfig(config, path) }
                ?: configWriter.writeConfigToDefaultPath(config)
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