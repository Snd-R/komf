package snd.komf.providers.mangaupdates

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import com.fleeksoft.ksoup.parser.Parser.Companion.unescapeEntities
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import snd.komf.model.Image
import snd.komf.providers.mangaupdates.model.MangaUpdatesSeries
import snd.komf.providers.mangaupdates.model.SearchResultPage
import snd.komf.providers.mangaupdates.model.SeriesType

class MangaUpdatesClient(
    private val ktor: HttpClient,
) {
    private val baseUrl: String = "https://api.mangaupdates.com/v1"

    suspend fun searchSeries(
        name: String,
        types: Collection<SeriesType>,
        page: Int = 1,
        perPage: Int = 5,
    ): SearchResultPage {
        val searchResult = ktor.post("$baseUrl/series/search") {
            contentType(ContentType.Application.Json)
            setBody(
                MangaUpdatesSearchRequest(
                    search = name,
                    page = page,
                    perPage = perPage,
                    types = types,
                )
            )
        }.body<SearchResultPage>()

        return searchResult.copy(results = searchResult.results.map { result ->
            result.copy(
                record = result.record.copy(
                    title = unescapeEntities(result.record.title, false),
                    year = result.record.year?.let { takeLastYear(it) }
                )
            )
        })
    }

    suspend fun getSeries(seriesId: Long): MangaUpdatesSeries {
        val series = ktor.get("$baseUrl/series/$seriesId").body<MangaUpdatesSeries>()

        return series.copy(
            title = unescapeEntities(series.title, false).removeSuffix(" (Novel)"),
            description = series.description?.let { parseDescription(it) },
            associated = series.associated.map { it.copy(title = unescapeEntities(it.title, false)) },
            genres = series.genres.map { it.copy(genre = unescapeEntities(it.genre, false)) },
            categories = series.categories.map { it.copy(category = unescapeEntities(it.category, false)) },
            authors = series.authors.map {
                it.copy(
                    name = unescapeEntities(it.name, false),
                    type = unescapeEntities(it.type, false)
                )
            },
            publishers = series.publishers.map {
                it.copy(
                    name = unescapeEntities(it.name, false),
                    type = unescapeEntities(it.type, false)
                )
            },
            year = series.year?.let { takeLastYear(it) }
        )
    }

    suspend fun getThumbnail(series: MangaUpdatesSeries): Image? {
        return series.image?.url?.original?.let { url ->
            val bytes = ktor.get(url).body<ByteArray>()
            Image(bytes)
        }
    }


    private fun parseDescription(description: String): String {
        return Ksoup.parse(description).child(0).child(1).childNodes()
            .joinToString("") {
                when (it) {
                    is Element -> parseDescriptionPart(it)
                    is TextNode -> it.getWholeText()
                    else -> ""
                }
            }
    }

    private fun parseDescriptionPart(element: Element): String {
        return if (element.tag().name == "br") "\n"
        else element.text()
    }

    private val yearRegex = "-[0-9]+$".toRegex()
    private fun takeLastYear(yearString: String): String {
        return yearString.replace(yearRegex, "")
    }

    @Serializable
    private data class MangaUpdatesSearchRequest(
        val search: String,
        val page: Int,
        val perPage: Int,
        val types: Collection<SeriesType>
    )
}

