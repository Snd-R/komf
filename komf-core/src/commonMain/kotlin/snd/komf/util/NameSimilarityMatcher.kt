package snd.komf.util

import snd.komf.util.NameSimilarityMatcher.NameMatchingMode.CLOSEST_MATCH
import snd.komf.util.NameSimilarityMatcher.NameMatchingMode.EXACT
import kotlin.math.min


class NameSimilarityMatcher private constructor(private val mode: NameMatchingMode) {

    fun matches(name: String, namesToMatch: Collection<String>): Boolean {
        return namesToMatch.map { matches(name, it) }.any { it }
    }

    fun matches(name: String, nameToMatch: String): Boolean {
        return if (mode == EXACT || name.length in 1..3) name == nameToMatch
        else {
            val distance = levenshtein(name.uppercase(), nameToMatch.uppercase())
            val distanceThreshold = when (name.length) {
                in 4..6 -> 1
                in 7..9 -> 2
                else -> 3
            }
            return distance <= distanceThreshold
        }
    }

    companion object {
        private val EXACT_MATCHER: NameSimilarityMatcher = NameSimilarityMatcher(EXACT)
        private val CLOSEST_MATCH_MATCHER: NameSimilarityMatcher = NameSimilarityMatcher(CLOSEST_MATCH)

        fun nameSimilarityMatcher(mode: NameMatchingMode): NameSimilarityMatcher {
            return when (mode) {
                EXACT -> EXACT_MATCHER
                CLOSEST_MATCH -> CLOSEST_MATCH_MATCHER
            }
        }
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        if (lhs == rhs) {
            return 0
        }
        if (lhs.isEmpty()) {
            return rhs.length
        }
        if (rhs.isEmpty()) {
            return lhs.length
        }

        val lhsLength = lhs.length + 1
        val rhsLength = rhs.length + 1

        var cost = Array(lhsLength) { it }
        var newCost = Array(lhsLength) { 0 }

        for (i in 1..<rhsLength) {
            newCost[0] = i

            for (j in 1..<lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength - 1]
    }

    enum class NameMatchingMode {
        EXACT,
        CLOSEST_MATCH,
    }
}
