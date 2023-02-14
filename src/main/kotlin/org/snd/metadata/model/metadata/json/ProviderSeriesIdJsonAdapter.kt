package org.snd.metadata.model.metadata.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.metadata.model.metadata.ProviderSeriesId

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
