package snd.komf.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import snd.komf.api.config.KomfConfig
import snd.komf.api.config.KomfConfigUpdateRequest
import snd.komf.api.config.MangaBakaDownloadProgress
import snd.komf.api.config.MangaBakaDownloadProgress.ErrorEvent
import snd.komf.api.config.MangaBakaDownloadProgress.FinishedEvent

class KomfConfigClient(
    private val ktor: HttpClient,
    private val json: Json
) {

    suspend fun getConfig(): KomfConfig {
        return ktor.get("/api/config").body()
    }

    suspend fun updateConfig(request: KomfConfigUpdateRequest) {
        ktor.patch("/api/config") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    fun updateMangaBakaDb(): Flow<MangaBakaDownloadProgress> {
        return flow {
            runCatching {
                ktor.preparePost("/api/update-manga-baka-db").execute { response ->
                    val channel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val message = channel.readUTF8Line()
                        if (message == null) {
                            emit(ErrorEvent("Connection closed"))
                            break
                        }

                        val event = json.decodeFromString<MangaBakaDownloadProgress>(message)
                        emit(event)
                        if (event is FinishedEvent || event is ErrorEvent) {
                            break
                        }
                    }
                }
            }.onFailure {
                emit(ErrorEvent(it.message ?: "Unexpected error"))
            }
        }
    }
}
