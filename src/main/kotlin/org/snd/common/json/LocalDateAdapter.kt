package org.snd.common.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDate


class LocalDateAdapter {
    @FromJson
    fun fromJson(date: String): LocalDate {
        return LocalDate.parse(date)
    }

    @ToJson
    fun toJson(date: LocalDate): String {
        return date.toString()
    }
}
