package org.snd.metadata.nautiljon.model

import java.time.Year

data class Series(
    val id: SeriesId,
    val title: String,
    val alternativeTitles: Collection<String>,
    val originalTitles: Collection<String>,
    val description: String?,
    val imageUrl: String?,
    val country: String?,
    val type: String?,
    val startYear: Year?,
    val status: String?,
    val numberOfVolumes: Int?,
    val genres: Collection<String>,
    val themes: Collection<String>,
    val authorsStory: Collection<String>,
    val authorsArt: Collection<String>,
    val originalPublisher: String?,
    val frenchPublisher: String?,
    val score: Double?,

    val volumeIds: Collection<VolumeId>
)
