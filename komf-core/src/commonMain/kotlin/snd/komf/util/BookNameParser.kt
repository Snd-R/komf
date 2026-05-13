package snd.komf.util

import snd.komf.model.BookRange

object BookNameParser {
    private val volumeRegexes = listOf(
        "(?i),?\\s\\(?volume\\s(?<volumeStart>[0-9]+)(,?\\s?[0-9]+,)+(?<volumeEnd>\\s?[0-9]+)\\)?".toRegex(),
        "(?i),?\\s\\(?([vtT]|vols\\.\\s|vol\\.\\s|volume\\s)(?<volumeStart>[0-9]+([.x#][0-9]+)?)(?<volumeEnd>-[0-9]+([.x#][0-9]+)?)?\\)?".toRegex(),
        ".*第(?<volumeStart>\\d+)-?(?<volumeEnd>\\d+)?.*巻".toRegex(),
        ".*年(?:[0-9]+月)?(?:[0-9]+日)?(?<volumeStart>\\d+)-?(?<volumeEnd>\\d+)?号".toRegex(),
    )

    private val chapterRegexes = listOf(
        "(?i)(\\sc|\\s?ch\\.\\s|\\s?chapter\\s|\\s?ep(?:isode)?\\.?\\s)(?<start>[0-9]+([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?".toRegex(),
        ".*第(?<start>\\d+(\\.\\d+)?)-?(?<end>\\d+(\\.\\d+)?)?.*話".toRegex(),
    )
    private val bookNumberRegexes = listOf(
        "(?i)(?:\\s|#|no\\.)(?<start>[0-9]+[AB]?([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?(?:\\s\\(.*\\)\\s*)*$".toRegex(),
        "Issue (?<start>[0-9]+[AB]?([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?".toRegex(),
        "Volume (?<start>[0-9]+[AB]?([.x#][0-9]+)?)(?<end>-[0-9]+([.x#][0-9]+)?)?".toRegex(),
    )
    private val extraDataRegex = "\\[(?<extra>.*?)]".toRegex()

    private val yearRangeRegex = """\((?:19|20)\d{2}\s*-\s*(?:19|20)\d{2}\)""".toRegex()
    private val yearRegex = """\((?:19|20)\d{2}\)""".toRegex()
    private val squareBracketRegex = """\[[^\]]*\]""".toRegex()
    private val parenRegex = """\([^)]*\)""".toRegex()

    private val unicodeFractions = mapOf(
        "½" to ".5", "⅓" to ".33", "⅔" to ".67",
        "¼" to ".25", "¾" to ".75", "⅕" to ".2",
        "⅖" to ".4", "⅗" to ".6", "⅘" to ".8",
        "⅙" to ".17", "⅚" to ".83", "⅛" to ".125",
        "⅜" to ".375", "⅝" to ".625", "⅞" to ".875",
    )

    private fun preprocess(name: String): String {
        var s = unicodeFractions.entries.fold(name) { acc, (frac, dec) -> acc.replace(frac, dec) }
        s = yearRangeRegex.replace(s, " ")
        s = yearRegex.replace(s, " ")
        return s
    }

    fun getVolumes(name: String): BookRange? {
        val processed = preprocess(name)
        val matchedGroups = volumeRegexes.firstNotNullOfOrNull { it.find(processed)?.groups }
        val startVolume = matchedGroups?.get("volumeStart")?.value
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()
        val endVolume = matchedGroups?.get("volumeEnd")?.value
            ?.replace("-", "")
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()

        return if (startVolume != null && endVolume != null) {
            BookRange(startVolume, endVolume)
        } else if (startVolume != null) {
            BookRange(startVolume, startVolume)
        } else null
    }

    fun getChapters(name: String) = getBookNumber(preprocess(name), chapterRegexes)

    fun getBookNumber(name: String): BookRange? {
        val processed = preprocess(name)
        // Strip bracket content before fallback number detection to avoid
        // release group names or edition info containing digits contaminating the match
        val stripped = squareBracketRegex.replace(parenRegex.replace(processed, " "), " ")
        return getBookNumber(stripped, bookNumberRegexes)
    }

    private fun getBookNumber(name: String, regexes: List<Regex>): BookRange? {
        val matchedGroups = regexes.firstNotNullOfOrNull { it.findAll(name).lastOrNull()?.groups }
        val startChapter = matchedGroups?.get("start")?.value
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()
        val endChapter = matchedGroups?.get("end")?.value
            ?.replace("-", "")
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()

        return if (startChapter != null && endChapter != null) {
            BookRange(startChapter, endChapter)
        } else if (startChapter != null) {
            BookRange(startChapter)
        } else null
    }

    fun getExtraData(name: String): List<String> {
        return extraDataRegex.findAll(name).mapNotNull { it.groups["extra"]?.value }.toList()
    }
}
