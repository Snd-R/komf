package snd.komf.mediaserver.jobs

import snd.komf.mediaserver.model.MediaServerSeriesId
import snd.komf.providers.CoreProviders
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

@JvmInline
value class MetadataJobId(val value: UUID)

data class MetadataJob(
    val seriesId: MediaServerSeriesId,
    val id: MetadataJobId = MetadataJobId(UUID.randomUUID()),
    val status: MetadataJobStatus = MetadataJobStatus.RUNNING,
    val message: String? = null,

    val startedAt: Instant = Clock.System.now(),
    val finishedAt: Instant? = null,
) {

    fun complete(): MetadataJob {
        return copy(
            status = MetadataJobStatus.COMPLETED,
            finishedAt = Clock.System.now()
        )
    }

    fun fail(message: String): MetadataJob {
        return copy(
            status = MetadataJobStatus.FAILED,
            message = message,
            finishedAt = Clock.System.now()
        )
    }
}

enum class MetadataJobStatus {
    RUNNING,
    FAILED,
    COMPLETED
}

sealed interface MetadataJobEvent {

    data class ProviderSeriesEvent(
        val provider: CoreProviders,
    ) : MetadataJobEvent

    data class ProviderBookEvent(
        val provider: CoreProviders,
        val totalBooks: Int,
        val bookProgress: Int,
    ) : MetadataJobEvent

    data class ProviderErrorEvent(
        val provider: CoreProviders,
        val message: String
    ) : MetadataJobEvent

    data class ProviderCompletedEvent(
        val provider: CoreProviders,
    ) : MetadataJobEvent

    data object PostProcessingStartEvent : MetadataJobEvent

    data class ProcessingErrorEvent(val message: String) : MetadataJobEvent
    data object CompletionEvent : MetadataJobEvent
}