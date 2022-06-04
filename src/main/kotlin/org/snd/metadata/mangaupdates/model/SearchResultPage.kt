package org.snd.metadata.mangaupdates.model

data class SearchResultPage(
    val totalHits: Int,
    val page: Int,
    val perPage: Int,
    val results: Collection<SearchResult>
)
