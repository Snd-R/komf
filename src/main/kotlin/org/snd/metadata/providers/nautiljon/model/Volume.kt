package org.snd.metadata.providers.nautiljon.model

import java.time.LocalDate

data class Volume(
    val id: VolumeId,
    val number: Int,
    val originalPublisher: String?,
    val frenchPublisher: String?,
    val originalReleaseDate: LocalDate?,
    val frenchReleaseDate: LocalDate?,
    val numberOfPages: Int?,
    val description: String?,
    val score: Double?,
    val imageUrl: String?,
    val chapters: Collection<Chapter>,
    val authorsStory: Collection<String>,
    val authorsArt: Collection<String>,
)

data class Chapter(val name: String?, val number: Int)
