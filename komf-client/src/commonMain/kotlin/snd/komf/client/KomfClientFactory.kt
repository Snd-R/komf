package snd.komf.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import snd.komf.api.MediaServer

class KomfClientFactory private constructor(private val builder: Builder) {

    fun configClient() = KomfConfigClient(ktor)
    fun metadataClient(mediaServer: MediaServer) = KomfMetadataClient(ktor, mediaServer)
    fun mediaServerClient(mediaServer: MediaServer) = KomfMediaServerClient(ktor, mediaServer)
    fun jobClient() = KomfJobClient(ktor = ktor, json = json)
    fun notificationClient() = KomfNotificationClient(ktor = ktor)

    private val json = Json(builder.json) {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }
    private val baseUrl: () -> String = builder.baseUrl

    private val ktor: HttpClient = (builder.ktor ?: HttpClient()).config {
        expectSuccess = true
        builder.cookieStorage?.let { install(HttpCookies) { storage = it } }
        defaultRequest { url(baseUrl()) }
        install(ContentNegotiation) { json(json) }
        install(SSE)
    }

    class Builder {
        internal var ktor: HttpClient? = null
        internal var baseUrl: () -> String = { "http://localhost:8085" }
        internal var cookieStorage: CookiesStorage? = AcceptAllCookiesStorage()
        internal var json: Json = Json

        fun ktor(ktor: HttpClient) = apply {
            this.ktor = ktor
        }

        fun baseUrl(block: () -> String) = apply {
            this.baseUrl = block
        }

        fun cookieStorage(cookiesStorage: CookiesStorage?) = apply {
            this.cookieStorage = cookiesStorage
        }

        fun build(): KomfClientFactory {
            return KomfClientFactory(this)
        }
    }
}