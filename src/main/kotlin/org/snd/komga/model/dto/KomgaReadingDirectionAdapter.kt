package org.snd.komga.model.dto

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class KomgaReadingDirectionAdapter {

    @FromJson
    @ReadingDirection
    fun fromJson(readingDirection: String): String? = readingDirection.ifBlank { null }

    @ToJson
    fun toJson(@ReadingDirection readingDirection: String?): String? = readingDirection
}
