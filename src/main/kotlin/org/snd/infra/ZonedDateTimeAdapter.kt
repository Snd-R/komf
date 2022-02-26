package org.snd.infra

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.ZonedDateTime


class ZonedDateTimeAdapter {
    @FromJson
    fun fromJson(dateTime: String): ZonedDateTime {
        return ZonedDateTime.parse(dateTime)
    }

    @ToJson
    fun toJson(dateTime: ZonedDateTime): String {
        return dateTime.toString()
    }
}
