package snd.komf.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import snd.komf.api.MediaServer

class KomfClientFactory private constructor(private val builder: Builder) {
    private val json = Json(builder.json) {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun configClient() = KomfConfigClient(ktor, json)
    fun metadataClient(mediaServer: MediaServer) = KomfMetadataClient(ktor, mediaServer)
    fun mediaServerClient(mediaServer: MediaServer) = KomfMediaServerClient(ktor, mediaServer)
    fun jobClient() = KomfJobClient(ktor = ktor, json = json)
    fun notificationClient() = KomfNotificationClient(ktor = ktor)


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