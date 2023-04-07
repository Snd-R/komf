package org.snd.metadata.model.metadata

import com.squareup.moshi.JsonClass
import kotlin.math.floor

@JsonClass(generateAdapter = true)
data class BookRange(
    val start: Double,
    val end: Double
) {
    constructor(start: Double) : this(start, start)

    constructor(start: Int) : this(start.toDouble(), start.toDouble())

    override fun toString(): String {
        val start = if (floor(start) == start) start.toInt() else start
        val end = if (floor(end) == end) end.toInt() else end
        return if (start == end) {
            start.toString()
        } else "$start-$end"
    }
}
