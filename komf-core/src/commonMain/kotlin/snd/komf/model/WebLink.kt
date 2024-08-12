package snd.komf.model

import kotlinx.serialization.Serializable

@Serializable
data class WebLink(
    val label: String,
    val url: String,
)
