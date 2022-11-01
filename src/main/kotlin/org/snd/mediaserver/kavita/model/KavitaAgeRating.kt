package org.snd.mediaserver.kavita.model

enum class KavitaAgeRating(val id: Int, val ageRating: Int? = null) {
    NOT_APPLICABLE(-1),
    UNKNOWN(0),
    RATING_PENDING(1, 0),
    EARLY_CHILDHOOD(2, 3),
    EVERYONE(3, 0),
    G(4, 0),
    EVERYONE_10PLUS(5, 10),
    PG(6, 8),
    KIDS_TO_ADULTS(7, 6),
    TEEN(8, 13),
    MATURE_15PLUS(9, 15),
    MATURE_17PLUS(10, 17),
    MATURE(11, 17),
    R_18PLUS(12, 18),
    ADULTS_ONLY(13, 18),
    X_18PLUS(14, 18)
}