package org.snd.metadata.providers.bangumi.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.metadata.providers.bangumi.model.PersonType

class PersonTypeAdapter {
    @ToJson
    fun toJson(personType: PersonType): Int = personType.value

    @FromJson
    fun fromJson(value: String): PersonType = PersonType.fromValue(value.toInt())
}