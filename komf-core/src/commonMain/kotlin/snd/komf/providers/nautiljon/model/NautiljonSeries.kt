package snd.komf.providers.nautiljon.model

import kotlin.jvm.JvmInline


@JvmInline
value class NautiljonSeriesId(val value: String)

data class NautiljonSeries(
    val id: NautiljonSeriesId,
    val title: String,
    val alternativeTitles: Collection<String>,
    val romajiTitle: String?,
    val japaneseTitle: String?,
    val description: String?,
    val imageUrl: String?,
    val country: String?,
    val type: String?,
    val startYear: Int?,
    val status: String?,
    val numberOfVolumes: Int?,
    val genres: Collection<String>,
    val themes: Collection<String>,
    val authorsStory: Collection<String>,
    val authorsArt: Collection<String>,
    val originalPublisher: String?,
    val frenchPublisher: String?,
    val recommendedAge: Int?,
    val score: Double?,

    val volumes: Collection<NautiljonSeriesVolume>
)

data class NautiljonSeriesVolume(
    val id: NautiljonVolumeId,
    val number: Int?,
    val edition: String?,
    val type: String?,
    val name: String?
)
