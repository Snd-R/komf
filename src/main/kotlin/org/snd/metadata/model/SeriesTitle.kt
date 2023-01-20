package org.snd.metadata.model

data class SeriesTitle(
    val name: String,
    val type: TitleType?,
)

enum class TitleType(val label: String) {
    ROMAJI("Romaji"),
    LOCALIZED("Localized"),
    NATIVE("Native"),
}