package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KavitaVolume(
    val id: Int,
    //TODO make non nullable
    // nullable for backwards compatibility with older versions
    val minNumber: Float?,
    val maxNumber: Float?,
    @Deprecated("replaced with minNumber in new releases")
    val number: Float,
    val name: String,
    val pages: Int,
    val pagesRead: Int,
    val seriesId: Int,
    val chapters: Collection<KavitaChapter>,
    val minHoursToRead: Int,
    val maxHoursToRead: Int,
    val avgHoursToRead: Int
) {
    fun volumeId() = KavitaVolumeId(id)
    fun seriesId() = KavitaSeriesId(seriesId)
}
