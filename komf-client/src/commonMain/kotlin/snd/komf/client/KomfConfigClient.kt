package snd.komf.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import snd.komf.api.config.KomfConfig
import snd.komf.api.config.KomfConfigUpdateRequest

class KomfConfigClient(private val ktor: HttpClient) {

    suspend fun getConfig(): KomfConfig {
        return ktor.get("/api/config").body()
    }

    suspend fun updateConfig(request: KomfConfigUpdateRequest) {
        ktor.patch("/api/config") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

}