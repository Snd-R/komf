package snd.komf.api.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface MangaBakaDownloadProgress {
    @Serializable
    @SerialName("ProgressEvent")
    data class ProgressEvent(
        val total: Long,
        val completed: Long,
        val info: String? = null,
    ) : MangaBakaDownloadProgress

    @Serializable
    @SerialName("FinishedEvent")
    data object FinishedEvent : MangaBakaDownloadProgress

    @Serializable
    @SerialName("ErrorEvent")
    data class ErrorEvent(val message: String) : MangaBakaDownloadProgress
}

