package org.snd.metadata.providers.mal.model.json

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import org.snd.metadata.providers.mal.model.AlternativeTitles
import org.snd.metadata.providers.mal.model.Picture
import org.snd.metadata.providers.mal.model.SearchResult
import org.snd.metadata.providers.mal.model.SearchResults


class SearchResultsJsonAdapter {
    @FromJson
    fun fromJson(searchJson: SearchResultsJson): SearchResults {
        val searchResults = searchJson.data.map { it.node }
            .map { json ->
                val altTitles = AlternativeTitles(
                    synonyms = json.alternative_titles.synonyms,
                    en = json.alternative_titles.en,
                    ja = json.alternative_titles.ja
                )

                SearchResult(
                    id = json.id,
                    title = json.title,
                    alternativeTitles = altTitles,
                    mediaType = json.media_type,
                    mainPicture = json.main_picture?.let { pictureFromJson(it) },
                )
            }

        return SearchResults(
            results = searchResults,
            nextPage = searchJson.paging.next
        )
    }

    @ToJson
    fun toJson(@Suppress("UNUSED_PARAMETER") searchResults: SearchResults): SearchResultsJson {
        throw UnsupportedOperationException()
    }

    private fun pictureFromJson(json: PictureJson): Picture {
        return Picture(
            large = json.large,
            medium = json.medium
        )
    }
}
