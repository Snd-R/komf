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
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readLine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import snd.komf.api.config.DownloadProgress
import snd.komf.api.config.DownloadProgress.ErrorEvent
import snd.komf.api.config.DownloadProgress.FinishedEvent
import snd.komf.api.config.KomfConfig
import snd.komf.api.config.KomfConfigUpdateRequest

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

    fun updateMangaBakaDb(): Flow<DownloadProgress> {
        return flow {
            runCatching {
                ktor.preparePost("/api/update-manga-baka-db").execute { response ->
                    streamProgressEvents(response.bodyAsChannel())
                }
            }.onFailure {
                emit(ErrorEvent(it.message ?: "Unexpected error"))
            }
        }
    }

    fun updateBookWalkerDb(): Flow<DownloadProgress> {
        return flow {
            runCatching {
                ktor.preparePost("/api/update-book-walker-db").execute { response ->
                    streamProgressEvents(response.bodyAsChannel())
                }
            }.onFailure {
                emit(ErrorEvent(it.message ?: "Unexpected error"))
            }
        }
    }

    private suspend fun FlowCollector<DownloadProgress>.streamProgressEvents(channel: ByteReadChannel) {
        while (!channel.isClosedForRead) {
            val message = channel.readLine()
            if (message == null) {
                emit(ErrorEvent("Connection closed"))
                break
            }

            val event = json.decodeFromString<DownloadProgress>(message)
            emit(event)
            if (event is FinishedEvent || event is ErrorEvent) {
                break
            }
        }
    }
}
