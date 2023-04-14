package org.snd.metadata.providers.bangumi.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token.BEGIN_ARRAY
import com.squareup.moshi.JsonReader.Token.END_ARRAY
import com.squareup.moshi.JsonReader.Token.STRING
import com.squareup.moshi.ToJson

class InfoboxAdapter {
    private val nestedValueOptions = JsonReader.Options.of("k", "v")

    @FromJson
    fun fromJson(reader: JsonReader): InfoboxValue {
        var value: InfoboxValue? = null
        while (reader.hasNext()) {
            when (reader.peek()) {
                BEGIN_ARRAY -> value = parseValueArray(reader)
                END_ARRAY -> reader.endArray()
                STRING -> value = InfoboxValue.SingleValue(reader.nextString())
                else -> throw IllegalStateException("Unexpected json token ${reader.peek()}")

            }

        }

        return value ?: throw JsonDataException("Required value 'value' missing at ${reader.path}")
    }

    private fun parseValueArray(reader: JsonReader): InfoboxValue.MultipleValues {
        val values: MutableList<InfoboxNestedValue> = ArrayList()
        reader.beginArray()

        while (reader.hasNext()) {
            values.add(parseNestedValue(reader))
        }
        reader.endArray()

        return InfoboxValue.MultipleValues(values)
    }

    private fun parseNestedValue(reader: JsonReader): InfoboxNestedValue {
        reader.beginObject()
        var key: String? = null
        var value: String? = null
        while (reader.hasNext()) {
            when (reader.selectName(nestedValueOptions)) {
                0 -> key = reader.nextString()
                1 -> value = reader.nextString()
                -1 -> {
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        if (value == null) throw JsonDataException("Required value 'v' missing at ${reader.path}")

        return if (key == null) {
            InfoboxNestedValue.SingleValue(value)
        } else {
            InfoboxNestedValue.PairValue(key, value)
        }

    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") infoValue: InfoboxValue): Any {
        throw UnsupportedOperationException()
    }
}