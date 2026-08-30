package snd.komf.providers.bookwalker.model

import snd.komf.providers.bookwalker.bookWalkerBaseUrl
import kotlin.time.Instant


@JvmInline
value class BookWalkerBookId(val value: String) {
    override fun toString() = value
}

data class BookWalkerBook(
    val id: BookWalkerBookId,
    val contentId: String,
    val seriesId: BookWalkerSeriesId,
    val level: Int,
    val contentType: BookWalkerContentType,
    val format: BookWalkerBookFormat,
    val title: String,
    val altTitles: List<String>,
    val subtitle: String,
    val displayTitle: String,
    val displayTitleShort: String,
    val description: String,
    val descriptionShort: String,
    val displayOrder: Double,
    val listedAt: Instant,
    val labelId: String,
    val geoblockId: String?,
    val displayName: String,
    val copyright: String?,
    val onPresaleAt: Instant?,
    val onSaleAt: Instant,
    val offSaleAt: Instant?,
    val addOn: Int,
    val addOnCampaignOnly: Int,

    val tags: List<BookWalkerTag>,
    val contributors: List<BookWalkerContributor>,
    val image: BookWalkerImage?,
    val isbn: String?
) {

    val url = buildString {
        append(bookWalkerBaseUrl)
        when (level) {
            3 -> append("/chapter/")
            else -> append("/volume/")
        }
        append(contentId.removePrefix("CNT_"))
    }
}

enum class BookWalkerBookFormat(val number: Int) {
    EBOOK(1),
    AUDIOBOOK(2);

    companion object {
        fun valueOf(number: Int): BookWalkerBookFormat {
            return BookWalkerBookFormat.entries.getOrNull(number) ?: EBOOK
        }
    }
}
