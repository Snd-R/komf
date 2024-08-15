package snd.komf.app.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.util.*
import io.ktor.sse.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.takeWhile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import snd.komf.api.KomfPage
import snd.komf.api.KomfServerSeriesId
import snd.komf.api.job.KomfMetadataJob
import snd.komf.api.job.KomfMetadataJobEvent
import snd.komf.api.job.KomfMetadataJobId
import snd.komf.api.job.KomfMetadataJobStatus
import snd.komf.api.job.eventsStreamNotFoundName
import snd.komf.api.job.postProcessingStartName
import snd.komf.api.job.processingErrorEvent
import snd.komf.api.job.providerBookEventName
import snd.komf.api.job.providerCompletedEventName
import snd.komf.api.job.providerErrorEventName
import snd.komf.api.job.providerSeriesEventName
import snd.komf.app.api.mappers.fromProvider
import snd.komf.mediaserver.jobs.KomfJobTracker
import snd.komf.mediaserver.jobs.KomfJobsRepository
import snd.komf.mediaserver.jobs.MetadataJob
import snd.komf.mediaserver.jobs.MetadataJobEvent.CompletionEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.PostProcessingStartEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProcessingErrorEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderBookEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderCompletedEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderErrorEvent
import snd.komf.mediaserver.jobs.MetadataJobEvent.ProviderSeriesEvent
import snd.komf.mediaserver.jobs.MetadataJobId
import snd.komf.mediaserver.jobs.MetadataJobStatus
import java.util.*

class JobRoutes(
    private val jobTracker: KomfJobTracker,
    private val jobsRepository: KomfJobsRepository,
    private val json: Json
) {
    private val sseScope = CoroutineScope(Dispatchers.Default)

    fun registerRoutes(routing: Route) {
        routing.route("/jobs") {
            getJobsRoute()
            getJobRoute()
            metadataEventFlowRoute()
            deleteAllRoute()
        }
    }

    private fun Route.metadataEventFlowRoute() {
        sse("/{jobId}/events") {
            val jobId = UUID.fromString(call.parameters.getOrFail("jobId"))

            val eventFlow = jobTracker.getMetadataJobEvents(MetadataJobId(jobId))
            if (eventFlow == null) {
                ServerSentEvent(null, eventsStreamNotFoundName)
                return@sse
            }

            eventFlow
                .takeWhile { it !is CompletionEvent }
                .collect { event ->
                    currentCoroutineContext()
                    when (event) {
                        is ProviderSeriesEvent -> send(
                            ServerSentEvent(json.encodeToString(event.toDto()), providerSeriesEventName)
                        )

                        is ProviderBookEvent -> send(
                            ServerSentEvent(json.encodeToString(event.toDto()), providerBookEventName)
                        )

                        is ProviderCompletedEvent -> send(
                            ServerSentEvent(json.encodeToString(event.toDto()), providerCompletedEventName)
                        )

                        is ProviderErrorEvent -> {
                            send(ServerSentEvent(json.encodeToString(event.toDto()), providerErrorEventName))
                        }

                        PostProcessingStartEvent -> send(
                            ServerSentEvent(
                                json.encodeToString(KomfMetadataJobEvent.PostProcessingStartEvent),
                                postProcessingStartName
                            )
                        )

                        is ProcessingErrorEvent -> send(
                            ServerSentEvent(json.encodeToString(event.toDto()), processingErrorEvent)
                        )

                        CompletionEvent -> this.cancel()
                    }
                }
        }
    }

    private fun Route.getJobRoute() {
        get("/{jobId}") {
            val jobId = UUID.fromString(call.parameters.getOrFail("jobId"))
            val job = jobsRepository.get(MetadataJobId(jobId))
                ?: return@get call.response.status(HttpStatusCode.NotFound)

            call.respond(job.toDto())
        }
    }

    private fun Route.deleteAllRoute() {
        delete("/all") {
            jobsRepository.deleteAll()
            call.respond(HttpStatusCode.NoContent, "")
        }
    }

    private fun Route.getJobsRoute() {
        get("") {
            val status = runCatching { call.queryParameters["status"]?.let { MetadataJobStatus.valueOf(it) } }
                .getOrElse { return@get call.respond(HttpStatusCode.BadRequest, "") }

            val limit = runCatching { call.queryParameters["pageSize"]?.toLong() }
                .getOrElse { return@get call.respond(HttpStatusCode.BadRequest, "") }
                ?: 1000L

            val page = runCatching { call.queryParameters["page"]?.toLong() }
                .getOrElse { return@get call.respond(HttpStatusCode.BadRequest, "") }
                ?: 0

            val offset = (page - 1) * limit
            val count = jobsRepository.countAll(status)
            val jobs = jobsRepository.findAll(status, limit, offset).map { it.toDto() }

            call.respond(
                KomfPage(
                    content = jobs,
                    totalPages = (count / limit).toInt(),
                    currentPage = page.toInt()
                )
            )
        }
    }
}

private fun ProviderBookEvent.toDto() = KomfMetadataJobEvent.ProviderBookEvent(
    provider = provider.fromProvider(),
    totalBooks = totalBooks,
    bookProgress = bookProgress
)

private fun ProviderSeriesEvent.toDto() = KomfMetadataJobEvent.ProviderSeriesEvent(
    provider = provider.fromProvider(),
)

private fun ProviderErrorEvent.toDto() = KomfMetadataJobEvent.ProviderErrorEvent(
    provider = provider.fromProvider(),
    message = message
)

private fun ProviderCompletedEvent.toDto() = KomfMetadataJobEvent.ProviderCompletedEvent(
    provider = provider.fromProvider(),
)

private fun PostProcessingStartEvent.toDto() = KomfMetadataJobEvent.PostProcessingStartEvent

private fun ProcessingErrorEvent.toDto() = KomfMetadataJobEvent.ProcessingErrorEvent(message)


private fun MetadataJob.toDto() = KomfMetadataJob(
    seriesId = KomfServerSeriesId(seriesId.value),
    id = KomfMetadataJobId(id.value.toString()),
    status = when (status) {
        MetadataJobStatus.RUNNING -> KomfMetadataJobStatus.RUNNING
        MetadataJobStatus.FAILED -> KomfMetadataJobStatus.FAILED
        MetadataJobStatus.COMPLETED -> KomfMetadataJobStatus.COMPLETED
    },
    message = message,
    startedAt = startedAt,
    finishedAt = finishedAt
)