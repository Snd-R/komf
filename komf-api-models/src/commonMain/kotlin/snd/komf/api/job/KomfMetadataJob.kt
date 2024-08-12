package snd.komf.api.job

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import snd.komf.api.KomfServerSeriesId
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class KomfMetadataJobId(val value: String) {
    override fun toString() = value
}

@Serializable
data class KomfMetadataJob(
    val seriesId: KomfServerSeriesId,
    val id: KomfMetadataJobId,
    val status: KomfMetadataJobStatus,
    val message: String?,

    val startedAt: Instant,
    val finishedAt: Instant?,
)

enum class KomfMetadataJobStatus {
    RUNNING,
    FAILED,
    COMPLETED
}
