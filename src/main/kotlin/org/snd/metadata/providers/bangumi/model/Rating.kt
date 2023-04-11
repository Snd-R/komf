package org.snd.metadata.providers.bangumi.model
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Rating (

    val rank: Int,

    val total: Int,

    val count: Map<Int, Int>,

    val score: java.math.BigDecimal

)