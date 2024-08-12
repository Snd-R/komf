package snd.komf.mediaserver.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import snd.komf.mediaserver.model.MediaServerSeriesId
import java.util.concurrent.ConcurrentHashMap

class KomfJobTracker(
    private val jobsRepository: KomfJobsRepository
) {
    private val activeJobs = ConcurrentHashMap<MetadataJobId, ActiveJob>()
    private val listenerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
                        jobsRepository.save(activeJob.metadataJob.fail(event.message))
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
            }.launchIn(listenerScope)

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
