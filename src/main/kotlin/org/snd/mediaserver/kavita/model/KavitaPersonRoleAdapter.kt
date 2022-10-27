package org.snd.mediaserver.kavita.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.mediaserver.kavita.model.KavitaPersonRole.*

class KavitaPersonRoleAdapter {

    @FromJson
    @PersonRole
    fun fromJson(status: Int): KavitaPersonRole =
        when (status) {
            1 -> OTHER
            3 -> WRITER
            4 -> PENCILLER
            5 -> INKER
            6 -> COLORIST
            7 -> LETTERER
            8 -> COVER_ARTIST
            9 -> EDITOR
            10 -> PUBLISHER
            11 -> CHARACTER
            12 -> TRANSLATOR
            else -> throw RuntimeException("Unsupported status code $status")
        }

    @ToJson
    fun toJson(@PersonRole status: KavitaPersonRole): Int = status.id
}
