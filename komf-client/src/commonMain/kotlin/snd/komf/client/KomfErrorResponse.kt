package snd.komf.client

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.serialization.*
import kotlinx.serialization.SerializationException
import snd.komf.api.KomfErrorResponse

suspend fun ResponseException.toKomfErrorResponse(): KomfErrorResponse? =
    try {
        response.body()
    } catch (e: SerializationException) {
        null
    } catch (e: JsonConvertException) {
        null
    }
