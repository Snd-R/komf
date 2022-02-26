package org.snd.infra

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.LocalDate


class LocalDateAdapter {
    @FromJson
    fun dateFromJson(date: String): LocalDate {
        return LocalDate.parse(date)
    }

    @ToJson
    fun eventToJson(date: LocalDate): String {
        return date.toString()
    }
}
