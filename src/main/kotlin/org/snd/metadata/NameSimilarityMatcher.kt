package org.snd.metadata

import org.apache.commons.text.similarity.JaroWinklerSimilarity

object NameSimilarityMatcher {
    private val similarity = JaroWinklerSimilarity()

    fun matches(name: String, namesToMatch: Collection<String>): Boolean {
        return namesToMatch.map { matches(name, it) }.any { it }
    }

    fun matches(name: String, nameToMatch: String): Boolean {
        val similarity = similarity.apply(name.uppercase(), nameToMatch.uppercase())
        return similarity > 0.9
    }
}
