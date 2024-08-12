package snd.komf.api.metadata

import kotlinx.serialization.Serializable
import snd.komf.api.job.KomfMetadataJobId

@Serializable
data class KomfMetadataJobResponse(
    val jobId: KomfMetadataJobId
)