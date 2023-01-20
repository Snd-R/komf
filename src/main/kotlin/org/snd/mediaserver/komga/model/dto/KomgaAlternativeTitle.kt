package org.snd.mediaserver.komga.model.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KomgaAlternativeTitle(
    val label: String,
    val title: String,
)
