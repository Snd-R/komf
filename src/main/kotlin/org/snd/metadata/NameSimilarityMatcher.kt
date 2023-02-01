package org.snd.metadata

import org.apache.commons.text.similarity.LevenshteinDistance
import org.snd.metadata.NameMatchingMode.CLOSEST_MATCH
import org.snd.metadata.NameMatchingMode.EXACT

class NameSimilarityMatcher private constructor(
    mode: NameMatchingMode
) {
    private val levenshteinDistance = LevenshteinDistance.getDefaultInstance()
    private val distanceThreshold = if (mode == EXACT) 0 else 3

    fun matches(name: String, namesToMatch: Collection<String>): Boolean {
        return namesToMatch.map { matches(name, it) }.any { it }
    }

    fun matches(name: String, nameToMatch: String): Boolean {
        val distance = levenshteinDistance.apply(name.uppercase(), nameToMatch.uppercase())
        return distance <= distanceThreshold
    }

    companion object {
        private val EXACT_MATCHER: NameSimilarityMatcher = NameSimilarityMatcher(EXACT)
        private val CLOSEST_MATCH_MATCHER: NameSimilarityMatcher = NameSimilarityMatcher(CLOSEST_MATCH)

        fun getInstance(mode: NameMatchingMode): NameSimilarityMatcher {
            return when (mode) {
                EXACT -> EXACT_MATCHER
                CLOSEST_MATCH -> CLOSEST_MATCH_MATCHER
            }
        }
    }
}
