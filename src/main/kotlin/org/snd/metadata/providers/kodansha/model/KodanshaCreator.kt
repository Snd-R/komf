package org.snd.metadata.providers.kodansha.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KodanshaCreator(
    val name: String,
)
