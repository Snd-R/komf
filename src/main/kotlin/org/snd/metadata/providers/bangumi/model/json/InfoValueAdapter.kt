package org.snd.metadata.providers.bangumi.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapter
import org.snd.metadata.providers.bangumi.model.InfoValue

class InfoValueAdapter {
    @ToJson
    fun toJson(infoValue: InfoValue): String {
        return infoValue.rawString
    }

    @FromJson
    fun fromJson(json: JsonReader): InfoValue {
        return when (json.peek()) {
            JsonReader.Token.BEGIN_ARRAY -> {
                val source = json.nextSource()
                val moshi = Moshi.Builder().build()
                val jsonAdapter = moshi.adapter<List<Map<String, String>>>()
                val list = jsonAdapter.fromJson(source)
                InfoValue(source.toString(), list)
            }
            else -> {
                val str = json.nextString()
                InfoValue(str, null)
            }
        }
    }

}