package org.snd.metadata.mylar.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MylarSeries(
    val metadata: MylarMetadata
)