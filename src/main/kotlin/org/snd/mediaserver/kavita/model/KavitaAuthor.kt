package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KavitaAuthor(
    val id: Int,
    val name: String,
    @PersonRole
    val role: KavitaPersonRole
)