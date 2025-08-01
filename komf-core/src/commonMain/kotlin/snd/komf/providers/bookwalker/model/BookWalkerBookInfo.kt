package snd.komf.providers.bookwalker.model

import kotlinx.serialization.Serializable

@Serializable
data class BookWalkerBookInfo(
    val productId: Int,
    val uuid: String,
    val thumbnailImageUrl: String
) {
}