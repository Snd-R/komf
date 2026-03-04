package snd.komf.providers.ehentai.model

data class EHentaiParsedTitle(
    /**
     * ### Match title
     * ``` md
     * (Convention)[Artist/Circle] Title [Language] [Attribute tags] -> Title
     * ```
     */
    val match: String,
    /**
     * ### The rightmost title from a matched title
     * ```md
     * Title1 | Title2 | Title3 -> Title3
     * ```
     */
    val bestMatch: String
)