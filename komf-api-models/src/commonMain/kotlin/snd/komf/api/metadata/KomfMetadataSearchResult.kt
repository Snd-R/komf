package snd.komf.api.metadata

import kotlinx.serialization.Serializable
import snd.komf.api.KomfProviderSeriesId
import snd.komf.api.KomfProviders

@Serializable
data class KomfMetadataSeriesSearchResult(
    val url: String?,
    val imageUrl: String? = null,
    val title: String,
    val provider: KomfProviders,
    val resultId: KomfProviderSeriesId,
)
