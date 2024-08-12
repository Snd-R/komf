package snd.komf.providers.kodansha.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class KodanshaBook(
    val id: Int,
    val name: String,
    val volumeNumber: Int?,
    val chapterNumber: Int?,
    val description: String?,
    val readable: KodanshaBookReadable,
    val variants: List<KodanshaBookVariant>,

    val ageRating: String?,
    val publishDate: LocalDateTime?,
    val categoryId: Int,
    val category: String,
    val subCategoryId: Int,
    val subCategory: String,
    val thumbnails: List<KodanshaThumbnail>,

    val creators: List<KodanshaCreator>?,

    val readableUrl: String?,
)

@JvmInline
value class KodanshaBookId(val id: Int)

@Serializable
data class KodanshaBookVariant(
    val type: String,
    val price: Double?,
    val fullPrice: Double?,
    val isComingSoon: Boolean?,
    val isPreorder: Boolean?,
    val priceType: String?,
    val id: Int,
    val description: String?,
    val isOnSale: Boolean?,
    val userDefaultProductImage: Boolean?,
    val thumbnails: List<KodanshaThumbnail>,
)

@Serializable
data class KodanshaBookReadable(
    val seriesId: String,
    val genres: List<KodanshaGenre>?,
    val isbn: String?,
    val eisbn: String?,
    val pageCount: Int?,
    val coverType: String?,
    val colorType: String?,
    val printReleaseDate: LocalDateTime?,
    val digitalReleaseDate: LocalDateTime?,
    val releaseDate: LocalDateTime?
)
