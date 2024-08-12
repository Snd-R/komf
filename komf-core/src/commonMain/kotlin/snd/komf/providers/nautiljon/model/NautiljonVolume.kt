package snd.komf.providers.nautiljon.model

import kotlinx.datetime.LocalDate
import kotlin.jvm.JvmInline

@JvmInline
value class NautiljonVolumeId(val value: String)

data class NautiljonVolume(
    val id: NautiljonVolumeId,
    val seriesId: NautiljonSeriesId,
    val number: Int,
    val originalPublisher: String?,
    val frenchPublisher: String?,
    val originalReleaseDate: LocalDate?,
    val frenchReleaseDate: LocalDate?,
    val numberOfPages: Int?,
    val description: String?,
    val score: Double?,
    val imageUrl: String?,
    val chapters: Collection<NautiljonChapter>,
    val authorsStory: Collection<String>,
    val authorsArt: Collection<String>,
)

data class NautiljonChapter(val name: String?, val number: Int)
