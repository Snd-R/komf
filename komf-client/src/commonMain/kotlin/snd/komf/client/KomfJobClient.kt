package snd.komf.client

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import snd.komf.api.KomfPage
import snd.komf.api.job.KomfMetadataJob
import snd.komf.api.job.KomfMetadataJobEvent
import snd.komf.api.job.KomfMetadataJobEvent.ProcessingErrorEvent
import snd.komf.api.job.KomfMetadataJobEvent.ProviderBookEvent
import snd.komf.api.job.KomfMetadataJobEvent.ProviderCompletedEvent
import snd.komf.api.job.KomfMetadataJobEvent.ProviderErrorEvent
import snd.komf.api.job.KomfMetadataJobEvent.ProviderSeriesEvent
import snd.komf.api.job.KomfMetadataJobEvent.UnknownEvent
import snd.komf.api.job.KomfMetadataJobId
import snd.komf.api.job.KomfMetadataJobStatus
import snd.komf.api.job.eventsStreamNotFoundName
import snd.komf.api.job.postProcessingStartName
import snd.komf.api.job.processingErrorEvent
import snd.komf.api.job.providerBookEventName
import snd.komf.api.job.providerCompletedEventName
import snd.komf.api.job.providerErrorEventName
import snd.komf.api.job.providerSeriesEventName

class KomfJobClient(
    private val ktor: HttpClient,
    private val ktorSSE: HttpClient,
    private val json: Json
) {

    suspend fun getJob(jobId: KomfMetadataJobId): KomfMetadataJob {
        return ktor.get("/api/jobs/$jobId").body()
    }

    suspend fun getJobs(
        status: KomfMetadataJobStatus? = null,
        page: Int? = null,
        pageSize: Int? = null,
    ): KomfPage<List<KomfMetadataJob>> {
        return ktor.get("/api/jobs") {
            status?.let { parameter("status", status.name) }
            page?.let { parameter("page", page) }
            pageSize?.let { parameter("pageSize", pageSize) }
        }.body()
    }

    suspend fun getJobEvents(jobId: KomfMetadataJobId): Flow<KomfMetadataJobEvent> {
        return ktorSSE.sseSession("/api/jobs/${jobId.value}/events").incoming
            .map { json.toKomfEvent(it.event, it.data) }
    }

    suspend fun deleteAll() {
        ktor.delete("/api/jobs/all")
    }


    private fun Json.toKomfEvent(event: String?, data: String?): KomfMetadataJobEvent {
        if (data == null) return UnknownEvent

        return when (event) {
            providerSeriesEventName -> decodeFromString<ProviderSeriesEvent>(data)
            providerBookEventName -> decodeFromString<ProviderBookEvent>(data)
            providerErrorEventName -> decodeFromString<ProviderErrorEvent>(data)
            providerCompletedEventName -> decodeFromString<ProviderCompletedEvent>(data)
            postProcessingStartName -> KomfMetadataJobEvent.PostProcessingStartEvent
            processingErrorEvent -> decodeFromString<ProcessingErrorEvent>(data)
            eventsStreamNotFoundName -> KomfMetadataJobEvent.NotFound
            else -> UnknownEvent
        }
    }
}