package org.snd.metadata.providers.bangumi.model

enum class SubjectType(val value: Int) {
    BOOK(1),
    ANIME(2),
    MUSIC(3),
    GAME(4),
    REAL(6);

    companion object {
        fun fromValue(value: Int): SubjectType {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid SubjectType value: $value")
        }
    }
}