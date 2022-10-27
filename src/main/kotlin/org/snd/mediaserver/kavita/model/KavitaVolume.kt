package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KavitaVolume(
    val id: Int,
    val number: Int,
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
