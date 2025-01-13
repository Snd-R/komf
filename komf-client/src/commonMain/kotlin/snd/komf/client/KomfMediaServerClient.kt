package snd.komf.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import snd.komf.api.MediaServer
import snd.komf.api.mediaserver.KomfMediaServerConnectionResponse
import snd.komf.api.mediaserver.KomfMediaServerLibrary

class KomfMediaServerClient(
    private val ktor: HttpClient,
    mediaServer: MediaServer
) {
    private val mediaServerApiPrefix = "/api/${mediaServer.name.lowercase()}/media-server"

    suspend fun checkConnection(): KomfMediaServerConnectionResponse {
        return ktor.get("$mediaServerApiPrefix/connected").body()
    }

    suspend fun getLibraries(): List<KomfMediaServerLibrary> {
        return ktor.get("$mediaServerApiPrefix/libraries").body()
    }
}
