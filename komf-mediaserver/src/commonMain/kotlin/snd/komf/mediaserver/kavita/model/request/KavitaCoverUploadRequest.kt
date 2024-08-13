package snd.komf.mediaserver.kavita.model.request

import kotlinx.serialization.Serializable

@Serializable
data class KavitaCoverUploadRequest(
    val url: String,
    val id: Int,
)