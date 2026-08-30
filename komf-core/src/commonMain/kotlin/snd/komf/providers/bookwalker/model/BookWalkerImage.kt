package snd.komf.providers.bookwalker.model

import snd.komf.providers.bookwalker.sosBrigadeCDN


@JvmInline
value class BookWalkerImageId(val value: String)

data class BookWalkerImage(
    val id: BookWalkerImageId,
    val name: String,
    val mime: String,
    val width: Int,
    val height: Int,
) {

    val url600 = buildString {
        append(sosBrigadeCDN)
        val idString = id.value
        val first = idString.slice(0..2)
        val second = idString.slice(3..3)
        val third = idString.slice(4..4)
        val last = idString.slice(5..<idString.length)
        append("/600")
        append("/$first")
        append("/$second")
        append("/$third")
        append("/$last")
        append(".webp")
    }
}