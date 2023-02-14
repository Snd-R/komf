package org.snd.common.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.net.URI


class UriAdapter {
    @FromJson
    fun fromJson(uri: String): URI {
        return URI.create(uri)
    }

    @ToJson
    fun toJson(uri: URI): String {
        return uri.toString()
    }
}
