package org.snd.module

import mu.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import org.snd.config.KomgaConfig
import org.snd.infra.BasicAuthInterceptor
import org.snd.infra.HttpClient
import org.snd.komga.KomgaClient
import org.snd.komga.KomgaEventListener
import org.snd.komga.KomgaService
import java.util.concurrent.TimeUnit.SECONDS

class KomgaModule(
    config: KomgaConfig,
    jsonModule: JsonModule,
    metadataModule: MetadataModule
) : AutoCloseable {
    private val httpClient = OkHttpClient.Builder().build()

    private val komgaHttpClient = httpClient
        .newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(config.komgaUser, config.komgaPassword))
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(BASIC))
        .build()

    private val komgaSseClient = httpClient.newBuilder()
        .readTimeout(0, SECONDS)
        .addInterceptor(BasicAuthInterceptor(config.komgaUser, config.komgaPassword))
        .addInterceptor(HttpLoggingInterceptor { message ->
            KotlinLogging.logger {}.debug { message }
        }.setLevel(BASIC))
        .build()

    private val komgaClient = KomgaClient(
        client = HttpClient(
            client = komgaHttpClient,
            name = "Komga"
        ),
        moshi = jsonModule.moshi,
        baseUrl = config.baseUri.toHttpUrl()
    )

    val komgaService = KomgaService(
        komgaClient = komgaClient,
        metadataProviders = metadataModule.metadataProviders
    )

    private val komgaEventListener = KomgaEventListener(
        client = komgaSseClient,
        moshi = jsonModule.moshi,
        komgaUrl = config.baseUri.toHttpUrl(),
        komgaService = komgaService,
        libraryFilter = {
            if (config.eventListener.libraries.isEmpty()) true
            else config.eventListener.libraries.contains(it)
        }
    )

    init {
        if (config.eventListener.enabled)
            komgaEventListener.start()
    }

    override fun close() {
        komgaEventListener.stop()
    }
}
