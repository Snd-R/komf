package org.snd.mediaserver.kavita.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class KavitaSeriesDetails(
    val chapters: Collection<KavitaChapter>,
    val storylineChapters: Collection<KavitaChapter>,
    val totalCount: Int,
    val unreadCount: Int,
    val volumes: Collection<KavitaVolume>,
)
