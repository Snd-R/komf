package org.snd.metadata

object BookFilenameParser {
    private val volumeRegex = "\\s\\(?[vtT](?<volume>[0-9]+)(?<volumeRange>-[0-9]+)?\\)?".toRegex()
    private val chapterRegex = "\\sc?(?<startChapter>[0-9]+([.x][0-9]+)?)(?<endChapter>-c?[0-9]+([.x][0-9]+)?)?".toRegex()
    private val extraDataRegex = "\\[(?<extra>.*?)\\]".toRegex()
    private val titleRegex = "(?<title>^.*\\s-)".toRegex()

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
        val matchedGroups = chapterRegex.find(name)?.groups
        val startChapter = matchedGroups?.get("startChapter")?.value
            ?.replace("x", ".")
            ?.toDoubleOrNull()
        val endChapter = matchedGroups?.get("endChapter")?.value
            ?.replace("-", "")
            ?.replace("c", "")
            ?.replace("x", ".")
            ?.toDoubleOrNull()

        return if (startChapter != null && endChapter != null) {
            ChapterRange(startChapter, endChapter)
        } else if (startChapter != null) {
            ChapterRange(startChapter, startChapter)
        } else null
    }

    fun getTitle(name: String): String? {
        return titleRegex.find(name)?.groups?.get("title")?.value?.removeSuffix(" -")
    }

    fun getExtraData(name: String): List<String> {
        return extraDataRegex.findAll(name).mapNotNull { it.groups["extra"]?.value }.toList()
    }

    data class ChapterRange(
        val start: Double,
        val end: Double
    )
}
