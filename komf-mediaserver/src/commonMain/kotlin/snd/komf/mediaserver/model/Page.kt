package snd.komf.mediaserver.model

data class Page<T> (
    val content: List<T>,
    val pageNumber: Int,
    val totalPages: Int?,
    val totalElements: Int?,
)