package org.snd.mediaserver.kavita.model

data class KavitaPage<T>(
    val content: Collection<T>,
    val currentPage: Int,
)
