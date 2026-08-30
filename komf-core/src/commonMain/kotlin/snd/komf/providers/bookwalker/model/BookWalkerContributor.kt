package snd.komf.providers.bookwalker.model

@JvmInline
value class BookWalkerContributorId(val value: String)

data class BookWalkerContributor(
    val id: BookWalkerContributorId,
    val role: BookWalkerContributorRole,
    val name: String
)

enum class BookWalkerContributorRole(val number: Int) {
    CONTRIBUTOR(0),
    AUTHOR(1),
    ARTIST(2),
    ILLUSTRATOR(3),
    UNKNOWN_4(4),
    UNKNOWN_5(5),
    COLORIST(6),
    LETTERER(7),
    EDITOR(8),
    TRANSLATOR(9),
    NARRATOR(10),
    ORIGINAL_AUTHOR(11),
    ORIGINAL_CHARACTER_DESIGN(12),
    ADAPTATION(13),
    COMPILATION(14),
    CONSULTING_EDITOR(15),
    COVER_DESIGN(16),
    CREATOR(17),
    DESIGNER(18),
    COORDINATION(19),
    IDEA(20),
    UNKNOWN_21(21),
    PRODUCER(22),
    SCRIPT(23),
    TEXT(24),
    WITH(25),
    UNKNOWN(-1);

    companion object {
        fun valueOf(number: Int): BookWalkerContributorRole {
            return BookWalkerContributorRole.entries.getOrNull(number) ?: UNKNOWN
        }
    }
}