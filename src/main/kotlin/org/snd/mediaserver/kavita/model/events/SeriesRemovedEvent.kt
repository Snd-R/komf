package org.snd.mediaserver.kavita.model.events

data class SeriesRemovedEvent(
    val body: Body,
    val name: String?,
    val title: String?,
    val subTitle: String?,
    val eventType: String?,
    val progress: String?,
) {

    data class Body(
        val seriesId: Int,
        val libraryId: Int,
        val seriesName: String,
    )
}