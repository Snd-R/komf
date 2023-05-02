package org.snd.metadata.providers.kodansha.model

import com.squareup.moshi.JsonClass
import org.snd.metadata.model.Provider.KODANSHA
import org.snd.metadata.model.SeriesSearchResult

@JsonClass(generateAdapter = true)
data class KodanshaSearchResult(
    val type: String,
    val displayType: String,
    val content: KodanshaSearchResultContent,
)

@JsonClass(generateAdapter = true)
data class KodanshaSearchResultContent(
    val id: Int,
    val seriesName: String?,
    val title: String,
    val description: String?,
    val thumbnails: List<KodanshaThumbnail>,
    val readableUrl: String?,
)

fun KodanshaSearchResult.toSeriesSearchResult(): SeriesSearchResult {
    return SeriesSearchResult(
        imageUrl = content.thumbnails.firstOrNull()?.url,
        title = content.title,
        resultId = content.id.toString(),
        provider = KODANSHA,
    )
}
