package org.snd.metadata.mangaupdates.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.apache.commons.text.StringEscapeUtils.unescapeHtml4
import org.snd.metadata.mangaupdates.model.SearchResult
import org.snd.metadata.mangaupdates.model.SearchResultPage
import java.time.Year

class MangaUpdatesSearchResultsJsonAdapter {
    @FromJson
    fun fromJson(json: SearchResultPageJson): SearchResultPage {
        return SearchResultPage(
            totalHits = json.total_hits,
            page = json.page,
            perPage = json.per_page,
            results = json.results.map { searchResult(it) }
        )
    }

    @ToJson
    fun toJson(searchResult: SearchResultPage): SearchResultPageJson {
        throw UnsupportedOperationException()
    }

    private fun searchResult(json: ResultJson): SearchResult {
        return SearchResult(
            id = json.record.series_id,
            title = unescapeHtml4(json.hit_title ?: json.record.title),
            summary = json.record.description,
            thumbnail = json.record.image?.url?.original,
            genres = json.record.genres?.map { it.genre } ?: emptyList(),
            year = json.record.year?.let { if (it.isNotBlank()) Year.of(it.toInt()) else null }
        )
    }

}
