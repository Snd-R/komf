package snd.komf.mediaserver.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import snd.komf.mediaserver.model.MediaServerSeriesId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.days

class KomfJobTracker(
    private val jobsRepository: KomfJobsRepository
) {
    private val activeJobs = ConcurrentHashMap<MetadataJobId, ActiveJob>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        coroutineScope.launch {
            jobsRepository.cancelAllRunning()
            val totalCount = jobsRepository.countAll()
            if (totalCount > 10_000) {
                jobsRepository.deleteAllBeforeDate(Clock.System.now().minus(30.days))
            }
        }
    }

    fun registerMetadataJob(
        seriesId: MediaServerSeriesId,
        flow: SharedFlow<MetadataJobEvent>,
    ): MetadataJobId {
        val job = MetadataJob(seriesId = seriesId)
        jobsRepository.save(job)

        val listenerJob = flow
            .onEach { event ->
                when (event) {
                    is MetadataJobEvent.ProviderErrorEvent -> {
                        val activeJob = requireNotNull(activeJobs.remove(job.id))
                        jobsRepository.save(
                            activeJob.metadataJob.fail("${event.provider}\n${event.message}")
                        )
                        activeJob.flowCompletionListener.cancel()
                    }

                    is MetadataJobEvent.ProcessingErrorEvent -> {
                        val activeJob = requireNotNull(activeJobs.remove(job.id))
                        jobsRepository.save(activeJob.metadataJob.fail(event.message))
                        activeJob.flowCompletionListener.cancel()
                    }

                    MetadataJobEvent.CompletionEvent -> {
                        val activeJob = requireNotNull(activeJobs.remove(job.id))
                        jobsRepository.save(activeJob.metadataJob.complete())
                        activeJob.flowCompletionListener.cancel()
                    }

                    else -> {}
                }
            }.launchIn(coroutineScope)

        activeJobs[job.id] = ActiveJob(job, flow, listenerJob)
        return job.id
    }

    fun getMetadataJobEvents(jobId: MetadataJobId): SharedFlow<MetadataJobEvent>? {
        return activeJobs[jobId]?.eventFlow
    }

    private data class ActiveJob(
        val metadataJob: MetadataJob,
        val eventFlow: SharedFlow<MetadataJobEvent>,
        val flowCompletionListener: Job,
    )
}
