package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KavitaCoverUploadRequest(
    val url: String,
    val id: Int,
)