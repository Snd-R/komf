package org.snd.metadata.model.metadata.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.metadata.model.metadata.ProviderBookId

class ProviderBookIdJsonAdapter {
    @FromJson
    fun fromJson(@Suppress("UNUSED_PARAMETER") json: String): ProviderBookId {
        throw UnsupportedOperationException()
    }

    @ToJson
    fun toJson(id: ProviderBookId) = id.id
}
