package snd.komf.mediaserver.kavita.model.events

data class CoverUpdateEvent(
    val body: Map<String, *>?,
    val name: String?,
    val title: String?,
    val subTitle: String?,
    val eventType: String?,
    val progress: String?,
)
