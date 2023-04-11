package org.snd.metadata.providers.bangumi.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Tag (

    val name: String,

    val count: Int

)
