package snd.komf.providers.bookwalker.model

@JvmInline
value class BookWalkerTagId(val value: String)

data class BookWalkerTag(
    val id: BookWalkerTagId,
    val name: String,
    val slug: String,
    val description: String,
    val namespace: Int,
    val priority: Int
)