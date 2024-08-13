package snd.komf.mediaserver.kavita.model.events

import kotlinx.serialization.json.Json
import snd.komga.client.sse.KomgaEvent

sealed interface KavitaEvent {

    data class UnknownEvent(val event: String?, val data: String?):KavitaEvent
}

fun Json.toKavitaEvent(event: String?, data: String?): KavitaEvent {
    return when (event) {
        else -> KavitaEvent.UnknownEvent(event, data)
    }
}
