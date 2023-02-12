package org.snd.metadata.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class ProviderSeriesIdJsonAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") json: String): ProviderSeriesId {
        throw UnsupportedOperationException()
    }

    @ToJson
    fun toJson(id: ProviderSeriesId): String {
        return id.id
    }
}
