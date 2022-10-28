package org.snd.metadata.mylar.model


enum class MylarAgeRating(val value: String, val ageRating: Int? = null) {
    ALL("All", 0),
    NINE("9+", 9),
    TWELVE("12+", 12),
    FIFTEEN("15+", 15),
    SEVENTEEN("17+", 17),
    ADULT("Adult", 18)
}
