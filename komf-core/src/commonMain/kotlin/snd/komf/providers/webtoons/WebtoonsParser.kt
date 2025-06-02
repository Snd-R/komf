package snd.komf.providers.webtoons

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.select.Elements
import io.ktor.http.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import snd.komf.providers.webtoons.model.PersonInfo
import snd.komf.providers.webtoons.model.SearchResult
import snd.komf.providers.webtoons.model.WebtoonsChapter
import snd.komf.providers.webtoons.model.WebtoonsChapterId
import snd.komf.providers.webtoons.model.WebtoonsSeries
import snd.komf.providers.webtoons.model.WebtoonsSeriesId

class WebtoonsParser {
    private val chapterDateFormat = LocalDate.Format {
        monthName(
            MonthNames(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
        )
        char(' ')
        dayOfMonth(Padding.NONE)
        char(',')
        char(' ')
        year()
    }

    private val searchEntrySelector = "#content > div.card_wrap.search ul:not(#filterLayer) li a"

    fun parseSearchResults(results: String): Collection<SearchResult> {
        val document = Ksoup.parse(results)
        val searchEntries = document.select(searchEntrySelector)

        // logger.error { searchEntries }

        return searchEntries.mapNotNull { entry ->
            val url = entry.attr("href")

            // Technically title_no is the series ID, but if something has to be stored as an ID, the URL path is easier
            // val id = Url(url).parameters["title_no"] ?: return@mapNotNull null
            val id = Url(url).encodedPathAndQuery

            val title = entry.select("p.subj").text()
            val imageUrl = entry.select("img").attr("src")
            val genre = entry.select("p.genre").text()
            val author = entry.select("p.author").text()
            // Contains expressions like "100K" "3M" "653", not mapped to numbers since it's not used
            val views = entry.select("p.grade_area em.grade_num").text()

            SearchResult(
                id = WebtoonsSeriesId(id),
                title = title,
                url = url,
                imageUrl = imageUrl,
                genre = genre,
                author = author,
                views = views
            )
        }
    }

    fun parseSeries(series: String): WebtoonsSeries {
        val document = Ksoup.parse(series)

        val detailElement = document.select("#content > div.cont_box > div.detail_header > div.info")
        val infoElement = document.select("#_asideDetail")
        val peopleElement = document.select("#wrap > div._authorInfoLayer div._authorInnerContent")

        val title = document.selectFirst("h1.subj, h3.subj")!!.text()
        val description = infoElement.select("p.summary").text()
        val imageUrl = parseDetailsThumbnail(document)
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
        val score = infoElement.select("li span.ico_grade5 + em").text().toDouble()

        return WebtoonsSeries(
            id = WebtoonsSeriesId(idFromUrl(getDocumentUrl(document))),
            title = title,
            description = description,
            url = getDocumentUrl(document),
            thumbnailUrl = imageUrl,
            genres = genres,
            author = author,
            adaptedBy = adaptedBy,
            artist = artist,
            views = views,
            subscribers = subscribers,
            score = score,
            chapters = null
        )
    }

    private fun getDocumentUrl(document: Document): String {
        return document.getElementsByTag("meta").first { it.attr("property") == "og:url" }.attr("content")
    }

    private fun idFromUrl(urlRaw: String): String {
        return Url(urlRaw).encodedPathAndQuery
    }

    private fun parseDetailsThumbnail(document: Document): String? {
        val picElement = document.select("#content > div.cont_box > div.detail_body")
        val discoverPic = document.select("#content > div.cont_box > div.detail_header > span.thmb")
        return picElement.attr("style").substringAfter("url(").substringBeforeLast(")").removeSurrounding("\"")
            .removeSurrounding("'")
            .ifBlank { discoverPic.select("img").not("[alt='Representative image']").first()?.attr("src") }
    }

    private fun getPersonInfo(peopleElement: Elements, text: String): PersonInfo? {
        val title = peopleElement.select("p.by:contains($text) + h3.title").first()?.text() ?: return null
        val description =
            peopleElement.select("p.by:contains($text) + h3.title").first()?.parent()?.select("p.desc")?.first()?.text()
        return PersonInfo(title, description)
    }

    fun parseChapters(mobileSeries: String): Collection<WebtoonsChapter> {
        val document = Ksoup.parse(mobileSeries)

        val chapters = document.select("ul#_episodeList li[id*=episode]")

        return chapters.map {
            val urlElement = it.select("a")
            val chapterUrl = urlElement.attr("href")

            val title = it.select("a > div.row > div.info > p.sub_title > span.ellipsis").text()
            val chapterNumber = it.select("a > div.row > div.num").text().substringAfter("#").toDouble()

            val thumbnailUrl = it.select("a > div.row > div.pic > img._thumbnail").attr("data-image-url")

            val releaseDate = it.select("a > div.row > div.col > div.sub_info > span.date").text()
                .let { date -> LocalDate.parse(date, chapterDateFormat) }

            val likes = it.select("a > div.row > div.info > div.sub_info >span.likeList").text()

            WebtoonsChapter(
                id = WebtoonsChapterId(idFromUrl(chapterUrl)),
                title = title,
                url = chapterUrl,
                number = chapterNumber,
                thumbnailUrl = thumbnailUrl,
                releaseDate = releaseDate,
                likes = likes
            )
        }
    }
}
