package org.snd.metadata.comicinfo.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
data class ComicInfo(
    @XmlElement(true)
    @XmlSerialName("Title", "", "")
    val title: String? = null,

    @XmlElement(true)
    @XmlSerialName("Series", "", "")
    val series: String? = null,

    @XmlElement(true)
    @XmlSerialName("Number", "", "")
    val number: String? = null,

    @XmlElement(true)
    @XmlSerialName("Count", "", "")
    val count: Int? = null,

    @XmlElement(true)
    @XmlSerialName("Volume", "", "")
    val volume: Int? = null,

    @XmlElement(true)
    @XmlSerialName("AlternateSeries", "", "")
    val alternateSeries: String? = null,

    @XmlElement(true)
    @XmlSerialName("AlternateNumber", "", "")
    val alternateNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("AlternateCount", "", "")
    val alternateCount: Int? = null,

    @XmlElement(true)
    @XmlSerialName("Summary", "", "")
    val summary: String? = null,

    @XmlElement(true)
    @XmlSerialName("Notes", "", "")
    val notes: String? = null,

    @XmlElement(true)
    @XmlSerialName("Year", "", "")
    val year: Int? = null,

    @XmlElement(true)
    @XmlSerialName("Month", "", "")
    val month: Int? = null,

    @XmlElement(true)
    @XmlSerialName("Day", "", "")
    val day: Int? = null,

    @XmlElement(true)
    @XmlSerialName("Writer", "", "")
    val writer: String? = null,

    @XmlElement(true)
    @XmlSerialName("Penciller", "", "")
    val penciller: String? = null,

    @XmlElement(true)
    @XmlSerialName("Inker", "", "")
    val inker: String? = null,

    @XmlElement(true)
    @XmlSerialName("Colorist", "", "")
    val colorist: String? = null,

    @XmlElement(true)
    @XmlSerialName("Letterer", "", "")
    val letterer: String? = null,

    @XmlElement(true)
    @XmlSerialName("CoverArtist", "", "")
    val coverArtist: String? = null,

    @XmlElement(true)
    @XmlSerialName("Editor", "", "")
    val editor: String? = null,

    @XmlElement(true)
    @XmlSerialName("Translator", "", "")
    val translator: String? = null,

    @XmlElement(true)
    @XmlSerialName("Publisher", "", "")
    val publisher: String? = null,

    @XmlElement(true)
    @XmlSerialName("Imprint", "", "")
    val imprint: String? = null,

    @XmlElement(true)
    @XmlSerialName("Genre", "", "")
    val genre: String? = null,

    @XmlElement(true)
    @XmlSerialName("Tags", "", "")
    val tags: String? = null,

    @XmlElement(true)
    @XmlSerialName("Web", "", "")
    val web: String? = null,

    @XmlElement(true)
    @XmlSerialName("PageCount", "", "")
    val pageCount: Int? = null,

    @XmlElement(true)
    @XmlSerialName("LanguageISO", "", "")
    val languageISO: String? = null,

    @XmlElement(true)
    @XmlSerialName("Format", "", "")
    val format: String? = null,

    @XmlElement(true)
    @XmlSerialName("BlackAndWhite", "", "")
    val blackAndWhite: String? = null,

    @XmlElement(true)
    @XmlSerialName("Manga", "", "")
    val manga: String? = null,

    @XmlElement(true)
    @XmlSerialName("Characters", "", "")
    val characters: String? = null,

    @XmlElement(true)
    @XmlSerialName("Teams", "", "")
    val teams: String? = null,

    @XmlElement(true)
    @XmlSerialName("Locations", "", "")
    val locations: String? = null,

    @XmlElement(true)
    @XmlSerialName("ScanInformation", "", "")
    val scanInformation: String? = null,

    @XmlElement(true)
    @XmlSerialName("StoryArc", "", "")
    val storyArc: String? = null,

    @XmlElement(true)
    @XmlSerialName("StoryArcNumber", "", "")
    val storyArcNumber: String? = null,

    @XmlElement(true)
    @XmlSerialName("SeriesGroup", "", "")
    val seriesGroup: String? = null,

    @XmlElement(true)
    @XmlSerialName("AgeRating", "", "")
    val ageRating: String? = null,

    @XmlElement(true)
    @XmlSerialName("Rating", "", "")
    val rating: String? = null,

    @XmlElement(true)
    @XmlSerialName("LocalizedTitle", "", "")
    val localizedTitle: String? = null,

    @XmlElement(true)
    @XmlSerialName("Pages", "", "")
    @XmlChildrenName("Page", "", "")
    val pages: Collection<Page>? = null
)

@Serializable
data class Page(
    @XmlSerialName("Image", "", "")
    val image: Int? = null,

    @XmlSerialName("Type", "", "")
    val type: String? = null,

    @XmlSerialName("DoublePage", "", "")
    val doublePage: Boolean? = null,

    @XmlSerialName("ImageSize", "", "")
    val imageSize: Long? = null,

    @XmlSerialName("Key", "", "")
    val key: String? = null,

    @XmlSerialName("Bookmark", "", "")
    val bookmark: String? = null,

    @XmlSerialName("ImageWidth", "", "")
    val imageWidth: Int? = null,

    @XmlSerialName("ImageHeight", "", "")
    val imageHeight: Int? = null
)
