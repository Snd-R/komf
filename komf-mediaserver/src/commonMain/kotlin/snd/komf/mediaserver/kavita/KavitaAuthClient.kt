package snd.komf.mediaserver.kavita

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class KavitaAuthClient(private val ktor: HttpClient) {

    suspend fun authenticate(apiKey: String): KavitaAuthenticateResponse {
        return ktor.post("api/plugin/authenticate") {
            parameter("apiKey", apiKey)
            parameter("pluginName", "Komf")
        }.body()
    }

}

@Serializable
data class KavitaAuthenticateResponse(
    val username: String,
    val email: String?,
    val token: String,
    val apiKey: String,
)
