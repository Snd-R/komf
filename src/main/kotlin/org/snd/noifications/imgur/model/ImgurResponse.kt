package org.snd.noifications.imgur.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ImgurResponse<T>(
    val data: T,
    val success: Boolean,
    val status: Int
)