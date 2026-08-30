package snd.komf.providers.bookwalker.model

import snd.komf.providers.bookwalker.bookWalkerBaseUrl
import kotlin.time.Instant


@JvmInline
value class BookWalkerSeriesId(val value: String) {
    override fun toString() = value
}

data class BookWalkerSeries(
    val id: BookWalkerSeriesId,
    val type: BookWalkerContentType,
    val title: String,
    val altTitles: List<String>,
    val subtitle: String,
    val displayTitle: String,
    val displayTitleShort: String,
    val description: String,
    val descriptionShort: String,
    val listedAt: Instant,

    val tags: List<BookWalkerTag>,
    val image: BookWalkerImage?,
) {

    val url = buildString {
        append("$bookWalkerBaseUrl/series/")
        append(id.value.removePrefix("CNT_"))
    }
}
