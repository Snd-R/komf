package snd.komf.providers.ehentai

import snd.komf.model.ProviderSeriesId
import snd.komf.providers.ehentai.model.EHentaiParsedTitle

object EHentaiParser {

    private val platformPrefixRegex = """
        ^\[(pixiv|fanbox|fantia|patreon|gumroad|ci-en)([\s/,&|]+(pixiv|fanbox|fantia|patreon|gumroad|ci-en))*]
        """
        .trimIndent()
        .toRegex(RegexOption.IGNORE_CASE)

    fun parseGid(seriesId: ProviderSeriesId): Pair<Int, String> {
        val parts = seriesId.value.split(";", limit = 2)

        require(parts.size == 2) { "Invalid E-Hentai ID format: ${seriesId.value}" }
        val gid = parts[0].toIntOrNull()
            ?: throw IllegalArgumentException("Invalid GID (Not a number) in ID: ${seriesId.value}")
        val token = parts[1]

        return Pair(gid, token)
    }

    fun parseTitle(rawTitle: String): EHentaiParsedTitle {
        if (rawTitle.isBlank()) return EHentaiParsedTitle("", "")

        // Remove all trailing [xxx] tags
        val trailingRegex = """(?:\s*\[[^]]+])+$""".toRegex()
        val withoutTrailing = rawTitle.replace(trailingRegex, "").trim()

        /// Remove leading convention and artist information
        val leadingRegex = """^(?:\([^)]+\)\s*)?(?:\[[^]]+]\s*)?""".toRegex()
        var baseTitle = withoutTrailing.replace(leadingRegex, "").trim()

        // Pure date digits or artist's image pack only
        if (baseTitle.matches("""^[\d\-.\s]+$""".toRegex()) ||
            platformPrefixRegex.containsMatchIn(rawTitle)
        ) {
            baseTitle = withoutTrailing
        }

        // Take the final translated name or original name to the right of "|"
        val bestMatch = if (baseTitle.contains("|")) {
            baseTitle.substringAfterLast("|").trim()
        } else {
            baseTitle
        }

        if (baseTitle.isBlank()) {
            baseTitle = rawTitle
        }

        return EHentaiParsedTitle(match = baseTitle, bestMatch = bestMatch.ifBlank { baseTitle })
    }

    /**
     * ### Generate up to 3 search variants
     * ``` md
     * 1. [a (b)] c (d) [e] [f]
     * 2. c (d)
     * 3. c
     * ```
     */
    fun getSearchQueries(rawTitle: String): List<String> {
        val parsed = parseTitle(rawTitle)

        val coreTitleRegex = """(?:\s*[（(][^)）]+[)）])+$""".toRegex()
        val coreTitle = parsed.bestMatch.replace(coreTitleRegex, "").trim()
        val finalCoreTitle = coreTitle.ifBlank { parsed.bestMatch }

        return listOf(rawTitle, parsed.bestMatch, finalCoreTitle)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
}