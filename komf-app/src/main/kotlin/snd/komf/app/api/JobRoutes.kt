package snd.komf.app.api

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.util.*
import io.ktor.sse.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import snd.komf.api.KomfPage
import snd.komf.api.KomfServerSeriesId
import snd.komf.api.job.*
import snd.komf.app.api.mappers.fromProvider
import snd.komf.mediaserver.jobs.*
import snd.komf.mediaserver.jobs.MetadataJobEvent.*
import java.util.*

class JobRoutes(
    private val jobTracker: KomfJobTracker,
    private val jobsRepository: KomfJobsRepository,
    private val json: Json
) {

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

            eventFlow.collect { event ->
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

                    CompletionEvent -> this.close()
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