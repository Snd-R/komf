package org.snd.metadata.providers.kodansha.model

data class KodanshaBookListPage(
    val page: Int,
    val totalPages: Int,
    val books: Collection<KodanshaSeriesBook>
) {
}
