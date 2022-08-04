package org.snd.metadata.mangaupdates.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.apache.commons.text.StringEscapeUtils.unescapeHtml4
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.snd.metadata.mangaupdates.model.Author
import org.snd.metadata.mangaupdates.model.Category
import org.snd.metadata.mangaupdates.model.Publisher
import org.snd.metadata.mangaupdates.model.Series
import org.snd.metadata.mangaupdates.model.Status
import org.snd.metadata.mangaupdates.model.Type
import java.net.URI
import java.time.Year

class MangaUpdatesSeriesJsonAdapter {
    @FromJson
    fun fromJson(json: SeriesJson): Series {
        return Series(
            id = json.series_id,
            title = unescapeHtml4(json.title),
            description = json.description?.let { parseDescription(it) },
            type = type(json.type),
            associatedNames = json.associated.map { unescapeHtml4(it.title) },
            status = status(json.status),
            image = json.image?.url?.original?.let { URI.create(it) },
            genres = json.genres?.map { unescapeHtml4(it.genre) } ?: emptyList(),
            categories = json.categories?.map {
                Category(
                    id = it.series_id,
                    name = unescapeHtml4(it.category),
                    votes = it.votes,
                    votesPlus = it.votes_plus,
                    votesMinus = it.votes_minus
                )
            } ?: emptyList(),
            authors = json.authors?.map { Author(id = it.author_id, name = it.name, type = it.type) } ?: emptyList(),
            year = json.year?.let { if (it.isNotBlank()) Year.of(it.toInt()) else null },
            publishers = json.publishers?.map {
                Publisher(
                    id = it.publisher_id,
                    name = unescapeHtml4(it.publisher_name),
                    type = it.type,
                    notes = it.notes
                )
            } ?: emptyList()
        )
    }

    @ToJson
    fun toJson(searchResult: Series): SeriesJson {
        throw UnsupportedOperationException()
    }

    private fun type(type: String?): Type? {
        return runCatching { type?.let { Type.valueOf(it.trim().uppercase()) } }
            .getOrNull()
    }

    private fun status(status: String?): Status? {
        return status?.let {
            "\\(.*\\)".toRegex().find(status)?.value?.removeSurrounding("(", ")")?.let {
                runCatching { Status.valueOf(it.uppercase()) }
                    .getOrNull()
            }
        }
    }


    private fun parseDescription(description: String): String {
        return Jsoup.parse(description).child(0).child(1).childNodes()
            .joinToString("") {
                when (it) {
                    is Element -> parseDescriptionPart(it)
                    is TextNode -> it.wholeText
                    else -> ""
                }
            }
    }

    private fun parseDescriptionPart(element: Element): String {
        return if (element.tag().name == "br") "\n"
        else element.text()
    }
}
