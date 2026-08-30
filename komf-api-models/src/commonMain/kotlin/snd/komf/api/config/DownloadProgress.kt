package snd.komf.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DownloadProgress {
    @Serializable
    @SerialName("ProgressEvent")
    data class ProgressEvent(
        val total: Long,
        val completed: Long,
        val info: String? = null,
    ) : DownloadProgress

    @Serializable
    @SerialName("FinishedEvent")
    data object FinishedEvent : DownloadProgress

    @Serializable
    @SerialName("ErrorEvent")
    data class ErrorEvent(val message: String) : DownloadProgress
}

