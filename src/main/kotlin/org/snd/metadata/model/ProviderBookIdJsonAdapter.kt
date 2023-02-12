package org.snd.metadata.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class ProviderBookIdJsonAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") json: String): ProviderBookId {
        throw UnsupportedOperationException()
    }

    @ToJson
    fun toJson(id: ProviderBookId) = id.id
}
