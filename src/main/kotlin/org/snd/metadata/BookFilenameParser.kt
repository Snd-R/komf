package org.snd.metadata

object BookFilenameParser {
    private val volumeRegex = "\\s\\(?[vtT](?<volume>[0-9]+)(?<volumeRange>-[0-9]+)?\\)?".toRegex()
    private val chapterRegex = "\\sc?(?<startChapter>[0-9]+([.x#][0-9]+)?)(?<endChapter>-[0-9]+([.x#][0-9]+)?)?".toRegex()
    private val extraDataRegex = "\\[(?<extra>.*?)]".toRegex()

    fun getVolumes(name: String): IntRange? {
        val matchedGroups = volumeRegex.find(name)?.groups
        val volume = matchedGroups?.get("volume")?.value?.toIntOrNull()
        val volumeRange = matchedGroups?.get("volumeRange")?.value?.toIntOrNull()

        return if (volume != null && volumeRange != null) {
            volume..volumeRange
        } else if (volume != null) {
            volume..volume
        } else null
    }

    fun getChapters(name: String): ChapterRange? {
        val matchedGroups = chapterRegex.findAll(name).lastOrNull()?.groups
        val startChapter = matchedGroups?.get("startChapter")?.value
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()
        val endChapter = matchedGroups?.get("endChapter")?.value
            ?.replace("-", "")
            ?.replace("[x#]".toRegex(), ".")
            ?.toDoubleOrNull()

        return if (startChapter != null && endChapter != null) {
            ChapterRange(startChapter, endChapter)
        } else if (startChapter != null) {
            ChapterRange(startChapter, startChapter)
        } else null
    }

    fun getExtraData(name: String): List<String> {
        return extraDataRegex.findAll(name).mapNotNull { it.groups["extra"]?.value }.toList()
    }

    data class ChapterRange(
        val start: Double,
        val end: Double
    )
}
