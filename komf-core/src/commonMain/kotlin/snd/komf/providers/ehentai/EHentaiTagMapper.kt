package snd.komf.providers.ehentai

object EHentaiTagMapper {

    val BCP47_MAP: Map<String, String> = mapOf(
        "language:afrikaans" to "af",
        "language:albanian" to "sq",
        "language:arabic" to "ar",
        "language:aramaic" to "arc", // ISO 639-2
        "language:armenian" to "hy",
        "language:bengali" to "bn",
        "language:bosnian" to "bs",
        "language:bulgarian" to "bg",
        "language:burmese" to "my",
        "language:catalan" to "ca",
        "language:cebuano" to "ceb", // ISO 639-2
        "language:chinese" to "zh",
        "language:cree" to "cr",
        "language:creole" to "ht",
        "language:croatian" to "hr",
        "language:czech" to "cs",
        "language:danish" to "da",
        "language:dutch" to "nl",
        "language:english" to "en",
        "language:esperanto" to "eo",
        "language:estonian" to "et",
        "language:finnish" to "fi",
        "language:french" to "fr",
        "language:georgian" to "ka",
        "language:german" to "de",
        "language:greek" to "el",
        "language:gujarati" to "gu",
        "language:hebrew" to "he",
        "language:hindi" to "hi",
        "language:hmong" to "hmn",   // ISO 639-2
        "language:hungarian" to "hu",
        "language:icelandic" to "is",
        "language:indonesian" to "id",
        "language:irish" to "ga",
        "language:italian" to "it",
        "language:japanese" to "ja",
        "language:javanese" to "jv",
        "language:kannada" to "kn",
        "language:kazakh" to "kk",
        "language:khmer" to "km",
        "language:korean" to "ko",
        "language:kurdish" to "ku",
        "language:ladino" to "lad",  // ISO 639-2
        "language:lao" to "lo",
        "language:latin" to "la",
        "language:latvian" to "lv",
        "language:marathi" to "mr",
        "language:mongolian" to "mn",
        "language:ndebele" to "nd",
        "language:nepali" to "ne",
        "language:norwegian" to "no",
        "language:oromo" to "om",
        "language:papiamento" to "pap", // ISO 639-2
        "language:pashto" to "ps",
        "language:persian" to "fa",
        "language:polish" to "pl",
        "language:portuguese" to "pt",
        "language:punjabi" to "pa",
        "language:romanian" to "ro",
        "language:russian" to "ru",
        "language:sango" to "sg",
        "language:sanskrit" to "sa",
        "language:serbian" to "sr",
        "language:shona" to "sn",
        "language:slovak" to "sk",
        "language:slovenian" to "sl",
        "language:somali" to "so",
        "language:spanish" to "es",
        "language:swahili" to "sw",
        "language:swedish" to "sv",
        "language:tagalog" to "tl",
        "language:tamil" to "ta",
        "language:telugu" to "te",
        "language:thai" to "th",
        "language:tibetan" to "bo",
        "language:tigrinya" to "ti",
        "language:turkish" to "tr",
        "language:ukrainian" to "uk",
        "language:urdu" to "ur",
        "language:vietnamese" to "vi",
        "language:welsh" to "cy",
        "language:yiddish" to "yi",
        "language:zulu" to "zu"
    )

    fun mapAgeRating(category: String?, tags: List<String>): Int? {
        val hasExtremeTags = tags.any { tag ->
            val cleanTag = if (tag.contains(":")) tag.substringAfter(":") else tag
            cleanTag in listOf("guro", "ryona", "snuff", "scat")
        }
        if (hasExtremeTags) return 18

        return when (category?.lowercase()) {
            "non-h" -> 15
            "doujinshi",
            "manga",
            "artist cg",
            "game cg",
            "image set",
            "western",
            "cosplay",
            "misc",
            "private" -> 18

            else -> null
        }
    }
}