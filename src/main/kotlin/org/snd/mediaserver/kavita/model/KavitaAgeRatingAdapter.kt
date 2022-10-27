package org.snd.mediaserver.kavita.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.mediaserver.kavita.model.KavitaAgeRating.*

class KavitaAgeRatingAdapter {

    @FromJson
    @AgeRating
    fun fromJson(status: Int): KavitaAgeRating =
        when (status) {
            -1 -> NOT_APPLICABLE
            0 -> UNKNOWN
            1 -> RATING_PENDING
            2 -> EARLY_CHILDHOOD
            3 -> EVERYONE
            4 -> G
            5 -> EVERYONE_10PLUS
            6 -> PG
            7 -> KIDS_TO_ADULTS
            8 -> TEEN
            9 -> MATURE_15PLUS
            10 -> MATURE_17PLUS
            11 -> MATURE
            12 -> R_18PLUS
            13 -> ADULTS_ONLY
            14 -> X_18PLUS
            else -> throw RuntimeException("Unsupported status code $status")
        }

    @ToJson
    fun toJson(@AgeRating ageRating: KavitaAgeRating): Int = ageRating.id
}
