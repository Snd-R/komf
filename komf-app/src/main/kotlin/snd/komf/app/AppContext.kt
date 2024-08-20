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
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
import snd.komf.mediaserver.MediaServerClient
import snd.komf.mediaserver.MetadataServiceProvider
import snd.komf.mediaserver.repository.Database
import snd.komf.mediaserver.repository.DriverFactory
import snd.komf.mediaserver.repository.createDatabase
import snd.komf.notifications.apprise.AppriseCliService
import snd.komf.notifications.apprise.AppriseVelocityTemplates
import snd.komf.notifications.discord.DiscordVelocityTemplates
import snd.komf.notifications.discord.DiscordWebhookService
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

private val logger = KotlinLogging.logger {}

class AppContext(private val configPath: Path? = null) {
    @Volatile
    var appConfig: AppConfig
        private set

    private val ktorBaseClient: HttpClient
    private val jsonBase: Json
    private val mediaServerDatabase: Database
    private val serverModule: ServerModule

    private var providersModule: ProvidersModule
    private var mediaServerModule: MediaServerModule
    private var notificationsModule: NotificationsModule

    private val komgaClient: MutableStateFlow<MediaServerClient>
    private val komgaServiceProvider: MutableStateFlow<MetadataServiceProvider>
    private val kavitaClient: MutableStateFlow<MediaServerClient>
    private val kavitaServiceProvider: MutableStateFlow<MetadataServiceProvider>

    private val discordService: MutableStateFlow<DiscordWebhookService>
    private val discordRenderer: MutableStateFlow<DiscordVelocityTemplates>
    private val appriseService: MutableStateFlow<AppriseCliService>
    private val appriseRenderer: MutableStateFlow<AppriseVelocityTemplates>

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
            metadataProviders = providersModule.metadataProviders
        )
        komgaClient = MutableStateFlow(mediaServerModule.komgaClient)
        komgaServiceProvider = MutableStateFlow(mediaServerModule.komgaMetadataServiceProvider)
        kavitaClient = MutableStateFlow(mediaServerModule.kavitaMediaServerClient)
        kavitaServiceProvider = MutableStateFlow(mediaServerModule.kavitaMetadataServiceProvider)
        discordService = MutableStateFlow(notificationsModule.discordWebhookService)
        discordRenderer = MutableStateFlow(notificationsModule.discordVelocityRenderer)
        appriseService = MutableStateFlow(notificationsModule.appriseService)
        appriseRenderer = MutableStateFlow(notificationsModule.appriseVelocityRenderer)

        serverModule = ServerModule(
            appContext = this,
            jobTracker = mediaServerModule.jobTracker,
            jobsRepository = mediaServerModule.jobRepository,
            komgaMediaServerClient = komgaClient,
            komgaMetadataServiceProvider = komgaServiceProvider,
            kavitaMediaServerClient = kavitaClient,
            kavitaMetadataServiceProvider = kavitaServiceProvider,
            discordService = discordService,
            discordRenderer = discordRenderer,
            appriseService = appriseService,
            appriseRenderer = appriseRenderer
        )

        serverModule.startServer()
    }

    suspend fun refreshState(newConfig: AppConfig) {
        logger.info { "Reconfiguring application state" }

        val providersModule = ProvidersModule(newConfig.metadataProviders, ktorBaseClient)
        val notificationsModule = NotificationsModule(newConfig.notifications, ktorBaseClient)
        val mediaServerModule = MediaServerModule(
            komgaConfig = newConfig.komga,
            kavitaConfig = newConfig.kavita,
            jsonBase = jsonBase,
            ktorBaseClient = ktorBaseClient,
            mediaServerDatabase = mediaServerDatabase,
            appriseService = notificationsModule.appriseService,
            discordWebhookService = notificationsModule.discordWebhookService,
            metadataProviders = providersModule.metadataProviders
        )

        close()
        appConfig = newConfig

        this.providersModule = providersModule
        this.notificationsModule = notificationsModule
        this.mediaServerModule = mediaServerModule

        komgaClient.value = mediaServerModule.komgaClient
        komgaServiceProvider.value = mediaServerModule.komgaMetadataServiceProvider
        kavitaClient.value = mediaServerModule.kavitaMediaServerClient
        kavitaServiceProvider.value = mediaServerModule.kavitaMetadataServiceProvider
        discordService.value = notificationsModule.discordWebhookService
        discordRenderer.value = notificationsModule.discordVelocityRenderer
        appriseService.value = notificationsModule.appriseService
        appriseRenderer.value = notificationsModule.appriseVelocityRenderer

        withContext(Dispatchers.IO) {
            configPath?.let { path -> configWriter.writeConfig(newConfig, path) }
                ?: configWriter.writeConfigToDefaultPath(newConfig)
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