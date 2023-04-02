package org.snd.metadata.providers.bangumi.model

enum class SubjectType(val value: Int) {
    Book(1),
    Anime(2),
    Music(3),
    Game(4),
    Real(6);

    companion object {
        fun fromValue(value: Int): SubjectType {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid SubjectType value: $value")
        }
    }
}