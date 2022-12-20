package org.snd.metadata.model

import kotlin.math.floor

data class BookRange(
    val start: Double,
    val end: Double
) {
    override fun toString(): String {
        val start = if (floor(start) == start) start.toInt() else start
        val end = if (floor(end) == end) end.toInt() else end
        return if (start == end) {
            start.toString()
        } else "$start-$end"
    }
}
