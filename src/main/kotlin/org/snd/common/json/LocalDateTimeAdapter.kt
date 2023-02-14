package org.snd.common.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDateTime


class LocalDateTimeAdapter {
    @FromJson
    fun fromJson(dateTime: String): LocalDateTime {
        return LocalDateTime.parse(dateTime)
    }

    @ToJson
    fun toJson(dateTime: LocalDateTime): String {
        return dateTime.toString()
    }
}
