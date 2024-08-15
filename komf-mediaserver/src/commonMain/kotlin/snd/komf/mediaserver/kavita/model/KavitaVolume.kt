package snd.komf.mediaserver.kavita.model

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class KavitaVolumeId(val value: Int) {
    override fun toString() = value.toString()
}

@Serializable
data class KavitaVolume(
    val id: KavitaVolumeId,
    val minNumber: Float,
    val maxNumber: Float,
    val name: String,
    val pages: Int,
    val seriesId: KavitaSeriesId,
    val chapters: Collection<KavitaChapter>,
)

