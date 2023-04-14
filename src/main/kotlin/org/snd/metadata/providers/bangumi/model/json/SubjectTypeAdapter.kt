package org.snd.metadata.providers.bangumi.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.metadata.providers.bangumi.model.SubjectType

class SubjectTypeAdapter {
    @ToJson
    fun toJson(subjectType: SubjectType): Int = subjectType.value

    @FromJson
    fun fromJson(value: String): SubjectType = SubjectType.fromValue(value.toInt())
}