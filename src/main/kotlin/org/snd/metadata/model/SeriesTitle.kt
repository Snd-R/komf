package org.snd.metadata.model

data class SeriesTitle(
    val name: String,
    val type: TitleType?,
)

enum class TitleType {
    ROMAJI,
    LOCALIZED,
    NATIVE,
}