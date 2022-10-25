package org.snd.mediaserver.komga.model.event

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TaskQueueStatusEvent(
    val count: Int,
    val countByType: Map<String, Int>,
)
