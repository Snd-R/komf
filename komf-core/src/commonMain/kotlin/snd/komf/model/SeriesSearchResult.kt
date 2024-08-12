package snd.komf.model

import kotlinx.serialization.Serializable
import snd.komf.providers.CoreProviders

@Serializable
data class SeriesSearchResult(
    val url: String?,
    val imageUrl: String? = null,
    val title: String,
    val provider: CoreProviders,
    val resultId: String,
)
