package snd.komf.providers.webtoons

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.select.Elements
import io.ktor.http.*
import snd.komf.providers.webtoons.model.PersonInfo
import snd.komf.providers.webtoons.model.Status
import snd.komf.providers.webtoons.model.WebtoonsSeries
import snd.komf.providers.webtoons.model.WebtoonsSeriesId

class WebtoonsParser {
    fun parseSeries(series: String): WebtoonsSeries {
        val document = Ksoup.parse(series)

        val detailElement = document.select(".detail_header > div.info")
        val infoElement = document.select("#_asideDetail")
        val peopleElement = document.select("#wrap > div._authorInfoLayer div._authorInnerContent")

        val title = document.selectFirst("h1.subj, h3.subj")!!.text()
        val description = infoElement.select("p.summary").text()
        val status: Status = with(infoElement?.selectFirst("p.day_info")?.text().orEmpty()) {
            when {
                contains("UP") || contains("EVERY") || contains("NOUVEAU") -> Status.ONGOING
                contains("END") || contains("COMPLETED") || contains("TERMINÉ") -> Status.COMPLETED
                else -> Status.UNKNOWN
            }
        }
        val imageUrl = document.selectFirst("head meta[property=\"og:image\"]")?.attr("content")
        val genres = detailElement.select(".genre").map { it.text() }

        val author: PersonInfo?
        var adaptedBy: PersonInfo? = null
        val artist: PersonInfo?

        if (peopleElement.isNotEmpty()) {
            author = getPersonInfo(peopleElement, "Original work by")
            adaptedBy = getPersonInfo(peopleElement, "Adapted by")
            artist = getPersonInfo(peopleElement, "Art by")
        } else {
            val authorBackup = detailElement.select(".author_area").first()?.ownText()!!
            val authorName = detailElement.select(".author:nth-of-type(1)").first()?.ownText() ?: authorBackup
            val artistName = detailElement.select(".author:nth-of-type(2)").first()?.ownText()

            author = PersonInfo(authorName)
            artist = PersonInfo(artistName ?: authorName)
        }

        val views = infoElement.select("li span.ico_view + em").text()
        val subscribers = infoElement.select("li span.ico_subscribe + em").text()

        return WebtoonsSeries(
            id = WebtoonsSeriesId(idFromUrl(getDocumentUrl(document))),
            title = title,
            description = description,
            url = getDocumentUrl(document),
            thumbnailUrl = imageUrl,
            genres = genres,
            status = status,
            author = author,
            adaptedBy = adaptedBy,
            artist = artist,
            views = views,
            subscribers = subscribers,
            chapters = null
        )
    }

    private fun getDocumentUrl(document: Document): String {
        return document.getElementsByTag("meta").first { it.attr("property") == "og:url" }.attr("content")
    }

    private fun idFromUrl(urlRaw: String): String {
        return Url(urlRaw).encodedPathAndQuery
    }

    private fun getPersonInfo(peopleElement: Elements, text: String): PersonInfo? {
        val title = peopleElement.select("p.by:contains($text) + h3.title").first()?.text() ?: return null
        val description =
            peopleElement.select("p.by:contains($text) + h3.title").first()?.parent()?.select("p.desc")?.first()?.text()
        return PersonInfo(title, description)
    }
}
