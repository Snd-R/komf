package snd.komf.mediaserver.kavita.model.request

import kotlinx.serialization.Serializable

@Serializable
data class KavitaCoverUploadRequest(
    val id: Int,
    val url: String,
    val lockCover: Boolean,
)