package org.snd.mediaserver.kavita.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.mediaserver.kavita.model.KavitaPublicationStatus.*

class KavitaPublicationStatusAdapter {

    @FromJson
    @PublicationStatus
    fun fromJson(status: Int): KavitaPublicationStatus =
        when (status) {
            0 -> ONGOING
            1 -> HIATUS
            2 -> COMPLETED
            3 -> CANCELLED
            4 -> ENDED
            else -> throw RuntimeException("Unsupported status code $status")
        }

    @ToJson
    fun toJson(@PublicationStatus status: KavitaPublicationStatus): Int = status.id
}
