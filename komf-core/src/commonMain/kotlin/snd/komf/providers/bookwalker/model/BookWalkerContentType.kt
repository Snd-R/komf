package snd.komf.providers.bookwalker.model

enum class BookWalkerContentType(val number: Int) {
    MANGA(1),
    NOVEL(2),
    WEBTOONS(3),
    AUDIOBOOK(4);

    companion object {
        fun valueOf(number: Int): BookWalkerContentType {
            return BookWalkerContentType.entries.getOrNull(number) ?: MANGA
        }
    }
}
