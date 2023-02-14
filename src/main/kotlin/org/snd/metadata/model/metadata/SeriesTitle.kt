package org.snd.metadata.model.metadata

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SeriesTitle(
    val name: String,
    val type: TitleType?,
    val language: String?,
)

enum class TitleType(val label: String) {
    ROMAJI("Romaji"),
    LOCALIZED("Localized"),
    NATIVE("Native"),
}