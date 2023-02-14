package org.snd.metadata.model.metadata

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Author(
    val name: String,
    val role: AuthorRole
)
