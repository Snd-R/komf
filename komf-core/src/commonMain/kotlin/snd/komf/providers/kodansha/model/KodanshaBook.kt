package snd.komf.providers.kodansha.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class KodanshaBook(
    val id: Int,
    val name: String,
    val volumeNumber: Int? = null,
    val chapterNumber: Int? = null,
    val description: String? = null,
    val readable: KodanshaBookReadable,
    val variants: List<KodanshaBookVariant> = emptyList(),

    val ageRating: String? = null,
    val publishDate: LocalDateTime? = null,
    val categoryId: Int,
    val category: String,
    val subCategoryId: Int,
    val subCategory: String,
    val thumbnails: List<KodanshaThumbnail> = emptyList(),

    val creators: List<KodanshaCreator>? = null,

    val readableUrl: String?,
)

@JvmInline
value class KodanshaBookId(val id: Int)

@Serializable
data class KodanshaBookVariant(
    val type: String,
    val price: Double? = null,
    val fullPrice: Double? = null,
    val isComingSoon: Boolean? = null,
    val isPreorder: Boolean? = null,
    val priceType: String? = null,
    val id: Int,
    val description: String? = null,
    val isOnSale: Boolean? = null,
    val userDefaultProductImage: Boolean? = null,
    val thumbnails: List<KodanshaThumbnail>,
)

@Serializable
data class KodanshaBookReadable(
    val seriesId: String,
    val genres: List<KodanshaGenre>? = null,
    val isbn: String? = null,
    val eisbn: String? = null,
    val pageCount: Int? = null,
    val coverType: String? = null,
    val colorType: String? = null,
    val printReleaseDate: LocalDateTime? = null,
    val digitalReleaseDate: LocalDateTime? = null,
    val releaseDate: LocalDateTime? = null
)
