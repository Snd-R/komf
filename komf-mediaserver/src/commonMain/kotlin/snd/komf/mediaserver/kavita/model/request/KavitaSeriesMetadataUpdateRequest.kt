package snd.komf.mediaserver.kavita.model.request

import kotlinx.serialization.Serializable
import snd.komf.mediaserver.kavita.model.KavitaSeriesMetadata

@Serializable
data class KavitaSeriesMetadataUpdateRequest(
    val seriesMetadata: KavitaSeriesMetadata,
)