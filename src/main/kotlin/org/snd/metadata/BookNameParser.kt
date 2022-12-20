package org.snd.metadata

import org.snd.metadata.model.BookRange

object BookNameParser {
    private val volumeRegexes = listOf(
        "(?i),?\\s\\(?volume\\s(?<volumeStart>[0-9]+)(,?\\s?[0-9]+,)+(?<volumeEnd>\\s?[0-9]+)\\)?".toRegex(),
        "(?i),?\\s\\(?([vtT]|vols\\.\\s|vol\\.\\s|volume\\s)(?<startVolume>[0-9]+([.x#][0-9]+)?)(?<endVolume>-[0-9]+([.x#][0-9]+)?)?\\)?".toRegex(),
    )

    private val chapterRegex = "\\sc?(?<startChapter>[0-9]+([.x#][0-9]+)?)(?<endChapter>-[0-9]+([.x#][0-9]+)?)?".toRegex()
    private val extraDataRegex = "\\[(?<extra>.*?)]".toRegex()

    fun getVolumes(name: String): BookRange? {
        val matchedGroups = volumeRegexes.firstNotNullOfOrNull { it.find(name)?.groups }
        val startVolume = matchedGroups?.get("startVolume")?.value
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()
        val endVolume = matchedGroups?.get("endVolume")?.value
            ?.replace("-", "")
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()

        return if (startVolume != null && endVolume != null) {
            BookRange(startVolume, endVolume)
        } else if (startVolume != null) {
            BookRange(startVolume, startVolume)
        } else null
    }

    fun getChapters(name: String): BookRange? {
        val matchedGroups = chapterRegex.findAll(name).lastOrNull()?.groups
        val startChapter = matchedGroups?.get("startChapter")?.value
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()
        val endChapter = matchedGroups?.get("endChapter")?.value
            ?.replace("-", "")
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()

        return if (startChapter != null && endChapter != null) {
            BookRange(startChapter, endChapter)
        } else if (startChapter != null) {
            BookRange(startChapter, startChapter)
        } else null
    }

    fun getExtraData(name: String): List<String> {
        return extraDataRegex.findAll(name).mapNotNull { it.groups["extra"]?.value }.toList()
    }
}
