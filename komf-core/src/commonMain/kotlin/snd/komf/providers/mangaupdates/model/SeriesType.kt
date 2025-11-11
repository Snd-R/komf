package snd.komf.providers.mangaupdates.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SeriesType {
    @SerialName("Artbook")
    ARTBOOK,

    @SerialName("Doujinshi")
    DOUJINSHI,

    @SerialName("Filipino")
    FILIPINO,

    @SerialName("Indonesian")
    INDONESIAN,

    @SerialName("Manga")
    MANGA,

    @SerialName("Manhwa")
    MANHWA,

    @SerialName("Manhua")
    MANHUA,

    @SerialName("OEL")
    OEL,

    @SerialName("Thai")
    THAI,

    @SerialName("Vietnamese")
    VIETNAMESE,

    @SerialName("Malaysian")
    MALAYSIAN,

    @SerialName("Nordic")
    NORDIC,

    @SerialName("French")
    FRENCH,

    @SerialName("Spanish")
    SPANISH,

    @SerialName("Novel")
    NOVEL
}
