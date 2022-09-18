package org.snd.metadata.providers.kodansha.model

data class KodanshaSeries(
    val id: KodanshaSeriesId,
    val title: String,
    val coverUrl: String?,
    val summary: String?,
    val authors: Collection<String>,
    val ageRating: Int?,
    val status: Status?,
    val tags: Collection<String>,
    val books: Collection<KodanshaSeriesBook>,
    val publisher: String = "Kodansha"
)

data class KodanshaSeriesBook(
    val id: KodanshaBookId,
    val number: Int?,
)

enum class Status {
    ONGOING,
    COMPLETED,
    COMPLETE
}
