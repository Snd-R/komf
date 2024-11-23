package snd.komf.mediaserver.kavita.model

import kotlinx.serialization.Serializable


@Serializable
data class KavitaAuthor(
    val id: Int,
    val name: String,
)
