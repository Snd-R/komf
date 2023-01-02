package org.snd.noifications.imgur.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImgurImage(
    val id: String,
    val link: String,

    val title: String?,
    val description: String?,
    val datetime: Long,
    val type: String,
    val animated: Boolean,
    val width: Int,
    val height: Int,
    val size: Long,
    val deletehash: String?,
    val name: String?,
)