package snd.komf.model

import kotlinx.serialization.Serializable

const val DOWNLOAD_BUFFER_SIZE = 1024L * 1024L

@Serializable
sealed interface DownloadProgress {
    @Serializable
    data class ProgressEvent(
        val total: Long,
        val completed: Long,
        val info: String? = null,
    ) : DownloadProgress

    @Serializable
    data object FinishedEvent : DownloadProgress

    @Serializable
    data class ErrorEvent(val message: String) : DownloadProgress
}
