package org.snd.mediaserver.kavita.model

enum class KavitaAgeRating(val id: Int) {
    NOT_APPLICABLE(-1),
    UNKNOWN(0),
    RATING_PENDING(1),
    EARLY_CHILDHOOD(2),
    EVERYONE(3),
    G(4),
    EVERYONE_10PLUS(5),
    PG(6),
    KIDS_TO_ADULTS(7),
    TEEN(8),
    MATURE_15PLUS(9),
    MATURE_17PLUS(10),
    MATURE(11),
    R_18PLUS(12),
    ADULTS_ONLY(13),
    X_18PLUS(14)
}