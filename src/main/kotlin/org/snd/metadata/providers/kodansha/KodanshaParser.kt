package org.snd.metadata.providers.kodansha

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.snd.metadata.providers.kodansha.model.*
import java.net.URLDecoder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class KodanshaParser {
    private val baseUrl = "https://kodansha.us"

    fun parseSearchResults(results: String): Collection<KodanshaSearchResult> {
        val document = Jsoup.parse(results)
        return document.getElementsByClass("filters__results").first()!!.child(0).children()
            .mapNotNull { parseSearchResult(it) }
    }

    private fun parseSearchResult(result: Element): KodanshaSearchResult? {
        val resultCard = result.child(0)
        val titleBlock = resultCard.child(0).child(0)
        val type = titleBlock.child(0).text().trim()
        if (type != "Manga Series:") return null

        val title = titleBlock.child(1).text().removeSuffix(" (manga)")
        val id = titleBlock.child(1).attr("href").removeSurrounding("$baseUrl/series/", "/")

        val imageUrl = resultCard.child(1).child(0).child(0).attr("src")

        return KodanshaSearchResult(
            seriesId = KodanshaSeriesId(URLDecoder.decode(id, "UTF-8")),
            title = title,
            imageUrl = imageUrl
        )
    }

    fun parseSeries(series: String): KodanshaSeries {
        val document = Jsoup.parse(series)
        val productDetail = document.getElementsByClass("product-detail-hero").first()!!.child(0)
        val title = productDetail.child(1).getElementsByClass("title title--product-page").first()!!
            .text().removeSuffix(" (manga)")
        val summary = productDetail.child(1).getElementsByClass("product-detail-hero__synopsis").first()?.text()
        val authors = parseAuthors(productDetail)
        val coverUrl = productDetail.child(0).child(0).child(0).attr("srcset")

        val productInfo = document.getElementsByClass("product-info").firstOrNull()?.child(1)?.child(0)?.child(0)

        val ageRating = productInfo?.getElementById("rating")?.siblingElements()?.firstOrNull()
            ?.text()?.removeSuffix("+")?.toInt()
        val status = productInfo?.getElementById("status")?.siblingElements()?.firstOrNull()
            ?.text()?.let { Status.valueOf(it.uppercase()) }
        val tags = productInfo?.getElementById("tags")?.siblingElements()?.firstOrNull()
            ?.child(0)?.children()?.map { it.text() }

        val id = document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content")
            .removeSurrounding("$baseUrl/series/", "/")

        val seriesBooks = parseSeriesBooksFromShelf(document) ?: parseSeriesBooksFromDiscovery(document)
        return KodanshaSeries(
            id = KodanshaSeriesId(URLDecoder.decode(id, "UTF-8")),
            title = title,
            coverUrl = coverUrl,
            summary = summary,
            authors = authors,
            ageRating = ageRating,
            status = status,
            tags = tags ?: emptyList(),
            books = seriesBooks ?: emptyList()
        )
    }

    fun parseBookListPage(bookList: String): KodanshaBookListPage {
        val document = Jsoup.parse(bookList)
        val books = parseSeriesBooksFromBookListPage(document)
        val pages = document.getElementsByClass("pagination").first()!!
            .children()
            .filter { page -> page.hasClass("pagination__page") }
        val currentPage = pages.first { page -> page.hasAttr("aria-current") }.text().toInt()
        return KodanshaBookListPage(
            page = currentPage,
            totalPages = pages.size,
            books = books
        )
    }

    fun parseBook(book: String): KodanshaBook {
        val document = Jsoup.parse(book)
        val productDetail = document.getElementsByClass("product-detail-hero").first()!!.child(0)
        val fullTitle = productDetail.getElementsByClass("title title--product-page").first()!!
            .text().replace(" (manga)", "")
        val (title, number) = parseBookTitleAndNumber(fullTitle)

        val summary = productDetail.getElementsByClass("product-detail-hero__synopsis").first()?.text()
        val authors = parseAuthors(productDetail)
        val coverUrl = productDetail.getElementsByClass("l-frame product-image ").first()!!.child(0).attr("src")

        val productInfo = document.getElementsByClass("product-info").first()?.child(1)?.child(0)?.child(0)

        val printReleaseDate = productInfo?.getElementById("product_info_box_print_release")
            ?.siblingElements()?.firstOrNull()?.text()
            ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("M/d/yyyy")) }
        val isbn = productInfo?.getElementById("product_info_box_isbn")?.siblingElements()?.firstOrNull()?.text()

        val ebookReleaseDate = productInfo?.getElementById("product_info_box_ebook_release")
            ?.siblingElements()?.firstOrNull()?.text()
            ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("M/d/yyyy")) }
        val eisbn = productInfo?.getElementById("product_info_box_eisbn")?.siblingElements()?.firstOrNull()?.text()

        val pages = productInfo?.getElementById("product_info_box_pages")?.siblingElements()?.firstOrNull()
            ?.text()?.toInt()
        val ageRating = productInfo?.getElementById("product_info_box_rating")?.siblingElements()?.firstOrNull()
            ?.text()?.removeSuffix("+")?.toInt()
        val tags = productInfo?.getElementById("product_info_box_tags")?.siblingElements()?.firstOrNull()
            ?.child(0)?.children()?.map { it.text() }

        val id = document.getElementsByTag("meta").first { it.attr("property") == "og:url" }
            .attr("content")
            .removeSurrounding("$baseUrl/volume/", "/")

        return KodanshaBook(
            id = KodanshaBookId(URLDecoder.decode(id, "UTF-8")),
            name = title,
            number = number,
            summary = summary,
            coverUrl = coverUrl,
            tags = tags ?: emptyList(),
            authors = authors,
            ageRating = ageRating,
            printReleaseDate = printReleaseDate,
            isbn = isbn,
            ebookReleaseDate = ebookReleaseDate,
            eisbn = eisbn,
            pages = pages
        )
    }

    private fun parseAuthors(productDetail: Element): Collection<String> {
        return productDetail.getElementsByClass("product-detail-hero__main-content").first()
            ?.getElementsByClass("byline")?.first()?.text()
            ?.removePrefix("By ")
            ?.split("and")
            ?.map { it.trim() } ?: emptyList()
    }

    private fun parseSeriesBooksFromShelf(document: Document): Collection<KodanshaSeriesBook>? {
        val books = document.getElementsByClass("bookshelf").first()?.children()
            ?.map { book ->
                KodanshaSeriesBook(
                    id = book.child(0).attr("href").removeSurrounding("$baseUrl/volume/", "/")
                        .let { KodanshaBookId(URLDecoder.decode(it, "UTF-8")) },
                    number = book.child(0).child(0).child(1).text().removePrefix("Volume ").trim().toIntOrNull(),
                )
            }
        return if (books?.size == 30) null
        else books
    }

    private fun parseSeriesBooksFromDiscovery(document: Document): Collection<KodanshaSeriesBook>? {
        val books = document.getElementsByClass("product-discovery").first()
            ?.getElementsByTag("ul")?.first()?.children()
            ?.map { it.child(0).child(1) }
            ?.map {
                val (_, number) = parseBookTitleAndNumber(it.text())
                val id = it.child(0).attr("href").removeSurrounding("$baseUrl/volume/", "/")
                KodanshaSeriesBook(
                    id = KodanshaBookId(URLDecoder.decode(id, "UTF-8")),
                    number = number
                )
            }
        return if (books?.size == 4) return null
        else books
    }

    private fun parseSeriesBooksFromBookListPage(document: Document): Collection<KodanshaSeriesBook> {
        return document.getElementsByClass("filters__results").first()!!.child(0)
            .children()
            .map { it.child(0).child(1) }
            .map {
                val (_, number) = parseBookTitleAndNumber(it.text())
                val id = it.child(0).attr("href").removeSurrounding("$baseUrl/volume/", "/")
                KodanshaSeriesBook(
                    id = KodanshaBookId(URLDecoder.decode(id, "UTF-8")),
                    number = number
                )
            }
    }

    private fun parseBookTitleAndNumber(name: String): Pair<String, Int?> {
        val volumeNumber = "(, [Vv]olume (?<volumeNumber>[0-9]+))".toRegex().find(name)
            ?.groups?.get("volumeNumber")?.value?.toIntOrNull()
        val title = name.replace(" (manga)", "")

        return title to volumeNumber
    }
}
